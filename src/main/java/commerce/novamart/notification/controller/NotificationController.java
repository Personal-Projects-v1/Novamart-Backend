package commerce.novamart.notification.controller;

import commerce.novamart.common.dto.ApiResponse;
import commerce.novamart.common.security.CurrentUser;
import commerce.novamart.notification.service.NotificationService;
import commerce.novamart.notification.service.NotificationService.NotificationResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications")
public class NotificationController {

    private final NotificationService service;
    private final CurrentUser currentUser;

    @GetMapping
    public ApiResponse<List<NotificationResponse>> list() {
        return ApiResponse.success(service.list(currentUser.require()));
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<NotificationResponse> markRead(@PathVariable UUID id) {
        return ApiResponse.success(service.markRead(currentUser.require(), id, true));
    }

    @PatchMapping("/{id}/unread")
    public ApiResponse<NotificationResponse> markUnread(@PathVariable UUID id) {
        return ApiResponse.success(service.markRead(currentUser.require(), id, false));
    }

    @PostMapping("/read-all")
    public ApiResponse<Void> markAllRead() {
        service.markAllRead(currentUser.require());
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        service.delete(currentUser.require(), id);
        return ApiResponse.success(null);
    }
}
