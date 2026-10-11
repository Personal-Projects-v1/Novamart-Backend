package commerce.novamart.user.service;

import commerce.novamart.auth.entity.RefreshToken;
import commerce.novamart.auth.repository.RefreshTokenRepository;
import commerce.novamart.common.exception.BadRequestException;
import commerce.novamart.common.exception.ResourceNotFoundException;
import commerce.novamart.common.util.Dates;
import commerce.novamart.user.dto.UserDtos.AddressRequest;
import commerce.novamart.user.dto.UserDtos.AddressResponse;
import commerce.novamart.user.dto.UserDtos.ChangePasswordRequest;
import commerce.novamart.user.dto.UserDtos.Preferences;
import commerce.novamart.user.dto.UserDtos.SessionResponse;
import commerce.novamart.user.dto.UserDtos.UpdateProfileRequest;
import commerce.novamart.user.dto.UserDtos.UserResponse;
import commerce.novamart.user.entity.Address;
import commerce.novamart.user.entity.Role;
import commerce.novamart.user.entity.User;
import commerce.novamart.user.repository.AddressRepository;
import commerce.novamart.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    // ----- profile --------------------------------------------------------------------------------

    public static UserResponse toResponse(User u) {
        return new UserResponse(u.getId().toString(), u.getFirstName(), u.getLastName(), u.getEmail(), u.getPhone(),
                u.getAvatarUrl(), u.getRole() == Role.ADMIN ? "admin" : "customer", Dates.iso(u.getCreatedAt()),
                Boolean.TRUE.equals(u.getEmailVerified()), Boolean.TRUE.equals(u.getPhoneVerified()),
                u.getBirthday(), u.getGender(),
                new Preferences(u.isPrefOrderUpdates(), u.isPrefPromotions(), u.isPrefPriceDrops(), u.isPrefNewsletter()));
    }

    public UserResponse updateProfile(User user, UpdateProfileRequest req) {
        String phone = req.phone() == null || req.phone().isBlank() ? null : req.phone().trim();
        if (phone != null && !phone.equals(user.getPhone())
                && userRepository.findByPhone(phone).filter(o -> !o.getId().equals(user.getId())).isPresent()) {
            throw new BadRequestException("Phone number already registered");
        }
        if (phone != null && !phone.equals(user.getPhone())) user.setPhoneVerified(false);
        user.setFirstName(req.firstName().trim());
        user.setLastName(req.lastName().trim());
        user.setPhone(phone);
        user.setBirthday(req.birthday());
        user.setGender(req.gender() == null || req.gender().isBlank() ? null : req.gender());
        return toResponse(userRepository.save(user));
    }

    public UserResponse updatePreferences(User user, Preferences p) {
        user.setPrefOrderUpdates(p.orderUpdates());
        user.setPrefPromotions(p.promotions());
        user.setPrefPriceDrops(p.priceDrops());
        user.setPrefNewsletter(p.newsletter());
        return toResponse(userRepository.save(user));
    }

    public void changePassword(User user, ChangePasswordRequest req) {
        if (!passwordEncoder.matches(req.currentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }
        if (passwordEncoder.matches(req.newPassword(), user.getPassword())) {
            throw new BadRequestException("New password must be different from the current one");
        }
        user.setPassword(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);
    }

    // ----- addresses ------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<AddressResponse> addresses(User user) {
        return addressRepository.findByUserIdOrderByDefaultAddressDescCreatedAtAsc(user.getId()).stream()
                .map(UserService::toResponse).toList();
    }

    public AddressResponse createAddress(User user, AddressRequest req) {
        Address a = new Address();
        a.setUser(user);
        apply(a, req);
        boolean first = addressRepository.countByUserId(user.getId()) == 0;
        if (first || Boolean.TRUE.equals(req.isDefault())) makeDefault(user, a);
        return toResponse(addressRepository.save(a));
    }

    public AddressResponse updateAddress(User user, UUID id, AddressRequest req) {
        Address a = findAddress(user, id);
        apply(a, req);
        if (Boolean.TRUE.equals(req.isDefault())) makeDefault(user, a);
        return toResponse(addressRepository.save(a));
    }

    public AddressResponse setDefaultAddress(User user, UUID id) {
        Address a = findAddress(user, id);
        makeDefault(user, a);
        return toResponse(addressRepository.save(a));
    }

    public void deleteAddress(User user, UUID id) {
        Address a = findAddress(user, id);
        boolean wasDefault = a.isDefaultAddress();
        addressRepository.delete(a);
        addressRepository.flush();
        if (wasDefault) {
            addressRepository.findByUserIdOrderByDefaultAddressDescCreatedAtAsc(user.getId()).stream().findFirst()
                    .ifPresent(next -> next.setDefaultAddress(true));
        }
    }

    private Address findAddress(User user, UUID id) {
        return addressRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
    }

    private void makeDefault(User user, Address target) {
        addressRepository.findByUserIdOrderByDefaultAddressDescCreatedAtAsc(user.getId())
                .forEach(other -> other.setDefaultAddress(false));
        target.setDefaultAddress(true);
    }

    private static void apply(Address a, AddressRequest r) {
        a.setLabel(blankToNull(r.label()));
        a.setFullName(r.fullName().trim());
        a.setPhone(r.phone().trim());
        a.setEmail(blankToNull(r.email()));
        a.setCountry(r.country().trim());
        a.setProvince(r.province().trim());
        a.setCity(r.city().trim());
        a.setDistrict(blankToNull(r.district()));
        a.setStreet(r.street().trim());
        a.setPostalCode(blankToNull(r.postalCode()));
    }

    public static AddressResponse toResponse(Address a) {
        return new AddressResponse(a.getId().toString(), a.getFullName(), a.getPhone(), a.getEmail(), a.getCountry(),
                a.getProvince(), a.getCity(), a.getDistrict(), a.getStreet(), a.getPostalCode(),
                a.isDefaultAddress(), a.getLabel());
    }

    // ----- sessions -------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<SessionResponse> sessions(User user) {
        return refreshTokenRepository.findByUserAndRevokedAtIsNull(user).stream()
                .filter(t -> !t.isExpired())
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(t -> new SessionResponse(t.getId().toString(), describe(t.getDeviceInfo()), t.getIpAddress(),
                        Dates.iso(t.getCreatedAt())))
                .toList();
    }

    public void revokeSession(User user, UUID sessionId) {
        RefreshToken token = refreshTokenRepository.findById(sessionId)
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        token.setRevokedAt(LocalDateTime.now());
    }

    /** Turns a raw User-Agent into something readable like "Chrome on Windows". */
    private static String describe(String ua) {
        if (ua == null || ua.isBlank()) return "Unknown device";
        String browser = ua.contains("Edg/") ? "Edge" : ua.contains("Chrome/") ? "Chrome"
                : ua.contains("Firefox/") ? "Firefox" : ua.contains("Safari/") ? "Safari" : "Browser";
        String os = ua.contains("Windows") ? "Windows" : ua.contains("Android") ? "Android"
                : ua.contains("iPhone") || ua.contains("iPad") ? "iOS" : ua.contains("Mac OS") ? "macOS"
                : ua.contains("Linux") ? "Linux" : "Unknown OS";
        return browser + " on " + os;
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
