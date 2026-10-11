package commerce.novamart.auth.service;

import commerce.novamart.auth.dto.*;
import commerce.novamart.auth.entity.EmailVerificationToken;
import commerce.novamart.auth.entity.OtpToken;
import commerce.novamart.auth.entity.PasswordResetToken;
import commerce.novamart.auth.entity.RefreshToken;
import commerce.novamart.auth.repository.EmailVerificationTokenRepository;
import commerce.novamart.auth.repository.OtpTokenRepository;
import commerce.novamart.auth.repository.PasswordResetTokenRepository;
import commerce.novamart.auth.repository.RefreshTokenRepository;
import commerce.novamart.common.exception.BadRequestException;
import commerce.novamart.common.exception.UnauthorizedException;
import commerce.novamart.common.util.Dates;
import commerce.novamart.config.JwtConfig;
import commerce.novamart.notification.service.NotificationService;
import commerce.novamart.user.entity.Role;
import commerce.novamart.user.entity.User;
import commerce.novamart.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final OtpTokenRepository otpTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final JwtConfig jwtConfig;
    private final NotificationService notificationService;

    /** Dev/demo only: echo verification + reset tokens in API responses because no mail provider is wired up. */
    @Value("${app.dev.expose-tokens:false}")
    private boolean exposeDevTokens;

    @Transactional
    public AuthResponse register(RegisterRequest request, HttpServletRequest httpRequest) {
        String email = normalize(request.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email already registered");
        }

        String phone = blankToNull(request.getPhone());
        if (phone != null && userRepository.existsByPhone(phone)) {
            throw new BadRequestException("Phone number already registered");
        }

        User user = new User();
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setEmail(email);
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.CUSTOMER);
        user.setEnabled(true);
        user.setEmailVerified(false);
        user.setPhoneVerified(false);

        user = userRepository.save(user);
        log.info("User registered successfully: {}", user.getEmail());

        issueEmailVerification(user);
        notificationService.notify(user, "promotion", "Welcome to NovaMart!",
                "Use code WELCOME10 for 10% off your first order.", "/deals");

        return startSession(user, httpRequest);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String email = normalize(request.getEmail());
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.getPassword()));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!user.getEnabled()) {
            throw new UnauthorizedException("Account is disabled");
        }

        log.info("User logged in successfully: {}", user.getEmail());
        return startSession(user, httpRequest);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request, HttpServletRequest httpRequest) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (refreshToken.isExpired()) {
            throw new UnauthorizedException("Refresh token expired");
        }
        if (refreshToken.isRevoked()) {
            throw new UnauthorizedException("Refresh token revoked");
        }

        User user = refreshToken.getUser();
        if (!user.getEnabled()) {
            throw new UnauthorizedException("Account is disabled");
        }

        // Rotate: the old token is retired and the session continues under a new one.
        refreshToken.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(refreshToken);

        return startSession(user, httpRequest);
    }

    @Transactional
    public void logout(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(refreshToken -> {
            refreshToken.setRevokedAt(LocalDateTime.now());
            refreshTokenRepository.save(refreshToken);
            log.info("User logged out: {}", refreshToken.getUser().getEmail());
        });
    }

    @Transactional
    public void logoutAll(User user) {
        refreshTokenRepository.revokeAllUserTokens(user, LocalDateTime.now());
    }

    @Transactional
    public DevToken forgotPassword(ForgotPasswordRequest request) {
        // Always succeed so the endpoint can't be used to discover which emails are registered.
        return userRepository.findByEmail(normalize(request.getEmail())).map(user -> {
            passwordResetTokenRepository.deleteByUser(user);

            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setToken(token);
            resetToken.setUser(user);
            resetToken.setCreatedAt(LocalDateTime.now());
            resetToken.setExpiresAt(LocalDateTime.now().plusHours(1));
            passwordResetTokenRepository.save(resetToken);

            log.info("Password reset token generated for user: {}", user.getEmail());
            log.info("Reset token (DEV ONLY): {}", token);
            return devToken(token);
        }).orElse(null);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new BadRequestException("Invalid reset token"));

        if (resetToken.isExpired()) {
            throw new BadRequestException("Reset token expired");
        }
        if (resetToken.isUsed()) {
            throw new BadRequestException("Reset token already used");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(resetToken);

        refreshTokenRepository.revokeAllUserTokens(user, LocalDateTime.now());

        log.info("Password reset successfully for user: {}", user.getEmail());
    }

    // ----- email verification ---------------------------------------------------------------------

    @Transactional
    public DevToken resendVerification(User user) {
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BadRequestException("Email is already verified");
        }
        return issueEmailVerification(user);
    }

    @Transactional
    public void verifyEmail(String token) {
        EmailVerificationToken evt = emailVerificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid verification link"));
        if (evt.isExpired()) {
            throw new BadRequestException("Verification link expired");
        }
        markEmailVerified(evt.getUser());
        evt.setVerifiedAt(LocalDateTime.now());
        emailVerificationTokenRepository.save(evt);
    }

    @Transactional
    public DevToken sendOtp(User user, OtpToken.OtpType type) {
        otpTokenRepository.deleteByUserAndType(user, type);

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        OtpToken otp = new OtpToken();
        otp.setUser(user);
        otp.setType(type);
        otp.setOtp(passwordEncoder.encode(code));
        otp.setCreatedAt(LocalDateTime.now());
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        otp.setAttempts(0);
        otpTokenRepository.save(otp);

        log.info("OTP ({}) generated for user {} (DEV ONLY): {}", type, user.getEmail(), code);
        return devToken(code);
    }

    @Transactional(noRollbackFor = BadRequestException.class)
    public void verifyOtp(User user, String code, OtpToken.OtpType type) {
        OtpToken otp = otpTokenRepository.findByUserAndTypeAndVerifiedAtIsNull(user, type).stream()
                .findFirst()
                .orElseThrow(() -> new BadRequestException("No active code. Please request a new one."));

        if (otp.isExpired()) {
            throw new BadRequestException("This code has expired. Please request a new one.");
        }
        if (otp.hasExceededAttempts(3)) {
            throw new BadRequestException("Too many attempts. Please request a new code.");
        }

        otp.setAttempts(otp.getAttempts() + 1);
        if (!passwordEncoder.matches(code, otp.getOtp())) {
            otpTokenRepository.save(otp);
            throw new BadRequestException("Incorrect code");
        }

        otp.setVerifiedAt(LocalDateTime.now());
        otpTokenRepository.save(otp);
        if (type == OtpToken.OtpType.EMAIL_VERIFICATION) markEmailVerified(user);
        if (type == OtpToken.OtpType.PHONE_VERIFICATION) {
            user.setPhoneVerified(true);
            userRepository.save(user);
        }
    }

    // ----- helpers ----------------------------------------------------------------------------------

    private DevToken issueEmailVerification(User user) {
        emailVerificationTokenRepository.deleteByUser(user);

        String token = UUID.randomUUID().toString();
        EmailVerificationToken evt = new EmailVerificationToken();
        evt.setToken(token);
        evt.setUser(user);
        evt.setCreatedAt(LocalDateTime.now());
        evt.setExpiresAt(LocalDateTime.now().plusHours(24));
        emailVerificationTokenRepository.save(evt);

        log.info("Email verification token for {} (DEV ONLY): {}", user.getEmail(), token);
        return devToken(token);
    }

    private void markEmailVerified(User user) {
        user.setEmailVerified(true);
        userRepository.save(user);
    }

    private DevToken devToken(String value) {
        return exposeDevTokens ? new DevToken(value) : null;
    }

    private AuthResponse startSession(User user, HttpServletRequest httpRequest) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        RefreshToken session = saveRefreshToken(user, refreshToken, httpRequest);
        return buildAuthResponse(user, accessToken, refreshToken, session.getId().toString());
    }

    private RefreshToken saveRefreshToken(User user, String token, HttpServletRequest request) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(token);
        refreshToken.setUser(user);
        refreshToken.setCreatedAt(LocalDateTime.now());
        refreshToken.setExpiresAt(LocalDateTime.now().plusSeconds(jwtConfig.getRefreshTokenExpiration() / 1000));
        if (request != null) {
            refreshToken.setIpAddress(getClientIp(request));
            String ua = request.getHeader("User-Agent");
            refreshToken.setDeviceInfo(ua == null ? null : ua.substring(0, Math.min(ua.length(), 490)));
        }
        return refreshTokenRepository.save(refreshToken);
    }

    public AuthResponse.UserInfo toUserInfo(User user) {
        return AuthResponse.UserInfo.builder()
                .id(user.getId().toString())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole().name())
                .emailVerified(user.getEmailVerified())
                .phoneVerified(user.getPhoneVerified())
                .createdAt(Dates.iso(user.getCreatedAt()))
                .build();
    }

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken, String sessionId) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .sessionId(sessionId)
                .user(toUserInfo(user))
                .build();
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /** Verification/reset secret echoed back to the client in dev mode only. */
    public record DevToken(String devToken) {
    }
}
