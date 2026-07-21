package com.lifetracker.infrastructure;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The authenticated session-management endpoints over HTTP: listing (with the current-device flag),
 * logout, revoking one device, and signing out everywhere. Logout/revoke kill the Session, so the
 * matching refresh token stops working (the short-lived access token itself is stateless).
 *
 * <p>Note: register auto-logs-in (ADR-0007), so registering already opens one Session.
 */
@AutoConfigureMockMvc
class SessionEndpointsIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;

    private record Tokens(String access, String refresh) {
    }

    private static String creds(String email) {
        return "{\"email\":\"" + email + "\",\"password\":\"correct horse battery\"}";
    }

    private Tokens tokensFrom(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        return new Tokens(JsonPath.read(body, "$.accessToken"), JsonPath.read(body, "$.refreshToken"));
    }

    private Tokens register(String email) throws Exception {
        return tokensFrom(mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(creds(email)))
                .andExpect(status().isCreated()).andReturn());
    }

    private Tokens login(String email) throws Exception {
        return tokensFrom(mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(creds(email)))
                .andExpect(status().isOk()).andReturn());
    }

    private void refreshExpectingUnauthorized(String refreshToken) throws Exception {
        mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void lists_sessions_flagging_the_current_one() throws Exception {
        Tokens t = register("sess-list@example.com"); // one session, from register's auto-login

        mvc.perform(get("/auth/sessions").header("Authorization", "Bearer " + t.access()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].current").value(true))
                .andExpect(jsonPath("$[0].deviceLabel").isNotEmpty());
    }

    @Test
    void logout_revokes_the_current_session() throws Exception {
        Tokens t = register("sess-logout@example.com");

        mvc.perform(post("/auth/logout").header("Authorization", "Bearer " + t.access()))
                .andExpect(status().isNoContent());

        refreshExpectingUnauthorized(t.refresh());
    }

    @Test
    void revoking_another_device_stops_that_devices_refresh() throws Exception {
        Tokens victim = register("sess-revoke-one@example.com"); // session 1
        Tokens actor = login("sess-revoke-one@example.com");     // session 2
        String victimSessionId = victim.refresh().substring(0, victim.refresh().indexOf('.'));

        mvc.perform(delete("/auth/sessions/" + victimSessionId).header("Authorization", "Bearer " + actor.access()))
                .andExpect(status().isNoContent());

        refreshExpectingUnauthorized(victim.refresh());
    }

    @Test
    void sign_out_everywhere_revokes_all_sessions() throws Exception {
        register("sess-all@example.com");            // session 1
        Tokens last = login("sess-all@example.com"); // session 2

        mvc.perform(get("/auth/sessions").header("Authorization", "Bearer " + last.access()))
                .andExpect(jsonPath("$.length()").value(2));

        mvc.perform(delete("/auth/sessions").header("Authorization", "Bearer " + last.access()))
                .andExpect(status().isNoContent());

        mvc.perform(get("/auth/sessions").header("Authorization", "Bearer " + last.access()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
