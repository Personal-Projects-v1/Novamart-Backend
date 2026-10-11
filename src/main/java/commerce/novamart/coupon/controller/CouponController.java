package commerce.novamart.coupon.controller;

import commerce.novamart.common.dto.ApiResponse;
import commerce.novamart.common.exception.BadRequestException;
import commerce.novamart.coupon.service.CouponService;
import commerce.novamart.coupon.service.CouponService.CouponResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
@Tag(name = "Coupons")
public class CouponController {

    public record ValidateRequest(@NotBlank(message = "Coupon code is required") String code,
                                  @NotNull(message = "Subtotal is required") BigDecimal subtotal) {
    }

    /** A rejected coupon is a normal outcome of the UI flow, so it is reported in-band with HTTP 200. */
    public record ValidateResponse(CouponResponse coupon, String error) {
    }

    private final CouponService service;

    @GetMapping("/active")
    public ApiResponse<List<CouponResponse>> active() {
        return ApiResponse.success(service.active());
    }

    @PostMapping("/validate")
    public ApiResponse<ValidateResponse> validate(@Valid @RequestBody ValidateRequest request) {
        try {
            var coupon = service.validate(request.code(), request.subtotal());
            return ApiResponse.success(new ValidateResponse(CouponService.toResponse(coupon), null));
        } catch (BadRequestException e) {
            return ApiResponse.success(new ValidateResponse(null, e.getMessage()));
        }
    }
}
