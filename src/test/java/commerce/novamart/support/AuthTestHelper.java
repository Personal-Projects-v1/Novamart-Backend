package commerce.novamart.support;

import commerce.novamart.user.entity.Role;
import commerce.novamart.user.entity.User;
import commerce.novamart.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthTestHelper {

    public static final String VALID_PASSWORD = "Secret123";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User customer(String email) {
        return user(email, Role.CUSTOMER);
    }

    public User admin(String email) {
        return user(email, Role.ADMIN);
    }

    private User user(String email, Role role) {
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail(email.toLowerCase());
        user.setPassword(passwordEncoder.encode(VALID_PASSWORD));
        user.setRole(role);
        user.setEnabled(true);
        user.setEmailVerified(true);
        user.setPhoneVerified(false);
        return userRepository.save(user);
    }
}
