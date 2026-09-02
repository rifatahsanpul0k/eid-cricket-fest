package com.eidcricketfest.integration;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.http.MediaType;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MatchOperationsIntegrationTest
        extends AuthTestSupport {

    @Test
    void organizerCanRescheduleMatchAndReadAuditHistory()
            throws Exception {

        TestTokens organizer = organizer();
        Long matchId = createTournamentMatch("SCHEDULED");
        Long venueId = venue("Operations Reschedule");

        mockMvc.perform(
                        patch(
                                "/api/v1/matches/{matchId}/operations/reschedule",
                                matchId
                        )
                                .header("Authorization", bearer(organizer))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(
                                        Map.of(
                                                "scheduledAt",
                                                "2026-12-16T04:00:00Z",
                                                "venueId",
                                                venueId,
                                                "oversPerInnings",
                                                8,
                                                "reason",
                                                "Rain forecast moved the fixture"
                                        )
                                ))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.oversPerInnings").value(8))
                .andExpect(jsonPath("$.operationHistory[0].operationType")
                        .value("RESCHEDULE"))
                .andExpect(jsonPath("$.operationHistory[0].reason")
                        .value("Rain forecast moved the fixture"));

        assertThat(auditCount(matchId, "RESCHEDULE"))
                .isEqualTo(1L);
    }

    @Test
    void playerCannotUseMatchOperations()
            throws Exception {

        TestTokens player = registerPlayer();
        Long matchId = createTournamentMatch("SCHEDULED");

        mockMvc.perform(
                        post(
                                "/api/v1/matches/{matchId}/operations/postpone",
                                matchId
                        )
                                .header("Authorization", bearer(player))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(
                                        Map.of(
                                                "reason",
                                                "Player should not manage fixtures"
                                        )
                                ))
                )
                .andExpect(status().isForbidden());

        assertThat(auditCount(matchId, "POSTPONE"))
                .isZero();
    }

    @Test
    void resetTossDeletesPersistedTossAndReturnsReady()
            throws Exception {

        TestTokens admin = admin();
        Long matchId = createTournamentMatch("TOSS_COMPLETED");
        Long winnerSideId =
                jdbcTemplate.queryForObject(
                        """
                        SELECT team_a_side_id
                        FROM matches
                        WHERE id = ?
                        """,
                        Long.class,
                        matchId
                );

        jdbcTemplate.update(
                """
                INSERT INTO match_tosses (
                    match_id,
                    winner_match_side_id,
                    decision
                )
                VALUES (?, ?, 'BAT')
                """,
                matchId,
                winnerSideId
        );

        mockMvc.perform(
                        post(
                                "/api/v1/matches/{matchId}/operations/reset-toss",
                                matchId
                        )
                                .header("Authorization", bearer(admin))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(
                                        Map.of(
                                                "reason",
                                                "Wrong toss winner recorded"
                                        )
                                ))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.tossCompleted").value(false));

        assertThat(
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM match_tosses
                        WHERE match_id = ?
                        """,
                        Long.class,
                        matchId
                )
        ).isZero();
    }

    @Test
    void suspendAndResumeRestoresPriorStatus()
            throws Exception {

        TestTokens organizer = organizer();
        Long matchId = createTournamentMatch("LIVE");

        mockMvc.perform(
                        post(
                                "/api/v1/matches/{matchId}/operations/suspend",
                                matchId
                        )
                                .header("Authorization", bearer(organizer))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(
                                        Map.of("reason", "Bad light")
                                ))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));

        mockMvc.perform(
                        post(
                                "/api/v1/matches/{matchId}/operations/resume",
                                matchId
                        )
                                .header("Authorization", bearer(organizer))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(
                                        Map.of("reason", "Light improved")
                                ))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LIVE"));
    }

    @Test
    void rematchSupersedesOriginalAndCreatesFreshLinkedMatch()
            throws Exception {

        TestTokens admin = admin();
        Long originalMatchId = createTournamentMatch("COMPLETED");

        mockMvc.perform(
                        post(
                                "/api/v1/matches/{matchId}/operations/rematch",
                                originalMatchId
                        )
                                .header("Authorization", bearer(admin))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(
                                        Map.of(
                                                "reason",
                                                "Result voided by committee",
                                                "scheduledAt",
                                                "2026-12-20T04:00:00Z"
                                        )
                                ))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.rematchOfMatchId")
                        .value(originalMatchId));

        Map<String, Object> original =
                jdbcTemplate.queryForMap(
                        """
                        SELECT result_status,
                               superseded_by_match_id
                        FROM matches
                        WHERE id = ?
                        """,
                        originalMatchId
                );

        assertThat(original.get("result_status"))
                .isEqualTo("SUPERSEDED");

        assertThat(original.get("superseded_by_match_id"))
                .isNotNull();

        Long rematchId =
                ((Number) original.get("superseded_by_match_id"))
                        .longValue();

        assertThat(
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM innings
                        WHERE match_id = ?
                        """,
                        Long.class,
                        rematchId
                )
        ).isZero();

        assertThat(auditCount(originalMatchId, "ORDER_REMATCH"))
                .isEqualTo(1L);
    }

    private TestTokens organizer()
            throws Exception {
        TestTokens tokens = registerPlayer();
        addRole(tokens.email(), "ORGANIZER");
        return login(tokens.email(), tokens.password());
    }

    private TestTokens admin()
            throws Exception {
        TestTokens tokens = registerPlayer();
        addRole(tokens.email(), "ADMIN");
        return login(tokens.email(), tokens.password());
    }

    private Long createTournamentMatch(String status) {
        String suffix = UUID.randomUUID().toString();
        Long tournamentId =
                insertAndReturnId(
                        """
                        INSERT INTO tournaments (name)
                        VALUES (?)
                        """,
                        "Operations Cup " + suffix
                );

        Long editionId =
                insertAndReturnId(
                        """
                        INSERT INTO tournament_editions (
                            tournament_id,
                            name,
                            overs_per_innings,
                            playing_xi_size
                        )
                        VALUES (?, ?, 5, 2)
                        """,
                        tournamentId,
                        "Edition " + suffix
                );

        Long venueId = venue("Operations Ground " + suffix);
        Long teamA = tournamentTeam(editionId, "Ops Alpha " + suffix);
        Long teamB = tournamentTeam(editionId, "Ops Beta " + suffix);

        Long matchId =
                insertAndReturnId(
                        """
                        INSERT INTO matches (
                            tournament_edition_id,
                            team_a_id,
                            team_b_id,
                            venue_id,
                            stage,
                            round_number,
                            match_number,
                            scheduled_at,
                            overs_per_innings,
                            status,
                            result_status,
                            result_type
                        )
                        VALUES (?, ?, ?, ?, 'LEAGUE', 1, 1, ?, 5, ?, ?, ?)
                        """,
                        editionId,
                        teamA,
                        teamB,
                        venueId,
                        Instant.parse("2026-12-15T04:00:00Z"),
                        status,
                        "COMPLETED".equals(status) ? "OFFICIAL" : null,
                        "COMPLETED".equals(status) ? "TIE" : null
                );

        Long sideA =
                matchSide(
                        matchId,
                        "A",
                        "Ops Alpha",
                        teamA
                );

        Long sideB =
                matchSide(
                        matchId,
                        "B",
                        "Ops Beta",
                        teamB
                );

        jdbcTemplate.update(
                """
                UPDATE matches
                SET team_a_side_id = ?,
                    team_b_side_id = ?
                WHERE id = ?
                """,
                sideA,
                sideB,
                matchId
        );

        return matchId;
    }

    private Long tournamentTeam(
            Long editionId,
            String teamName
    ) {
        Long teamId =
                insertAndReturnId(
                        """
                        INSERT INTO teams (name, short_name)
                        VALUES (?, ?)
                        """,
                        teamName,
                        teamName.substring(0, Math.min(10, teamName.length()))
                );

        return insertAndReturnId(
                """
                INSERT INTO tournament_teams (
                    tournament_edition_id,
                    team_id
                )
                VALUES (?, ?)
                """,
                editionId,
                teamId
        );
    }

    private Long matchSide(
            Long matchId,
            String sideKey,
            String displayName,
            Long tournamentTeamId
    ) {
        return insertAndReturnId(
                """
                INSERT INTO match_sides (
                    match_id,
                    side_key,
                    display_name,
                    tournament_team_id
                )
                VALUES (?, ?, ?, ?)
                """,
                matchId,
                sideKey,
                displayName,
                tournamentTeamId
        );
    }

    private Long venue(String name) {
        return insertAndReturnId(
                """
                INSERT INTO venues (name)
                VALUES (?)
                """,
                name
        );
    }

    private Long auditCount(
            Long matchId,
            String operationType
    ) {
        return jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM match_operation_audits
                WHERE match_id = ?
                  AND operation_type = ?
                """,
                Long.class,
                matchId,
                operationType
        );
    }

    private Long insertAndReturnId(
            String sql,
            Object... args
    ) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement statement =
                    connection.prepareStatement(
                            sql,
                            new String[] { "id" }
                    );

            for (int i = 0; i < args.length; i++) {
                Object value = args[i];

                if (value instanceof Instant instant) {
                    value = Timestamp.from(instant);
                }

                statement.setObject(i + 1, value);
            }

            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();

        if (key == null) {
            throw new IllegalStateException(
                    "Insert did not return a generated key"
            );
        }

        return key.longValue();
    }

    private String bearer(TestTokens tokens) {
        return "Bearer " + tokens.accessToken();
    }
}
