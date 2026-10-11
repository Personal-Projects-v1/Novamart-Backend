package commerce.novamart.unit;

import commerce.novamart.auth.service.JwtService;
import commerce.novamart.config.JwtConfig;
import commerce.novamart.user.entity.Role;
import commerce.novamart.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        JwtConfig config = new JwtConfig();
        config.setSecret("test-secret-key-for-testing-only-must-be-at-least-256-bits-long");
        config.setAccessTokenExpiration(60_000L);
        config.setRefreshTokenExpiration(120_000L);
        jwtService = new JwtService(config);

        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("shopper@example.com");
        user.setRole(Role.CUSTOMER);
    }

    @Test
    void accessTokenCarriesSubjectRoleAndUserId() {
        String token = jwtService.generateAccessToken(user);

        assertThat(jwtService.extractEmail(token)).isEqualTo("shopper@example.com");
        assertThat(jwtService.extractRole(token)).isEqualTo("CUSTOMER");
        assertThat(jwtService.extractUserId(token)).isEqualTo(user.getId());
    }

    @Test
    void tokenIsValidForMatchingUserDetails() {
        String token = jwtService.generateAccessToken(user);
        UserDetails details = org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password("ignored")
                .roles("CUSTOMER")
                .build();

        assertThat(jwtService.isTokenValid(token, details)).isTrue();
    }

    @Test
    void refreshTokenParsesUserId() {
        String token = jwtService.generateRefreshToken(user);
        assertThat(jwtService.extractUserId(token)).isEqualTo(user.getId());
        assertThat(jwtService.extractEmail(token)).isEqualTo("shopper@example.com");
    }
}
