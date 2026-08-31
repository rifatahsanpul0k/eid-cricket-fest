package com.eidcricketfest.integration;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public abstract class AuthTestSupport
        extends AbstractIntegrationTest {

    protected TestTokens registerPlayer()
            throws Exception {

        String email =
                "player-"
                + UUID.randomUUID()
                + "@example.com";

        return registerPlayer(email);
    }

    protected TestTokens registerPlayer(
            String email
    ) throws Exception {

        String password =
                "StrongPassword123";

        MvcResult result =
                mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        jsonMapper.writeValueAsString(
                                                Map.of(
                                                        "displayName",
                                                        "Test Player",

                                                        "email",
                                                        email,

                                                        "password",
                                                        password
                                                )
                                        )
                                )
                )
                .andExpect(status().isCreated())
                .andReturn();

        var json =
                jsonMapper.readTree(
                        result.getResponse()
                                .getContentAsString()
                );

        return new TestTokens(
                json.get("accessToken").asText(),
                json.get("refreshToken").asText(),
                email,
                password
        );
    }

    protected TestTokens login(
            String identifier,
            String password
    ) throws Exception {

        MvcResult result =
                mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        jsonMapper.writeValueAsString(
                                                Map.of(
                                                        "identifier",
                                                        identifier,
                                                        "password",
                                                        password
                                                )
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andReturn();

        var json =
                jsonMapper.readTree(
                        result.getResponse()
                                .getContentAsString()
                );

        return new TestTokens(
                json.get("accessToken").asText(),
                json.get("refreshToken").asText(),
                identifier,
                password
        );
    }

    protected void addRole(
            String email,
            String role
    ) {

        jdbcTemplate.update(
                """
                INSERT INTO user_roles (
                    user_id,
                    role_id
                )
                SELECT
                    u.id,
                    r.id
                FROM users u
                CROSS JOIN roles r
                WHERE LOWER(u.email) = LOWER(?)
                  AND r.code = ?
                ON CONFLICT DO NOTHING
                """,
                email,
                role
        );
    }

    protected record TestTokens(
            String accessToken,
            String refreshToken,
            String email,
            String password
    ) {}
}
