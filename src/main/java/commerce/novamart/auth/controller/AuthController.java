package commerce.novamart.auth.controller;

import commerce.novamart.auth.dto.*;
import commerce.novamart.auth.dto.VerificationDtos.SendOtpRequest;
import commerce.novamart.auth.dto.VerificationDtos.VerifyEmailRequest;
import commerce.novamart.auth.dto.VerificationDtos.VerifyOtpRequest;
import commerce.novamart.auth.service.AuthService;
import commerce.novamart.auth.service.AuthService.DevToken;
import commerce.novamart.common.dto.ApiResponse;
import commerce.novamart.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication APIs")
public class AuthController {

    private final AuthService authService;
    private final CurrentUser currentUser;

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        AuthResponse response = authService.register(request, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful", response));
    }

    @PostMapping("/login")
    @Operation(summary = "Login user")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        AuthResponse response = authService.login(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request, HttpServletRequest httpRequest) {
        AuthResponse response = authService.refresh(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
    }

    @PostMapping("/logout-all")
    @Operation(summary = "Revoke every session of the current user")
    public ApiResponse<Void> logoutAll() {
        authService.logoutAll(currentUser.require());
        return ApiResponse.success("Signed out of all devices", null);
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset")
    public ApiResponse<DevToken> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ApiResponse.success("If that email is registered, reset instructions have been sent",
                authService.forgotPassword(request));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password with token")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successful", null));
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify an email address with the token from the verification link")
    public ApiResponse<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request.token());
        return ApiResponse.success("Email verified", null);
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Send a new verification email")
    public ApiResponse<DevToken> resendVerification() {
        return ApiResponse.success("Verification email sent", authService.resendVerification(currentUser.require()));
    }

    @PostMapping("/send-otp")
    @Operation(summary = "Send a one-time verification code")
    public ApiResponse<DevToken> sendOtp(@RequestBody(required = false) SendOtpRequest request) {
        var type = request == null ? new SendOtpRequest(null).typeOrDefault() : request.typeOrDefault();
        return ApiResponse.success("Verification code sent", authService.sendOtp(currentUser.require(), type));
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify a one-time code")
    public ApiResponse<Void> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        authService.verifyOtp(currentUser.require(), request.code(), request.typeOrDefault());
        return ApiResponse.success("Verified successfully", null);
    }
}
