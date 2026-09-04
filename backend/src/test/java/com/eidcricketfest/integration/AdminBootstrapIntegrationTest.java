package com.eidcricketfest.integration;

import com.eidcricketfest.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@TestPropertySource(
        properties = "app.bootstrap.admin-token=test-bootstrap-token"
)
class AdminBootstrapIntegrationTest
        extends AuthTestSupport {

    @Autowired
    private AuthService authService;

    @BeforeEach
    void removeExistingPrivilegedAssignments() {

        jdbcTemplate.update(
                """
                DELETE FROM user_roles ur
                USING roles r
                WHERE ur.role_id = r.id
                  AND r.code IN ('ADMIN', 'ORGANIZER')
                """
        );
    }

    @Test
    void registerShouldStillCreatePlayerOnly()
            throws Exception {

        String email =
                "bootstrap-player-"
                + UUID.randomUUID()
                + "@example.com";

        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        jsonMapper.writeValueAsString(
                                                Map.of(
                                                        "displayName",
                                                        "Player Only",
                                                        "email",
                                                        email,
                                                        "password",
                                                        "StrongPassword123"
                                                )
                                        )
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.roles.length()").value(1))
                .andExpect(jsonPath("$.user.roles[0]").value("PLAYER"));
    }

    @Test
    void bootstrapAdminShouldCreateFirstPrivilegedAccount()
            throws Exception {

        String email =
                "bootstrap-admin-"
                + UUID.randomUUID()
                + "@example.com";

        var result =
                mockMvc.perform(
                                post("/api/v1/auth/bootstrap-admin")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                jsonMapper.writeValueAsString(
                                                        Map.of(
                                                                "bootstrapToken",
                                                                "test-bootstrap-token",
                                                                "displayName",
                                                                "First Admin",
                                                                "email",
                                                                email,
                                                                "password",
                                                                "StrongPassword123"
                                                        )
                                                )
                                        )
                        )
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.accessToken").isNotEmpty())
                        .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                        .andExpect(jsonPath("$.user.email").value(email))
                        .andReturn();

        var roles =
                result.getResponse()
                        .getContentAsString();

        assertThat(roles)
                .contains("PLAYER")
                .contains("SCORER")
                .contains("ORGANIZER")
                .contains("ADMIN");
    }

    @Test
    void bootstrapAdminShouldRejectInvalidToken()
            throws Exception {

        mockMvc.perform(
                        post("/api/v1/auth/bootstrap-admin")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        jsonMapper.writeValueAsString(
                                                Map.of(
                                                        "bootstrapToken",
                                                        "wrong-token",
                                                        "displayName",
                                                        "First Admin",
                                                        "email",
                                                        "invalid-bootstrap-"
                                                        + UUID.randomUUID()
                                                        + "@example.com",
                                                        "password",
                                                        "StrongPassword123"
                                                )
                                        )
                                )
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void bootstrapAdminShouldBeOneTimeOnly()
            throws Exception {

        bootstrapAdmin(
                "first-bootstrap-"
                + UUID.randomUUID()
                + "@example.com"
        );

        mockMvc.perform(
                        post("/api/v1/auth/bootstrap-admin")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        jsonMapper.writeValueAsString(
                                                Map.of(
                                                        "bootstrapToken",
                                                        "test-bootstrap-token",
                                                        "displayName",
                                                        "Second Admin",
                                                        "email",
                                                        "second-bootstrap-"
                                                        + UUID.randomUUID()
                                                        + "@example.com",
                                                        "password",
                                                        "StrongPassword123"
                                                )
                                        )
                                )
                )
                .andExpect(status().isConflict());
    }

    @Test
    void configuredBootstrapShouldCreateFirstPrivilegedAccount() {

        String email =
                "configured-bootstrap-"
                + UUID.randomUUID()
                + "@example.com";

        authService.bootstrapConfiguredAdmin(
                "Configured Admin",
                email,
                "StrongPassword123"
        );

        Integer privilegedRoles =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM users u
                        JOIN user_roles ur ON ur.user_id = u.id
                        JOIN roles r ON r.id = ur.role_id
                        WHERE LOWER(u.email) = LOWER(?)
                          AND r.code IN ('ADMIN', 'ORGANIZER', 'PLAYER', 'SCORER')
                        """,
                        Integer.class,
                        email
                );

        assertThat(privilegedRoles)
                .isEqualTo(4);
    }

    private void bootstrapAdmin(
            String email
    ) throws Exception {

        mockMvc.perform(
                        post("/api/v1/auth/bootstrap-admin")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        jsonMapper.writeValueAsString(
                                                Map.of(
                                                        "bootstrapToken",
                                                        "test-bootstrap-token",
                                                        "displayName",
                                                        "First Admin",
                                                        "email",
                                                        email,
                                                        "password",
                                                        "StrongPassword123"
                                                )
                                        )
                                )
                )
                .andExpect(status().isCreated());
    }
}
