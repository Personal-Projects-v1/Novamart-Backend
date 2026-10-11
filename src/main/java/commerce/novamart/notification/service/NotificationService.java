package commerce.novamart.notification.service;

import commerce.novamart.common.exception.ResourceNotFoundException;
import commerce.novamart.common.util.Dates;
import commerce.novamart.notification.entity.Notification;
import commerce.novamart.notification.repository.NotificationRepository;
import commerce.novamart.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    public record NotificationResponse(String id, String type, String title, String body, String createdAt,
                                       boolean read, String href) {
    }

    private final NotificationRepository repository;

    @Transactional
    public void notify(User user, String type, String title, String body, String href) {
        Notification n = new Notification();
        n.setUser(user);
        n.setType(type);
        n.setTitle(title);
        n.setBody(body);
        n.setHref(href);
        repository.save(n);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> list(User user) {
        return repository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream().map(this::toResponse).toList();
    }

    @Transactional
    public NotificationResponse markRead(User user, UUID id, boolean read) {
        Notification n = find(user, id);
        n.setRead(read);
        return toResponse(n);
    }

    @Transactional
    public void markAllRead(User user) {
        repository.markAllRead(user.getId());
    }

    @Transactional
    public void delete(User user, UUID id) {
        repository.delete(find(user, id));
    }

    private Notification find(User user, UUID id) {
        return repository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(n.getId().toString(), n.getType(), n.getTitle(), n.getBody(),
                Dates.iso(n.getCreatedAt()), n.isRead(), n.getHref());
    }
}
