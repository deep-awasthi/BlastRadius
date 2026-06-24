package com.blastradius;

import com.blastradius.dto.LoginRequest;
import com.blastradius.dto.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Auth and Discovery REST APIs.
 * Uses H2 in-memory DB; no Redis (caching disabled in test profile).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BlastRadiusIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    // ── Auth ─────────────────────────────────────────────────

    @Test
    void register_validRequest_returns201() throws Exception {
        RegisterRequest req = new RegisterRequest("integtest_user", "password123", "USER");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.data.username").value("integtest_user"));
    }

    @Test
    void register_duplicateUsername_returns409() throws Exception {
        RegisterRequest req = new RegisterRequest("dup_user", "password123", "USER");

        // First registration should succeed
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated());

        // Second registration should conflict
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void register_shortUsername_returns400() throws Exception {
        RegisterRequest req = new RegisterRequest("ab", "password", "USER");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void login_validCredentials_returnsTokens() throws Exception {
        // Register first
        RegisterRequest reg = new RegisterRequest("login_test_user", "pass1234", "USER");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg)));

        // Now login
        LoginRequest login = new LoginRequest("login_test_user", "pass1234");
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        // Register first
        RegisterRequest reg = new RegisterRequest("wrongpass_user", "correctpass", "USER");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg)));

        // Wrong password
        LoginRequest login = new LoginRequest("wrongpass_user", "wrongpass");
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
            .andExpect(status().isUnauthorized());
    }

    // ── Protected endpoints ───────────────────────────────────

    @Test
    void dashboard_withoutToken_redirectsOrForbids() throws Exception {
        // Spring Security either redirects (302) or forbids (403) unauthenticated requests
        mockMvc.perform(get("/api/dashboard?scanId=1"))
            .andExpect(result -> {
                int status = result.getResponse().getStatus();
                assertTrue(status == 302 || status == 401 || status == 403,
                        "Expected redirect or auth error but got: " + status);
            });
    }

    @Test
    void components_withValidToken_returns200() throws Exception {
        // Register + login to get token
        RegisterRequest reg = new RegisterRequest("token_test_user", "pass12345", "USER");
        MvcResult regResult = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg)))
            .andReturn();

        String responseBody = regResult.getResponse().getContentAsString();
        String token = objectMapper.readTree(responseBody)
                .path("data").path("accessToken").asText();

        mockMvc.perform(get("/api/components?scanId=999")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }
}
