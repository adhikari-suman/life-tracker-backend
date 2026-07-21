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
 * The sharing endpoints over HTTP (MockMvc), owner-scoped to the token. Share Link lifecycle,
 * View Grant lifecycle, and the RFC 7807 error mappings.
 */
@AutoConfigureMockMvc
class SharingEndpointsIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;

    private String registerAndToken(String email) throws Exception {
        String creds = "{\"email\":\"" + email + "\",\"password\":\"correct horse battery\"}";
        MvcResult r = mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(creds))
                .andExpect(status().isCreated()).andReturn();
        return JsonPath.read(r.getResponse().getContentAsString(), "$.accessToken");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    void share_link_lifecycle() throws Exception {
        String token = registerAndToken("share-web-owner@example.com");

        mvc.perform(post("/me/share-link").header("Authorization", bearer(token)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").isNotEmpty());

        mvc.perform(post("/me/share-link").header("Authorization", bearer(token)))
                .andExpect(status().isOk()); // already on

        mvc.perform(get("/me/share-link").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").isNotEmpty());

        mvc.perform(delete("/me/share-link").header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        mvc.perform(get("/me/share-link").header("Authorization", bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SHARE_LINK_NOT_FOUND"));
    }

    @Test
    void view_grant_lifecycle() throws Exception {
        String owner = registerAndToken("grant-web-owner@example.com");
        registerAndToken("grant-web-viewer@example.com"); // grantee must exist

        MvcResult granted = mvc.perform(post("/me/view-grants").header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"grant-web-viewer@example.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.granteeEmail").value("grant-web-viewer@example.com"))
                .andReturn();
        String grantId = JsonPath.read(granted.getResponse().getContentAsString(), "$.id");

        mvc.perform(get("/me/view-grants").header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mvc.perform(post("/me/view-grants").header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"grant-web-viewer@example.com\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VIEW_GRANT_EXISTS"));

        mvc.perform(delete("/me/view-grants/" + grantId).header("Authorization", bearer(owner)))
                .andExpect(status().isNoContent());

        mvc.perform(get("/me/view-grants").header("Authorization", bearer(owner)))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void granting_to_an_unknown_email_is_404() throws Exception {
        String owner = registerAndToken("grant-unknown-owner@example.com");
        mvc.perform(post("/me/view-grants").header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"nobody@example.com\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GRANTEE_NOT_FOUND"));
    }

    @Test
    void sharing_requires_authentication() throws Exception {
        mvc.perform(post("/me/share-link")).andExpect(status().isUnauthorized());
        mvc.perform(get("/me/view-grants")).andExpect(status().isUnauthorized());
    }
}
