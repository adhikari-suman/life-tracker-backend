package com.lifetracker.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The login brute-force throttle over HTTP (MockMvc, real Postgres). The limit is small and the
 * window long, so the sequence is deterministic within one test. Each test uses a distinct email —
 * the throttle keys on email and rows persist across tests in the shared container.
 */
@AutoConfigureMockMvc
@TestPropertySource(properties = {"app.auth.login.max-attempts=3", "app.auth.login.window=15m"})
class LoginThrottleIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "correct horse battery";
    private static final String WRONG = "not the right password";

    @Autowired
    MockMvc mvc;

    private static String body(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    private void register(String email) throws Exception {
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body(email, PASSWORD)))
                .andExpect(status().isCreated());
    }

    private void loginExpecting(String email, String password, int expectedStatus) throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body(email, password)))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void locks_out_after_the_configured_number_of_failures() throws Exception {
        String email = "throttle-lockout@example.com";
        register(email);

        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body(email, WRONG)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        }

        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body(email, WRONG)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("TOO_MANY_ATTEMPTS"))
                .andExpect(header().exists("Retry-After"));

        // Even the correct password is refused while locked out — the throttle runs first.
        loginExpecting(email, PASSWORD, 429);
    }

    @Test
    void a_successful_login_clears_the_counter() throws Exception {
        String email = "throttle-clears@example.com";
        register(email);

        // Two failures — one under the limit of three...
        loginExpecting(email, WRONG, 401);
        loginExpecting(email, WRONG, 401);
        // ...then a success wipes them...
        loginExpecting(email, PASSWORD, 200);
        // ...so a fresh wrong attempt is a plain 401, not a lockout.
        loginExpecting(email, WRONG, 401);
    }

    @Test
    void an_unknown_email_is_throttled_too_so_a_lockout_never_confirms_existence() throws Exception {
        String email = "throttle-nobody@example.com"; // never registered

        for (int i = 0; i < 3; i++) {
            loginExpecting(email, WRONG, 401);
        }
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body(email, WRONG)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("TOO_MANY_ATTEMPTS"));
    }
}
