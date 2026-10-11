package commerce.novamart.integration;

import commerce.novamart.support.AuthTestHelper;
import commerce.novamart.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends IntegrationTestBase {

    @Test
    void registerThenLoginReturnsTokens() throws Exception {
        String email = "new.user+" + System.nanoTime() + "@example.com";
        String register = """
                {
                  "firstName": "Nova",
                  "lastName": "Shopper",
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, AuthTestHelper.VALID_PASSWORD);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(register))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.data.user.email").value(email.toLowerCase()));

        String login = """
                {"email":"%s","password":"%s"}
                """.formatted(email, AuthTestHelper.VALID_PASSWORD);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(login))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken", notNullValue()));
    }

    @Test
    void duplicateRegistrationIsRejected() throws Exception {
        String email = "dup.user+" + System.nanoTime() + "@example.com";
        String body = """
                {
                  "firstName": "Dup",
                  "lastName": "User",
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, AuthTestHelper.VALID_PASSWORD);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email already registered")); // ErrorResponse
    }

    @Test
    void loginWithWrongPasswordFails() throws Exception {
        String email = "known+" + System.nanoTime() + "@example.com";
        auth.customer(email);
        String login = """
                {"email":"%s","password":"WrongPass1"}
                """.formatted(email);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(login))
                .andExpect(status().isUnauthorized());
    }
}
