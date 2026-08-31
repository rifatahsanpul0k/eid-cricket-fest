package com.eidcricketfest.integration.cricket;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ScorerConsoleAccessIntegrationTest
        extends CricketIntegrationSupport {

    @Test
    void anonymousMustAuthenticateForScorerConsole()
            throws Exception {

        mockMvc.perform(
                        get("/api/v1/scorer/matches")
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void playerCannotUseScorerConsole()
            throws Exception {

        TestAccount player =
                registerAccount();

        mockMvc.perform(
                        get("/api/v1/scorer/matches")
                                .header(
                                        "Authorization",
                                        "Bearer " + player.accessToken()
                                )
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void unassignedScorerCanListEmptyWorklistButCannotOpenMatch()
            throws Exception {

        Scenario scenario =
                createScenario(2);

        TestAccount scorer =
                registerAccount();

        addRole(
                scorer.email(),
                "SCORER"
        );

        TestAccount loggedInScorer =
                login(
                        scorer.email(),
                        scorer.password()
                );

        mockMvc.perform(
                        get("/api/v1/scorer/matches")
                                .header(
                                        "Authorization",
                                        "Bearer " + loggedInScorer.accessToken()
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(
                        get(
                                "/api/v1/scorer/matches/{matchId}",
                                scenario.matchId()
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + loggedInScorer.accessToken()
                                )
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void assignedScorerCanSeeWorklistAndMatchState()
            throws Exception {

        Scenario scenario =
                createScenario(2);

        TestAccount scorer =
                registerAccount();

        addRole(
                scorer.email(),
                "SCORER"
        );

        Long scorerUserId =
                userIdForEmail(scorer.email());

        assignScorer(
                scenario.matchId(),
                scorerUserId
        );

        TestAccount loggedInScorer =
                login(
                        scorer.email(),
                        scorer.password()
                );

        mockMvc.perform(
                        get("/api/v1/scorer/matches")
                                .header(
                                        "Authorization",
                                        "Bearer " + loggedInScorer.accessToken()
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$[0].match.id")
                                .value(scenario.matchId())
                )
                .andExpect(
                        jsonPath("$[0].assignedToCurrentUser")
                                .value(true)
                );

        mockMvc.perform(
                        get(
                                "/api/v1/scorer/matches/{matchId}",
                                scenario.matchId()
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + loggedInScorer.accessToken()
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.match.id")
                                .value(scenario.matchId())
                )
                .andExpect(
                        jsonPath("$.teamAPlayingXi.length()")
                                .value(3)
                )
                .andExpect(
                        jsonPath("$.teamBPlayingXi.length()")
                                .value(3)
                )
                .andExpect(
                        jsonPath("$.teamAPlayingXi[0].playingXiId")
                                .value(scenario.a1().xiId())
                );
    }

    private TestAccount registerAccount()
            throws Exception {

        String email =
                "scorer-"
                + UUID.randomUUID()
                + "@example.com";

        String password =
                "StrongPassword123";

        MvcResult result =
                mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        jsonMapper.writeValueAsString(
                                                Map.of(
                                                        "displayName",
                                                        "Test Scorer",
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

        return new TestAccount(
                json.get("accessToken").asText(),
                email,
                password
        );
    }

    private TestAccount login(
            String email,
            String password
    ) throws Exception {

        MvcResult result =
                mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        jsonMapper.writeValueAsString(
                                                Map.of(
                                                        "identifier",
                                                        email,
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

        return new TestAccount(
                json.get("accessToken").asText(),
                email,
                password
        );
    }

    private void addRole(
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

    private Long userIdForEmail(String email) {

        return jdbcTemplate.queryForObject(
                """
                SELECT id
                FROM users
                WHERE LOWER(email) = LOWER(?)
                """,
                Long.class,
                email
        );
    }

    private void assignScorer(
            Long matchId,
            Long userId
    ) {

        jdbcTemplate.update(
                """
                INSERT INTO match_scorers (
                    match_id,
                    user_id,
                    primary_scorer,
                    assigned_at,
                    assigned_by_user_id
                )
                VALUES (
                    ?,
                    ?,
                    true,
                    CURRENT_TIMESTAMP,
                    ?
                )
                """,
                matchId,
                userId,
                userId
        );
    }

    private record TestAccount(
            String accessToken,
            String email,
            String password
    ) {}
}
