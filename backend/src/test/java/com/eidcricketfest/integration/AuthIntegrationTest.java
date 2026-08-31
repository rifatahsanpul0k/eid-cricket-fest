package com.eidcricketfest.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthIntegrationTest
        extends AuthTestSupport {

    @Test
    void registerShouldCreatePlayerAccount()
            throws Exception {

        String email =
                "register-"
                + UUID.randomUUID()
                + "@example.com";

        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        jsonMapper.writeValueAsString(
                                                Map.of(
                                                        "displayName",
                                                        "Rahim",

                                                        "email",
                                                        email,

                                                        "password",
                                                        "StrongPassword123"
                                                )
                                        )
                                )
                )
                .andExpect(
                        status().isCreated()
                )
                .andExpect(
                        jsonPath("$.accessToken")
                                .isNotEmpty()
                )
                .andExpect(
                        jsonPath("$.refreshToken")
                                .isNotEmpty()
                )
                .andExpect(
                        jsonPath("$.tokenType")
                                .value("Bearer")
                )
                .andExpect(
                        jsonPath("$.user.email")
                                .value(email)
                )
                .andExpect(
                        jsonPath("$.user.roles[0]")
                                .value("PLAYER")
                );
    }

    @Test
    void duplicateEmailShouldReturn409()
            throws Exception {

        TestTokens tokens =
                registerPlayer();

        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        jsonMapper.writeValueAsString(
                                                Map.of(
                                                        "displayName",
                                                        "Another User",

                                                        "email",
                                                        tokens.email(),

                                                        "password",
                                                        "AnotherPassword123"
                                                )
                                        )
                                )
                )
                .andExpect(
                        status().isConflict()
                );
    }

    @Test
    void incorrectPasswordShouldReturn401()
            throws Exception {

        TestTokens account =
                registerPlayer();

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        jsonMapper.writeValueAsString(
                                                Map.of(
                                                        "identifier",
                                                        account.email(),

                                                        "password",
                                                        "WrongPassword999"
                                                )
                                        )
                                )
                )
                .andExpect(
                        status().isUnauthorized()
                );
    }

    @Test
    void refreshShouldRotateRefreshToken()
            throws Exception {

        TestTokens tokens =
                registerPlayer();

        var result =
                mockMvc.perform(
                        post("/api/v1/auth/refresh")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        jsonMapper.writeValueAsString(
                                                Map.of(
                                                        "refreshToken",
                                                        tokens.refreshToken()
                                                )
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.accessToken")
                                .isNotEmpty()
                )
                .andExpect(
                        jsonPath("$.refreshToken")
                                .isNotEmpty()
                )
                .andReturn();

        var json =
                jsonMapper.readTree(
                        result.getResponse()
                                .getContentAsString()
                );

        String rotatedRefreshToken =
                json.get("refreshToken")
                        .asText();

        assertThat(rotatedRefreshToken)
                .isNotEqualTo(
                        tokens.refreshToken()
                );
    }

    @Test
    void reusedRefreshTokenShouldRevokeEntireFamily()
            throws Exception {

        TestTokens original =
                registerPlayer();

        var refreshResult =
                mockMvc.perform(
                        post("/api/v1/auth/refresh")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        jsonMapper.writeValueAsString(
                                                Map.of(
                                                        "refreshToken",
                                                        original.refreshToken()
                                                )
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andReturn();

        var json =
                jsonMapper.readTree(
                        refreshResult
                                .getResponse()
                                .getContentAsString()
                );

        String newRefreshToken =
                json.get("refreshToken")
                        .asText();

        /*
         * Attacker reuses old token.
         */
        mockMvc.perform(
                        post("/api/v1/auth/refresh")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        jsonMapper.writeValueAsString(
                                                Map.of(
                                                        "refreshToken",
                                                        original.refreshToken()
                                                )
                                        )
                                )
                )
                .andExpect(
                        status().isUnauthorized()
                );

        /*
         * Reuse detection should have revoked
         * the whole session family.
         */
        mockMvc.perform(
                        post("/api/v1/auth/refresh")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        jsonMapper.writeValueAsString(
                                                Map.of(
                                                        "refreshToken",
                                                        newRefreshToken
                                                )
                                        )
                                )
                )
                .andExpect(
                        status().isUnauthorized()
                );
    }

    @Test
    void logoutShouldInvalidateRefreshFamily()
            throws Exception {

        TestTokens tokens =
                registerPlayer();

        mockMvc.perform(
                        post("/api/v1/auth/logout")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        jsonMapper.writeValueAsString(
                                                Map.of(
                                                        "refreshToken",
                                                        tokens.refreshToken()
                                                )
                                        )
                                )
                )
                .andExpect(
                        status().isNoContent()
                );

        mockMvc.perform(
                        post("/api/v1/auth/refresh")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        jsonMapper.writeValueAsString(
                                                Map.of(
                                                        "refreshToken",
                                                        tokens.refreshToken()
                                                )
                                        )
                                )
                )
                .andExpect(
                        status().isUnauthorized()
                );
    }
}
