package commerce.novamart.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public final class UserDtos {

    private UserDtos() {
    }

    public record Preferences(boolean orderUpdates, boolean promotions, boolean priceDrops, boolean newsletter) {
    }

    /** Matches the frontend {@code User} type (role is lower-case), plus profile extras. */
    public record UserResponse(
            String id, String firstName, String lastName, String email, String phone, String avatar,
            String role, String createdAt, boolean emailVerified, boolean phoneVerified,
            LocalDate birthday, String gender, Preferences preferences) {
    }

    public record UpdateProfileRequest(
            @NotBlank(message = "First name is required") @Size(max = 100) String firstName,
            @NotBlank(message = "Last name is required") @Size(max = 100) String lastName,
            @Pattern(regexp = "^(\\+?[1-9]\\d{1,14})?$", message = "Phone number must be valid") String phone,
            LocalDate birthday,
            @Size(max = 20) String gender) {
    }

    public record ChangePasswordRequest(
            @NotBlank(message = "Current password is required") String currentPassword,
            @NotBlank(message = "New password is required")
            @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
            @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
                    message = "Password must contain at least one uppercase letter, one lowercase letter, and one number")
            String newPassword) {
    }

    public record AddressRequest(
            @Size(max = 60) String label,
            @NotBlank(message = "Full name is required") @Size(max = 200) String fullName,
            @NotBlank(message = "Phone is required") @Size(max = 40) String phone,
            @Size(max = 255) String email,
            @NotBlank(message = "Country is required") @Size(max = 120) String country,
            @NotBlank(message = "Province is required") @Size(max = 120) String province,
            @NotBlank(message = "City is required") @Size(max = 120) String city,
            @Size(max = 120) String district,
            @NotBlank(message = "Street is required") @Size(max = 300) String street,
            @Size(max = 30) String postalCode,
            @JsonProperty("isDefault") Boolean isDefault) {
    }

    public record AddressResponse(
            String id, String fullName, String phone, String email, String country, String province, String city,
            String district, String street, String postalCode, @JsonProperty("isDefault") boolean isDefault, String label) {
    }

    public record SessionResponse(String id, String device, String ipAddress, String createdAt) {
    }
}
