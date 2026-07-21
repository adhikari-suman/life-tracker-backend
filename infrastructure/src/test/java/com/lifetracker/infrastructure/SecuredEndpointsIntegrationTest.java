package com.lifetracker.infrastructure;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The security filter chain over HTTP: protected endpoints reject missing/garbage tokens (401) and
 * accept a real access token; JWKS is public. Against real Postgres + real RS256.
 */
@AutoConfigureMockMvc
class SecuredEndpointsIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;

    private String registerAndLogin(String email) throws Exception {
        String creds = "{\"email\":\"" + email + "\",\"password\":\"correct horse battery\"}";
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(creds))
                .andExpect(status().isCreated());
        MvcResult login = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(creds))
                .andExpect(status().isOk()).andReturn();
        return JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
    }

    @Test
    void me_requires_a_token() throws Exception {
        mvc.perform(get("/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void me_rejects_a_garbage_token() throws Exception {
        mvc.perform(get("/me").header("Authorization", "Bearer not-a-real-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_returns_the_authenticated_user_with_a_valid_token() throws Exception {
        String token = registerAndLogin("secured-me@example.com");
        mvc.perform(get("/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("secured-me@example.com"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void jwks_is_public_and_serves_the_public_key() throws Exception {
        mvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys[0].kty").value("RSA"))
                .andExpect(jsonPath("$.keys[0].kid").isNotEmpty());
    }
}
