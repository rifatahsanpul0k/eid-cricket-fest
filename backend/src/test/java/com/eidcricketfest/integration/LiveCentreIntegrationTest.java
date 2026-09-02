package com.eidcricketfest.integration;

import com.eidcricketfest.match.dto.MatchOperationReasonRequest;
import com.eidcricketfest.match.service.MatchOperationsService;
import com.eidcricketfest.scoring.dto.InningsResponse;
import com.eidcricketfest.scoring.dto.RecordDeliveryRequest;
import com.eidcricketfest.scoring.dto.StartInningsRequest;
import com.eidcricketfest.scoring.service.ScoringService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LiveCentreIntegrationTest
        extends AbstractIntegrationTest {

    @Autowired
    private ScoringService scoringService;

    @Autowired
    private MatchOperationsService matchOperationsService;

    @Test
    void publicLiveCentreIncludesTournamentAndFriendlyMatchStates()
            throws Exception {

        Long tournamentToss =
                tournamentMatch("TOSS_COMPLETED");
        Long friendlyToss =
                friendlyMatch("TOSS_COMPLETED");
        Long tournamentLive =
                tournamentMatch("LIVE");
        Long friendlyLive =
                friendlyMatch("LIVE");
        Long inningsBreak =
                tournamentMatch("INNINGS_BREAK");
        Long suspended =
                friendlyMatch("SUSPENDED");
        Long completed =
                completedTournamentMatch();

        Long planned =
                tournamentMatch("PLANNED");
        Long ready =
                friendlyMatch("READY");
        Long postponed =
                tournamentMatch("POSTPONED");
        Long cancelled =
                friendlyMatch("CANCELLED");

        mockMvc.perform(get("/api/v1/matches/live-centre"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.matchId == " + tournamentToss + ")]")
                        .exists())
                .andExpect(jsonPath("$[?(@.matchId == " + friendlyToss + ")]")
                        .exists())
                .andExpect(jsonPath("$[?(@.matchId == " + tournamentLive + ")]")
                        .exists())
                .andExpect(jsonPath("$[?(@.matchId == " + friendlyLive + ")]")
                        .exists())
                .andExpect(jsonPath("$[?(@.matchId == " + inningsBreak + ")]")
                        .exists())
                .andExpect(jsonPath("$[?(@.matchId == " + suspended + ")]")
                        .exists())
                .andExpect(jsonPath("$[?(@.matchId == " + completed + ")]")
                        .exists())
                .andExpect(jsonPath("$[?(@.matchId == " + planned + ")]")
                        .doesNotExist())
                .andExpect(jsonPath("$[?(@.matchId == " + ready + ")]")
                        .doesNotExist())
                .andExpect(jsonPath("$[?(@.matchId == " + postponed + ")]")
                        .doesNotExist())
                .andExpect(jsonPath("$[?(@.matchId == " + cancelled + ")]")
                        .doesNotExist());
    }

    @Test
    void friendlyLifecycleAppearsThroughLiveCentre()
            throws Exception {

        FriendlyScenario scenario =
                friendlyScenario();

        mockMvc.perform(get("/api/v1/matches/live-centre"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.matchId == " + scenario.matchId()
                        + " && @.matchType == 'FRIENDLY'"
                        + " && @.status == 'TOSS_COMPLETED')]")
                        .exists());

        InningsResponse first =
                scoringService.startInnings(
                        scenario.matchId(),
                        scenario.actorUserId(),
                        true,
                        new StartInningsRequest(
                                scenario.a1XiId(),
                                scenario.a2XiId(),
                                scenario.b1XiId()
                        )
                );

        mockMvc.perform(get("/api/v1/matches/live-centre"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.matchId == " + scenario.matchId()
                        + " && @.status == 'LIVE')]")
                        .exists());

        matchOperationsService.suspend(
                scenario.matchId(),
                scenario.actorUserId(),
                new MatchOperationReasonRequest("Bad light")
        );

        mockMvc.perform(get("/api/v1/matches/live-centre"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.matchId == " + scenario.matchId()
                        + " && @.status == 'SUSPENDED')]")
                        .exists());

        matchOperationsService.resume(
                scenario.matchId(),
                scenario.actorUserId(),
                new MatchOperationReasonRequest("Light improved")
        );

        for (int i = 0; i < 6; i++) {
            scoringService.recordDelivery(
                    first.id(),
                    scenario.actorUserId(),
                    true,
                    ball(1)
            );
        }

        mockMvc.perform(get("/api/v1/matches/live-centre"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.matchId == " + scenario.matchId()
                        + " && @.status == 'INNINGS_BREAK'"
                        + " && @.innings.target == 7)]")
                        .exists());

        InningsResponse second =
                scoringService.startInnings(
                        scenario.matchId(),
                        scenario.actorUserId(),
                        true,
                        new StartInningsRequest(
                                scenario.b1XiId(),
                                scenario.b2XiId(),
                                scenario.a1XiId()
                        )
                );

        scoringService.recordDelivery(
                second.id(),
                scenario.actorUserId(),
                true,
                ball(7)
        );

        mockMvc.perform(get("/api/v1/matches/live-centre"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.matchId == " + scenario.matchId()
                        + " && @.status == 'COMPLETED'"
                        + " && @.resultStatus == 'OFFICIAL'"
                        + " && @.winner.name == 'Warriors XI')]")
                        .exists());
    }

    private Long tournamentMatch(String status) {
        String suffix =
                UUID.randomUUID().toString();

        Long tournamentId =
                insert(
                        """
                        INSERT INTO tournaments (name)
                        VALUES (?)
                        """,
                        "Live Centre Cup " + suffix
                );

        Long editionId =
                insert(
                        """
                        INSERT INTO tournament_editions (
                            tournament_id,
                            name,
                            overs_per_innings,
                            playing_xi_size,
                            status
                        )
                        VALUES (?, ?, 1, 2, 'ONGOING')
                        """,
                        tournamentId,
                        "Edition " + suffix
                );

        Long teamA =
                tournamentTeam(editionId, "Alpha " + suffix);

        Long teamB =
                tournamentTeam(editionId, "Bravo " + suffix);

        Long matchId =
                insert(
                        """
                        INSERT INTO matches (
                            match_type,
                            tournament_edition_id,
                            team_a_id,
                            team_b_id,
                            stage,
                            round_number,
                            match_number,
                            overs_per_innings,
                            status,
                            scheduled_at
                        )
                        VALUES (
                            'TOURNAMENT',
                            ?, ?, ?,
                            'LEAGUE',
                            1,
                            1,
                            1,
                            ?,
                            CURRENT_TIMESTAMP
                        )
                        """,
                        editionId,
                        teamA,
                        teamB,
                        status
                );

        Long sideA =
                matchSide(matchId, "A", "Alpha XI", teamA);

        Long sideB =
                matchSide(matchId, "B", "Bravo XI", teamB);

        attachSides(matchId, sideA, sideB);

        if ("TOSS_COMPLETED".equals(status)
                || "LIVE".equals(status)
                || "INNINGS_BREAK".equals(status)
                || "SUSPENDED".equals(status)) {
            toss(matchId, sideA);
        }

        return matchId;
    }

    private Long friendlyMatch(String status) {
        String suffix =
                UUID.randomUUID().toString();

        Long venueId =
                venue("Friendly Venue " + suffix);

        Long matchId =
                insert(
                        """
                        INSERT INTO matches (
                            match_type,
                            venue_id,
                            overs_per_innings,
                            status,
                            scheduled_at
                        )
                        VALUES (
                            'FRIENDLY',
                            ?,
                            1,
                            ?,
                            CURRENT_TIMESTAMP
                        )
                        """,
                        venueId,
                        status
                );

        Long sideA =
                matchSide(matchId, "A", "Thunder XI", null);

        Long sideB =
                matchSide(matchId, "B", "Warriors XI", null);

        attachSides(matchId, sideA, sideB);

        if ("TOSS_COMPLETED".equals(status)
                || "LIVE".equals(status)
                || "INNINGS_BREAK".equals(status)
                || "SUSPENDED".equals(status)) {
            toss(matchId, sideA);
        }

        return matchId;
    }

    private Long completedTournamentMatch() {
        Long matchId =
                tournamentMatch("COMPLETED");

        jdbcTemplate.update(
                """
                UPDATE matches
                SET actual_ended_at = CURRENT_TIMESTAMP,
                    result_status = 'OFFICIAL',
                    result_type = 'RUNS',
                    result_summary = 'Alpha XI won by 4 runs',
                    winner_side_id = team_a_side_id
                WHERE id = ?
                """,
                matchId
        );

        return matchId;
    }

    private FriendlyScenario friendlyScenario() {
        String suffix =
                UUID.randomUUID().toString();

        Long actor =
                insert(
                        """
                        INSERT INTO users (
                            display_name,
                            email,
                            password_hash,
                            enabled
                        )
                        VALUES (?, ?, '{noop}password', true)
                        """,
                        "Live Centre Organizer " + suffix,
                        "live-centre-" + suffix + "@test.com"
                );

        Long matchId =
                friendlyMatch("TOSS_COMPLETED");

        Long sideA =
                jdbcTemplate.queryForObject(
                        """
                        SELECT team_a_side_id
                        FROM matches
                        WHERE id = ?
                        """,
                        Long.class,
                        matchId
                );

        Long sideB =
                jdbcTemplate.queryForObject(
                        """
                        SELECT team_b_side_id
                        FROM matches
                        WHERE id = ?
                        """,
                        Long.class,
                        matchId
                );

        Long a1 =
                playingXi(matchId, sideA, "Pulok " + suffix);
        Long a2 =
                playingXi(matchId, sideA, "Nitol " + suffix);
        Long b1 =
                playingXi(matchId, sideB, "Limon " + suffix);
        Long b2 =
                playingXi(matchId, sideB, "Hridoy " + suffix);

        return new FriendlyScenario(
                actor,
                matchId,
                a1,
                a2,
                b1,
                b2
        );
    }

    private Long tournamentTeam(
            Long editionId,
            String name
    ) {
        Long team =
                insert(
                        """
                        INSERT INTO teams (name, short_name)
                        VALUES (?, ?)
                        """,
                        name,
                        name.substring(0, Math.min(10, name.length()))
                );

        return insert(
                """
                INSERT INTO tournament_teams (
                    tournament_edition_id,
                    team_id
                )
                VALUES (?, ?)
                """,
                editionId,
                team
        );
    }

    private Long matchSide(
            Long matchId,
            String sideKey,
            String displayName,
            Long tournamentTeamId
    ) {
        return insert(
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

    private void attachSides(
            Long matchId,
            Long sideA,
            Long sideB
    ) {
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
    }

    private Long playingXi(
            Long matchId,
            Long matchSideId,
            String playerName
    ) {
        Long playerId =
                insert(
                        """
                        INSERT INTO players (full_name)
                        VALUES (?)
                        """,
                        playerName
                );

        return insert(
                """
                INSERT INTO playing_xi_entries (
                    match_id,
                    match_side_id,
                    player_id,
                    is_captain,
                    is_wicketkeeper
                )
                VALUES (?, ?, ?, false, false)
                """,
                matchId,
                matchSideId,
                playerId
        );
    }

    private void toss(
            Long matchId,
            Long winnerSideId
    ) {
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
    }

    private Long venue(String name) {
        return insert(
                """
                INSERT INTO venues (name)
                VALUES (?)
                """,
                name
        );
    }

    private RecordDeliveryRequest ball(int runs) {
        return new RecordDeliveryRequest(
                UUID.randomUUID(),
                runs,
                0,
                0,
                0,
                0,
                0,
                null,
                null
        );
    }

    private Long insert(String sql, Object... args) {
        return jdbcTemplate.queryForObject(
                sql + " RETURNING id",
                Long.class,
                args
        );
    }

    private record FriendlyScenario(
            Long actorUserId,
            Long matchId,
            Long a1XiId,
            Long a2XiId,
            Long b1XiId,
            Long b2XiId
    ) {}
}
