package commerce.novamart.user.controller;

import commerce.novamart.common.dto.ApiResponse;
import commerce.novamart.common.security.CurrentUser;
import commerce.novamart.user.dto.UserDtos.AddressRequest;
import commerce.novamart.user.dto.UserDtos.AddressResponse;
import commerce.novamart.user.dto.UserDtos.ChangePasswordRequest;
import commerce.novamart.user.dto.UserDtos.Preferences;
import commerce.novamart.user.dto.UserDtos.SessionResponse;
import commerce.novamart.user.dto.UserDtos.UpdateProfileRequest;
import commerce.novamart.user.dto.UserDtos.UserResponse;
import commerce.novamart.user.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
@Tag(name = "Account", description = "Profile, addresses, security")
public class UserController {

    private final UserService service;
    private final CurrentUser currentUser;

    @GetMapping
    public ApiResponse<UserResponse> me() {
        return ApiResponse.success(UserService.toResponse(currentUser.require()));
    }

    @PatchMapping
    public ApiResponse<UserResponse> update(@Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.success("Profile updated", service.updateProfile(currentUser.require(), request));
    }

    @PutMapping("/preferences")
    public ApiResponse<UserResponse> preferences(@RequestBody Preferences preferences) {
        return ApiResponse.success(service.updatePreferences(currentUser.require(), preferences));
    }

    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        service.changePassword(currentUser.require(), request);
        return ApiResponse.success("Password changed successfully", null);
    }

    @GetMapping("/addresses")
    public ApiResponse<List<AddressResponse>> addresses() {
        return ApiResponse.success(service.addresses(currentUser.require()));
    }

    @PostMapping("/addresses")
    public ResponseEntity<ApiResponse<AddressResponse>> createAddress(@Valid @RequestBody AddressRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(service.createAddress(currentUser.require(), request)));
    }

    @PutMapping("/addresses/{id}")
    public ApiResponse<AddressResponse> updateAddress(@PathVariable UUID id, @Valid @RequestBody AddressRequest request) {
        return ApiResponse.success(service.updateAddress(currentUser.require(), id, request));
    }

    @PatchMapping("/addresses/{id}/default")
    public ApiResponse<AddressResponse> setDefault(@PathVariable UUID id) {
        return ApiResponse.success(service.setDefaultAddress(currentUser.require(), id));
    }

    @DeleteMapping("/addresses/{id}")
    public ApiResponse<Void> deleteAddress(@PathVariable UUID id) {
        service.deleteAddress(currentUser.require(), id);
        return ApiResponse.success(null);
    }

    @GetMapping("/sessions")
    public ApiResponse<List<SessionResponse>> sessions() {
        return ApiResponse.success(service.sessions(currentUser.require()));
    }

    @DeleteMapping("/sessions/{id}")
    public ApiResponse<Void> revokeSession(@PathVariable UUID id) {
        service.revokeSession(currentUser.require(), id);
        return ApiResponse.success(null);
    }
}
