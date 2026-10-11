package commerce.novamart.common.security;

import commerce.novamart.common.exception.UnauthorizedException;
import commerce.novamart.user.entity.User;
import commerce.novamart.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Resolves the authenticated {@link User} entity (the security principal only carries the email). */
@Component
@RequiredArgsConstructor
public class CurrentUser {

    private final UserRepository userRepository;

    public Optional<User> find() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return userRepository.findByEmail(auth.getName());
    }

    public User require() {
        return find().orElseThrow(() -> new UnauthorizedException("Authentication required"));
    }
}
