package com.lifetracker.infrastructure;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Email verification and password reset over HTTP (MockMvc, real Postgres). The stub email sender is
 * replaced by a capturing double, so a test can pull the token and complete each round-trip.
 */
@AutoConfigureMockMvc
class EmailVerificationResetIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "correct horse battery";

    @Autowired
    MockMvc mvc;

    @Autowired
    CapturingEmailSender emails;

    private static String creds(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private String register(String email) throws Exception {
        MvcResult r = mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(creds(email, PASSWORD)))
                .andExpect(status().isCreated()).andReturn();
        return JsonPath.read(r.getResponse().getContentAsString(), "$.accessToken");
    }

    private void verify(String token) throws Exception {
        mvc.perform(post("/auth/verify-email").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void verification_round_trip_flips_the_flag_and_unlocks_sharing() throws Exception {
        String email = "verify-flow@example.com";
        String access = register(email);

        mvc.perform(get("/me").header("Authorization", bearer(access)))
                .andExpect(jsonPath("$.emailVerified").value(false));

        // Sharing is refused while unverified.
        mvc.perform(post("/me/share-link").header("Authorization", bearer(access)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));

        verify(emails.latestToken(CapturingEmailSender.Kind.VERIFICATION, email));

        mvc.perform(get("/me").header("Authorization", bearer(access)))
                .andExpect(jsonPath("$.emailVerified").value(true));
        mvc.perform(post("/me/share-link").header("Authorization", bearer(access)))
                .andExpect(status().isCreated());
    }

    @Test
    void a_bad_verification_token_is_400() throws Exception {
        mvc.perform(post("/auth/verify-email").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"not-a-real-token\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @Test
    void resend_issues_a_working_verification_token() throws Exception {
        String email = "resend-flow@example.com";
        String access = register(email);

        mvc.perform(post("/auth/verify-email/resend").header("Authorization", bearer(access)))
                .andExpect(status().isAccepted());

        verify(emails.latestToken(CapturingEmailSender.Kind.VERIFICATION, email));
        mvc.perform(get("/me").header("Authorization", bearer(access)))
                .andExpect(jsonPath("$.emailVerified").value(true));
    }

    @Test
    void password_reset_changes_the_password_and_revokes_sessions() throws Exception {
        String email = "reset-flow@example.com";
        register(email);

        // A live session whose refresh token we will watch die.
        MvcResult login = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(creds(email, PASSWORD)))
                .andExpect(status().isOk()).andReturn();
        String refreshToken = JsonPath.read(login.getResponse().getContentAsString(), "$.refreshToken");

        mvc.perform(post("/auth/password-reset").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isAccepted());
        String resetToken = emails.latestToken(CapturingEmailSender.Kind.PASSWORD_RESET, email);
        String newPassword = "a whole new password";
        mvc.perform(post("/auth/password-reset/confirm").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + resetToken + "\",\"newPassword\":\"" + newPassword + "\"}"))
                .andExpect(status().isNoContent());

        // Old password no longer works; the new one does.
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(creds(email, PASSWORD)))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(creds(email, newPassword)))
                .andExpect(status().isOk());

        // The pre-reset refresh token was revoked along with every other session.
        mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void a_reset_request_for_an_unknown_email_is_202_and_sends_nothing() throws Exception {
        mvc.perform(post("/auth/password-reset").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody-here@example.com\"}"))
                .andExpect(status().isAccepted());
        assertFalse(emails.hasSentTo("nobody-here@example.com"));
    }

    @Test
    void a_weak_new_password_is_422() throws Exception {
        String email = "reset-weak@example.com";
        register(email);
        mvc.perform(post("/auth/password-reset").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isAccepted());
        String resetToken = emails.latestToken(CapturingEmailSender.Kind.PASSWORD_RESET, email);

        mvc.perform(post("/auth/password-reset/confirm").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + resetToken + "\",\"newPassword\":\"short\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION"));
    }
}
