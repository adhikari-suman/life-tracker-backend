package com.lifetracker.infrastructure;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The public auth endpoints over HTTP (MockMvc), against real Postgres + real crypto. Proves the
 * wire shapes, the {@code sessionId.secret} refresh token round-trip, and the RFC 7807 error mapping.
 */
@AutoConfigureMockMvc
class AuthEndpointsIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;

    private static String json(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    @Test
    void register_then_login_then_refresh_then_replay() throws Exception {
        String creds = json("web-flow@example.com", "correct horse battery");

        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(creds))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").isNumber())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());

        MvcResult login = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(creds))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();
        String refreshToken = JsonPath.read(login.getResponse().getContentAsString(), "$.refreshToken");

        String refreshBody = "{\"refreshToken\":\"" + refreshToken + "\"}";
        MvcResult refreshed = mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(refreshBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();
        String rotated = JsonPath.read(refreshed.getResponse().getContentAsString(), "$.refreshToken");
        assertNotEquals(refreshToken, rotated);

        // Replay the retired refresh token -> 401, indistinguishable.
        mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(refreshBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void wrong_password_is_401() throws Exception {
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json("web-wrongpw@example.com", "correct horse battery")))
                .andExpect(status().isCreated());

        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json("web-wrongpw@example.com", "not the password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void duplicate_registration_is_409() throws Exception {
        String creds = json("web-dup@example.com", "correct horse battery");
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(creds))
                .andExpect(status().isCreated());
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(creds))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
    }

    @Test
    void weak_password_is_422() throws Exception {
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json("web-weak@example.com", "short")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION"));
    }
}
