package com.eidcricketfest.integration;

import com.eidcricketfest.knockout.service.KnockoutService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TournamentFinalizationIntegrationTest
        extends AuthTestSupport {

    @Autowired
    private KnockoutService knockoutService;

    @Test
    void officialFinalCompletionPersistsChampionAndHistory()
            throws Exception {

        FinalFixture fixture =
                createFinalFixture(
                        "OFFICIAL",
                        "Alpha won by 8 wickets"
                );

        knockoutService.completeEditionFromFinal(fixture.matchId());

        Map<String, Object> edition =
                jdbcTemplate.queryForMap(
                        """
                        SELECT status,
                               champion_tournament_team_id,
                               runner_up_tournament_team_id,
                               final_match_id,
                               completed_at
                        FROM tournament_editions
                        WHERE id = ?
                        """,
                        fixture.editionId()
                );

        assertThat(edition.get("status")).isEqualTo("COMPLETED");
        assertThat(((Number) edition.get("champion_tournament_team_id")).longValue())
                .isEqualTo(fixture.teamAId());
        assertThat(((Number) edition.get("runner_up_tournament_team_id")).longValue())
                .isEqualTo(fixture.teamBId());
        assertThat(((Number) edition.get("final_match_id")).longValue())
                .isEqualTo(fixture.matchId());
        assertThat(edition.get("completed_at")).isNotNull();

        mockMvc.perform(
                        get(
                                "/api/v1/tournaments/{tournamentId}/history",
                                fixture.tournamentId()
                        )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.editions[0].champion.name", containsString("Alpha")))
                .andExpect(jsonPath("$.editions[0].runnerUp.name", containsString("Beta")))
                .andExpect(jsonPath("$.editions[0].finalResult").value("Alpha won by 8 wickets"));
    }

    @Test
    void reviewedOrVoidedFinalDoesNotRemainCompleted()
            throws Exception {

        TestTokens organizer = loginWithRole("ORGANIZER");
        FinalFixture reviewed =
                createFinalFixture(
                        "UNDER_REVIEW",
                        "Under review"
                );

        knockoutService.completeEditionFromFinal(reviewed.matchId());

        String reviewedStatus =
                jdbcTemplate.queryForObject(
                        "SELECT status FROM tournament_editions WHERE id = ?",
                        String.class,
                        reviewed.editionId()
                );

        assertThat(reviewedStatus).isEqualTo("ONGOING");

        FinalFixture official =
                createFinalFixture(
                        "OFFICIAL",
                        "Alpha won"
                );

        knockoutService.completeEditionFromFinal(official.matchId());

        mockMvc.perform(
                        post(
                                "/api/v1/matches/{matchId}/operations/void-result",
                                official.matchId()
                        )
                                .header("Authorization", bearer(organizer))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        jsonMapper.writeValueAsString(
                                                Map.of(
                                                        "reason",
                                                        "Final result was invalid"
                                                )
                                        )
                                )
                )
                .andExpect(status().isOk());

        Map<String, Object> edition =
                jdbcTemplate.queryForMap(
                        """
                        SELECT status,
                               champion_tournament_team_id,
                               runner_up_tournament_team_id,
                               final_match_id,
                               completed_at
                        FROM tournament_editions
                        WHERE id = ?
                        """,
                        official.editionId()
                );

        assertThat(edition.get("status")).isEqualTo("ONGOING");
        assertThat(edition.get("champion_tournament_team_id")).isNull();
        assertThat(edition.get("runner_up_tournament_team_id")).isNull();
        assertThat(edition.get("final_match_id")).isNull();
        assertThat(edition.get("completed_at")).isNull();
    }

    @Test
    void awardsAreOrganizerManagedAndReadBackWithTeamContext()
            throws Exception {

        TestTokens organizer = loginWithRole("ORGANIZER");
        TestTokens player = registerPlayer();
        FinalFixture fixture =
                createFinalFixture(
                        "OFFICIAL",
                        "Alpha won"
                );
        Long registrationId =
                createRosteredRegistration(
                        fixture.editionId(),
                        fixture.teamAId(),
                        "Award Player"
                );

        knockoutService.completeEditionFromFinal(fixture.matchId());

        mockMvc.perform(
                        get(
                                "/api/v1/tournament-editions/{editionId}/awards/player-options",
                                fixture.editionId()
                        )
                                .header("Authorization", bearer(player))
                )
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        get(
                                "/api/v1/tournament-editions/{editionId}/awards/player-options",
                                fixture.editionId()
                        )
                                .header("Authorization", bearer(organizer))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].registrationId").value(registrationId))
                .andExpect(jsonPath("$[0].teamName", containsString("Alpha")));

        mockMvc.perform(
                        post(
                                "/api/v1/tournament-editions/{editionId}/awards",
                                fixture.editionId()
                        )
                                .header("Authorization", bearer(player))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(awardJson(registrationId))
                )
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        post(
                                "/api/v1/tournament-editions/{editionId}/awards",
                                fixture.editionId()
                        )
                                .header("Authorization", bearer(organizer))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(awardJson(registrationId))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.playerName").value("Award Player"))
                .andExpect(jsonPath("$.teamName", containsString("Alpha")));

        mockMvc.perform(
                        get(
                                "/api/v1/tournament-editions/{editionId}/awards",
                                fixture.editionId()
                        )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Tournament MVP"))
                .andExpect(jsonPath("$[0].teamName", containsString("Alpha")));

        mockMvc.perform(
                        get(
                                "/api/v1/tournaments/{tournamentId}/history",
                                fixture.tournamentId()
                        )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.editions[0].awards[0].teamName", containsString("Alpha")))
                .andExpect(jsonPath("$.editions[0].awards[0].title").value("Tournament MVP"));
    }

    private TestTokens loginWithRole(String role)
            throws Exception {

        TestTokens account = registerPlayer();
        addRole(account.email(), role);

        return login(account.email(), account.password());
    }

    private String awardJson(Long registrationId)
            throws Exception {

        return jsonMapper.writeValueAsString(
                Map.of(
                        "registrationId",
                        registrationId,
                        "awardType",
                        "PLAYER_OF_TOURNAMENT",
                        "title",
                        "Tournament MVP",
                        "notes",
                        "Led the champion side"
                )
        );
    }

    private FinalFixture createFinalFixture(
            String resultStatus,
            String resultSummary
    ) {
        Long tournamentId =
                insertAndReturnId(
                        "INSERT INTO tournaments (name) VALUES (?) RETURNING id",
                        "Finalization " + UUID.randomUUID()
                );

        Long editionId =
                insertAndReturnId(
                        """
                        INSERT INTO tournament_editions (
                            tournament_id,
                            name,
                            start_date,
                            end_date,
                            overs_per_innings,
                            squad_size,
                            playing_xi_size,
                            status,
                            registration_fee,
                            registration_currency,
                            win_points,
                            tie_points,
                            no_result_points,
                            loss_points
                        )
                        VALUES (?, ?, '2026-06-01', '2026-06-10', 5, 3, 3,
                                'ONGOING', 0, 'BDT', 2, 1, 1, 0)
                        RETURNING id
                        """,
                        tournamentId,
                        "Edition " + UUID.randomUUID()
                );

        Long teamAId = tournamentTeam(editionId, "Alpha");
        Long teamBId = tournamentTeam(editionId, "Beta");

        Long matchId =
                insertAndReturnId(
                        """
                        INSERT INTO matches (
                            tournament_edition_id,
                            team_a_id,
                            team_b_id,
                            stage,
                            round_number,
                            match_number,
                            overs_per_innings,
                            status,
                            winner_team_id,
                            result_status,
                            result_type,
                            result_summary
                        )
                        VALUES (?, ?, ?, 'FINAL', 1, 99, 5, 'COMPLETED',
                                ?, ?, 'WICKETS', ?)
                        RETURNING id
                        """,
                        editionId,
                        teamAId,
                        teamBId,
                        teamAId,
                        resultStatus,
                        resultSummary
                );

        Long sideAId =
                matchSide(
                        matchId,
                        "A",
                        "Alpha",
                        teamAId
                );

        Long sideBId =
                matchSide(
                        matchId,
                        "B",
                        "Beta",
                        teamBId
                );

        jdbcTemplate.update(
                """
                UPDATE matches
                SET team_a_side_id = ?,
                    team_b_side_id = ?,
                    winner_side_id = ?
                WHERE id = ?
                """,
                sideAId,
                sideBId,
                sideAId,
                matchId
        );

        return new FinalFixture(
                tournamentId,
                editionId,
                teamAId,
                teamBId,
                matchId
        );
    }

    private Long tournamentTeam(
            Long editionId,
            String name
    ) {
        Long teamId =
                insertAndReturnId(
                        "INSERT INTO teams (name) VALUES (?) RETURNING id",
                        name + " " + UUID.randomUUID()
                );

        return insertAndReturnId(
                """
                INSERT INTO tournament_teams (
                    tournament_edition_id,
                    team_id
                )
                VALUES (?, ?)
                RETURNING id
                """,
                editionId,
                teamId
        );
    }

    private Long createRosteredRegistration(
            Long editionId,
            Long tournamentTeamId,
            String playerName
    ) {
        Long playerId =
                insertAndReturnId(
                        """
                        INSERT INTO players (
                            full_name,
                            primary_category_id
                        )
                        VALUES (?, 1)
                        RETURNING id
                        """,
                        playerName
                );

        Long registrationId =
                insertAndReturnId(
                        """
                        INSERT INTO player_registrations (
                            tournament_edition_id,
                            player_id,
                            category_id,
                            fee_amount,
                            status,
                            registered_at,
                            approved_at
                        )
                        VALUES (?, ?, 1, 0, 'APPROVED', ?, ?)
                        RETURNING id
                        """,
                        editionId,
                        playerId,
                        Timestamp.from(Instant.now()),
                        Timestamp.from(Instant.now())
                );

        insertAndReturnId(
                """
                INSERT INTO team_roster_entries (
                    tournament_edition_id,
                    tournament_team_id,
                    player_registration_id,
                    acquisition_type,
                    status,
                    joined_at
                )
                VALUES (?, ?, ?, 'DRAFT', 'ACTIVE', ?)
                RETURNING id
                """,
                editionId,
                tournamentTeamId,
                registrationId,
                Timestamp.from(Instant.now())
        );

        return registrationId;
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
                RETURNING id
                """,
                matchId,
                sideKey,
                displayName,
                tournamentTeamId
        );
    }

    private Long insertAndReturnId(
            String sql,
            Object... args
    ) {
        return jdbcTemplate.queryForObject(
                sql,
                Long.class,
                args
        );
    }

    private String bearer(TestTokens tokens) {
        return "Bearer " + tokens.accessToken();
    }

    private record FinalFixture(
            Long tournamentId,
            Long editionId,
            Long teamAId,
            Long teamBId,
            Long matchId
    ) {}
}
