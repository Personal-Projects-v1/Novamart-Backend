package commerce.novamart.auth.dto;

import commerce.novamart.auth.entity.OtpToken;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public final class VerificationDtos {

    private VerificationDtos() {
    }

    public record VerifyEmailRequest(@NotBlank(message = "Token is required") String token) {
    }

    public record SendOtpRequest(OtpToken.OtpType type) {
        public OtpToken.OtpType typeOrDefault() {
            return type == null ? OtpToken.OtpType.EMAIL_VERIFICATION : type;
        }
    }

    public record VerifyOtpRequest(
            @NotBlank(message = "Code is required")
            @Pattern(regexp = "^\\d{6}$", message = "Code must be 6 digits") String code,
            OtpToken.OtpType type) {
        public OtpToken.OtpType typeOrDefault() {
            return type == null ? OtpToken.OtpType.EMAIL_VERIFICATION : type;
        }
    }
}
