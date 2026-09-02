package com.eidcricketfest.integration;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MyCricketIntegrationTest
        extends AuthTestSupport {

    @Test
    void playerCanReadOwnTeamMatchesAndEditionStatistics()
            throws Exception {

        TestTokens player =
                registerPlayer();

        String tag =
                marker();

        Short categoryId =
                categoryId("ALL_ROUNDER");

        Long editionId =
                createEdition(tag);

        Long venueId =
                createVenue("My Cricket Venue " + tag);

        Long playerId =
                createAccountPlayer(
                        player.email(),
                        "My Cricket Player " + tag,
                        categoryId
                );

        Long teammateId =
                createPlayer(
                        "My Cricket Teammate " + tag,
                        categoryId
                );

        Long opponentOneId =
                createPlayer(
                        "My Cricket Opponent One " + tag,
                        categoryId
                );

        Long opponentTwoId =
                createPlayer(
                        "My Cricket Opponent Two " + tag,
                        categoryId
                );

        Long playerRegistrationId =
                createRegistration(
                        editionId,
                        playerId,
                        categoryId
                );

        Long teammateRegistrationId =
                createRegistration(
                        editionId,
                        teammateId,
                        categoryId
                );

        Long opponentOneRegistrationId =
                createRegistration(
                        editionId,
                        opponentOneId,
                        categoryId
                );

        Long opponentTwoRegistrationId =
                createRegistration(
                        editionId,
                        opponentTwoId,
                        categoryId
                );

        Long myTeamId =
                createTournamentTeam(
                        editionId,
                        "My Cricket Team " + tag,
                        "MCT" + tag.substring(0, 4)
                );

        Long opponentTeamId =
                createTournamentTeam(
                        editionId,
                        "My Cricket Rival " + tag,
                        "MCR" + tag.substring(0, 4)
                );

        addRosterEntry(
                editionId,
                myTeamId,
                playerRegistrationId,
                "CAPTAIN",
                "7"
        );

        addRosterEntry(
                editionId,
                myTeamId,
                teammateRegistrationId,
                "DRAFT",
                "8"
        );

        addRosterEntry(
                editionId,
                opponentTeamId,
                opponentOneRegistrationId,
                "CAPTAIN",
                "1"
        );

        addRosterEntry(
                editionId,
                opponentTeamId,
                opponentTwoRegistrationId,
                "DRAFT",
                "2"
        );

        setCaptain(
                myTeamId,
                playerRegistrationId
        );

        Long matchId =
                createMatch(
                        editionId,
                        myTeamId,
                        opponentTeamId,
                        venueId,
                        1,
                        "COMPLETED",
                        myTeamId,
                        "My Cricket Team won by 6 runs"
                );

        Long playerXiId =
                createPlayingXiEntry(
                        matchId,
                        editionId,
                        myTeamId,
                        playerRegistrationId,
                        true,
                        false
                );

        Long teammateXiId =
                createPlayingXiEntry(
                        matchId,
                        editionId,
                        myTeamId,
                        teammateRegistrationId,
                        false,
                        true
                );

        Long opponentOneXiId =
                createPlayingXiEntry(
                        matchId,
                        editionId,
                        opponentTeamId,
                        opponentOneRegistrationId,
                        true,
                        false
                );

        Long opponentTwoXiId =
                createPlayingXiEntry(
                        matchId,
                        editionId,
                        opponentTeamId,
                        opponentTwoRegistrationId,
                        false,
                        true
                );

        Long battingInningsId =
                createInnings(
                        matchId,
                        editionId,
                        1,
                        myTeamId,
                        opponentTeamId,
                        playerXiId,
                        teammateXiId,
                        opponentOneXiId
                );

        Long battingDeliveryId =
                createDelivery(
                        battingInningsId,
                        matchId,
                        1,
                        playerXiId,
                        teammateXiId,
                        opponentOneXiId,
                        4,
                        0,
                        userId(player.email())
                );

        createWicket(
                battingDeliveryId,
                matchId,
                playerXiId,
                "CAUGHT",
                opponentTwoXiId
        );

        Long bowlingInningsId =
                createInnings(
                        matchId,
                        editionId,
                        2,
                        opponentTeamId,
                        myTeamId,
                        opponentOneXiId,
                        opponentTwoXiId,
                        playerXiId
                );

        Long bowlingDeliveryId =
                createDelivery(
                        bowlingInningsId,
                        matchId,
                        1,
                        opponentOneXiId,
                        opponentTwoXiId,
                        playerXiId,
                        0,
                        0,
                        userId(player.email())
                );

        createWicket(
                bowlingDeliveryId,
                matchId,
                opponentOneXiId,
                "BOWLED",
                null
        );

        Long fieldingDeliveryId =
                createDelivery(
                        bowlingInningsId,
                        matchId,
                        2,
                        opponentTwoXiId,
                        opponentOneXiId,
                        teammateXiId,
                        0,
                        0,
                        userId(player.email())
                );

        createWicket(
                fieldingDeliveryId,
                matchId,
                opponentTwoXiId,
                "CAUGHT",
                playerXiId
        );

        mockMvc.perform(
                        get("/api/v1/players/me/team")
                                .param(
                                        "editionId",
                                        editionId.toString()
                                )
                                .header(
                                        "Authorization",
                                        bearer(player)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tournamentTeamId")
                        .value(myTeamId))
                .andExpect(jsonPath("$.teamName")
                        .value("My Cricket Team " + tag))
                .andExpect(jsonPath("$.captain.registrationId")
                        .value(playerRegistrationId))
                .andExpect(jsonPath("$.me.registrationId")
                        .value(playerRegistrationId))
                .andExpect(jsonPath("$.me.acquisitionType")
                        .value("CAPTAIN"))
                .andExpect(jsonPath("$.me.jerseyNumber")
                        .value("7"))
                .andExpect(jsonPath("$.me.captain")
                        .value(true))
                .andExpect(jsonPath("$.squad[*].registrationId")
                        .value(containsInAnyOrder(
                                playerRegistrationId.intValue(),
                                teammateRegistrationId.intValue()
                        )))
                .andExpect(jsonPath("$.squad[?(@.registrationId == %s)].captain"
                        .formatted(playerRegistrationId))
                        .value(true));

        mockMvc.perform(
                        get("/api/v1/players/me/matches")
                                .param(
                                        "editionId",
                                        editionId.toString()
                                )
                                .header(
                                        "Authorization",
                                        bearer(player)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].matchId")
                        .value(matchId))
                .andExpect(jsonPath("$[0].myTournamentTeamId")
                        .value(myTeamId))
                .andExpect(jsonPath("$[0].opponent.tournamentTeamId")
                        .value(opponentTeamId))
                .andExpect(jsonPath("$[0].venue.name")
                        .value("My Cricket Venue " + tag))
                .andExpect(jsonPath("$[0].inPlayingXi")
                        .value(true))
                .andExpect(jsonPath("$[0].myTeamPlayingXiSubmitted")
                        .value(true))
                .andExpect(jsonPath("$[0].winnerTeamId")
                        .value(myTeamId))
                .andExpect(jsonPath("$[0].resultSummary")
                        .value("My Cricket Team won by 6 runs"));

        mockMvc.perform(
                        get("/api/v1/players/me/statistics")
                                .param(
                                        "editionId",
                                        editionId.toString()
                                )
                                .header(
                                        "Authorization",
                                        bearer(player)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.editionId")
                        .value(editionId))
                .andExpect(jsonPath("$.playerId")
                        .value(playerId))
                .andExpect(jsonPath("$.matchesPlayed")
                        .value(1))
                .andExpect(jsonPath("$.batting.runs")
                        .value(4))
                .andExpect(jsonPath("$.batting.balls")
                        .value(1))
                .andExpect(jsonPath("$.batting.dismissals")
                        .value(1))
                .andExpect(jsonPath("$.bowling.legalBalls")
                        .value(1))
                .andExpect(jsonPath("$.bowling.wickets")
                        .value(1))
                .andExpect(jsonPath("$.fielding.catches")
                        .value(1));
    }

    @Test
    void myCricketReadsOnlyUseAuthenticatedPlayerAndEdition()
            throws Exception {

        TestTokens owner =
                registerPlayer();

        TestTokens other =
                registerPlayer();

        String tag =
                marker();

        Short categoryId =
                categoryId("ALL_ROUNDER");

        Long editionId =
                createEdition(tag);

        Long otherEditionId =
                createEdition(tag + "b");

        Long ownerPlayerId =
                createAccountPlayer(
                        owner.email(),
                        "Owner My Cricket Player " + tag,
                        categoryId
                );

        createAccountPlayer(
                other.email(),
                "Other My Cricket Player " + tag,
                categoryId
        );

        Long registrationId =
                createRegistration(
                        editionId,
                        ownerPlayerId,
                        categoryId
                );

        Long teamId =
                createTournamentTeam(
                        editionId,
                        "Owner Team " + tag,
                        "OT" + tag.substring(0, 4)
                );

        addRosterEntry(
                editionId,
                teamId,
                registrationId,
                "MANUAL",
                null
        );

        mockMvc.perform(
                        get("/api/v1/players/me/team")
                                .param(
                                        "editionId",
                                        editionId.toString()
                                )
                                .header(
                                        "Authorization",
                                        bearer(other)
                                )
                )
                .andExpect(status().isNotFound());

        mockMvc.perform(
                        get("/api/v1/players/me/matches")
                                .param(
                                        "editionId",
                                        editionId.toString()
                                )
                                .header(
                                        "Authorization",
                                        bearer(other)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$")
                        .value(empty()));

        mockMvc.perform(
                        get("/api/v1/players/me/team")
                                .param(
                                        "editionId",
                                        otherEditionId.toString()
                                )
                                .header(
                                        "Authorization",
                                        bearer(owner)
                                )
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void anonymousMyCricketReadsAreRejected()
            throws Exception {

        mockMvc.perform(
                        get("/api/v1/players/me/team")
                                .param(
                                        "editionId",
                                        "1"
                                )
                )
                .andExpect(status().isUnauthorized());

        mockMvc.perform(
                        get("/api/v1/players/me/matches")
                                .param(
                                        "editionId",
                                        "1"
                                )
                )
                .andExpect(status().isUnauthorized());

        mockMvc.perform(
                        get("/api/v1/players/me/statistics")
                                .param(
                                        "editionId",
                                        "1"
                                )
                )
                .andExpect(status().isUnauthorized());
    }

    private String bearer(
            TestTokens tokens
    ) {
        return "Bearer " + tokens.accessToken();
    }

    private String marker() {
        return UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12);
    }

    private Short categoryId(
            String code
    ) {

        return jdbcTemplate.queryForObject(
                """
                SELECT id
                FROM player_categories
                WHERE code = ?
                """,
                Short.class,
                code
        );
    }

    private Long userId(
            String email
    ) {

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

    private Long createAccountPlayer(
            String email,
            String fullName,
            Short categoryId
    ) {

        return jdbcTemplate.queryForObject(
                """
                INSERT INTO players (
                    user_id,
                    full_name,
                    primary_category_id,
                    created_at,
                    updated_at
                )
                VALUES (
                    ?,
                    ?,
                    ?,
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                )
                RETURNING id
                """,
                Long.class,
                userId(email),
                fullName,
                categoryId
        );
    }

    private Long createPlayer(
            String fullName,
            Short categoryId
    ) {

        return jdbcTemplate.queryForObject(
                """
                INSERT INTO players (
                    full_name,
                    primary_category_id,
                    created_at,
                    updated_at
                )
                VALUES (
                    ?,
                    ?,
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                )
                RETURNING id
                """,
                Long.class,
                fullName,
                categoryId
        );
    }

    private Long createEdition(
            String tag
    ) {

        Long tournamentId =
                jdbcTemplate.queryForObject(
                        """
                        INSERT INTO tournaments (
                            name,
                            created_at,
                            updated_at
                        )
                        VALUES (
                            ?,
                            CURRENT_TIMESTAMP,
                            CURRENT_TIMESTAMP
                        )
                        RETURNING id
                        """,
                        Long.class,
                        "My Cricket Tournament " + tag
                );

        return jdbcTemplate.queryForObject(
                """
                INSERT INTO tournament_editions (
                    tournament_id,
                    name,
                    start_date,
                    end_date,
                    registration_start_at,
                    registration_end_at,
                    overs_per_innings,
                    squad_size,
                    playing_xi_size,
                    registration_fee,
                    registration_currency,
                    win_points,
                    tie_points,
                    no_result_points,
                    loss_points,
                    status,
                    created_at,
                    updated_at
                )
                VALUES (
                    ?,
                    ?,
                    CURRENT_DATE,
                    CURRENT_DATE + 1,
                    CURRENT_TIMESTAMP - INTERVAL '1 day',
                    CURRENT_TIMESTAMP + INTERVAL '1 day',
                    2,
                    2,
                    2,
                    0,
                    'BDT',
                    2,
                    1,
                    1,
                    0,
                    'SCHEDULED',
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                )
                RETURNING id
                """,
                Long.class,
                tournamentId,
                "My Cricket Edition " + tag
        );
    }

    private Long createRegistration(
            Long editionId,
            Long playerId,
            Short categoryId
    ) {

        return jdbcTemplate.queryForObject(
                """
                INSERT INTO player_registrations (
                    tournament_edition_id,
                    player_id,
                    category_id,
                    fee_amount,
                    status,
                    registered_at,
                    approved_at,
                    created_at,
                    updated_at,
                    version
                )
                VALUES (
                    ?,
                    ?,
                    ?,
                    0,
                    'APPROVED',
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP,
                    0
                )
                RETURNING id
                """,
                Long.class,
                editionId,
                playerId,
                categoryId
        );
    }

    private Long createVenue(
            String name
    ) {

        return jdbcTemplate.queryForObject(
                """
                INSERT INTO venues (
                    name,
                    created_at,
                    updated_at
                )
                VALUES (
                    ?,
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                )
                RETURNING id
                """,
                Long.class,
                name
        );
    }

    private Long createTournamentTeam(
            Long editionId,
            String name,
            String shortName
    ) {

        Long teamId =
                jdbcTemplate.queryForObject(
                        """
                        INSERT INTO teams (
                            name,
                            short_name,
                            created_at,
                            updated_at
                        )
                        VALUES (
                            ?,
                            ?,
                            CURRENT_TIMESTAMP,
                            CURRENT_TIMESTAMP
                        )
                        RETURNING id
                        """,
                        Long.class,
                        name,
                        shortName
                );

        return jdbcTemplate.queryForObject(
                """
                INSERT INTO tournament_teams (
                    tournament_edition_id,
                    team_id,
                    roster_status,
                    created_at,
                    updated_at
                )
                VALUES (
                    ?,
                    ?,
                    'OPEN',
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                )
                RETURNING id
                """,
                Long.class,
                editionId,
                teamId
        );
    }

    private void setCaptain(
            Long tournamentTeamId,
            Long registrationId
    ) {

        jdbcTemplate.update(
                """
                UPDATE tournament_teams
                SET captain_registration_id = ?
                WHERE id = ?
                """,
                registrationId,
                tournamentTeamId
        );
    }

    private void addRosterEntry(
            Long editionId,
            Long tournamentTeamId,
            Long registrationId,
            String acquisitionType,
            String jerseyNumber
    ) {

        jdbcTemplate.update(
                """
                INSERT INTO team_roster_entries (
                    tournament_edition_id,
                    tournament_team_id,
                    player_registration_id,
                    acquisition_type,
                    jersey_number,
                    status,
                    joined_at,
                    created_at,
                    updated_at
                )
                VALUES (
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    'ACTIVE',
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                )
                """,
                editionId,
                tournamentTeamId,
                registrationId,
                acquisitionType,
                jerseyNumber
        );
    }

    private Long createMatch(
            Long editionId,
            Long teamAId,
            Long teamBId,
            Long venueId,
            int matchNumber,
            String status,
            Long winnerTeamId,
            String resultSummary
    ) {

        Long matchId =
                jdbcTemplate.queryForObject(
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
                    winner_team_id,
                    result_type,
                    result_summary,
                    created_at,
                    updated_at,
                    version
                )
                VALUES (
                    ?,
                    ?,
                    ?,
                    ?,
                    'LEAGUE',
                    1,
                    ?,
                    CURRENT_TIMESTAMP,
                    2,
                    ?,
                    ?,
                    'RUNS',
                    ?,
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP,
                    0
                )
                RETURNING id
                """,
                Long.class,
                editionId,
                teamAId,
                teamBId,
                venueId,
                matchNumber,
                status,
                winnerTeamId,
                resultSummary
        );

        MatchSides matchSides =
                createMatchSides(
                        matchId,
                        teamAId,
                        teamBId
                );

        if (winnerTeamId != null) {
            jdbcTemplate.update(
                    """
                    UPDATE matches
                    SET winner_side_id = ?
                    WHERE id = ?
                    """,
                    winnerTeamId.equals(teamAId)
                            ? matchSides.teamASideId()
                            : matchSides.teamBSideId(),
                    matchId
            );
        }

        return matchId;
    }

    private MatchSides createMatchSides(
            Long matchId,
            Long teamAId,
            Long teamBId
    ) {

        Long teamASideId =
                createMatchSide(
                        matchId,
                        teamAId,
                        "A"
                );

        Long teamBSideId =
                createMatchSide(
                        matchId,
                        teamBId,
                        "B"
                );

        jdbcTemplate.update(
                """
                UPDATE matches
                SET
                    team_a_side_id = ?,
                    team_b_side_id = ?
                WHERE id = ?
                """,
                teamASideId,
                teamBSideId,
                matchId
        );

        return new MatchSides(
                teamASideId,
                teamBSideId
        );
    }

    private Long createMatchSide(
            Long matchId,
            Long tournamentTeamId,
            String sideKey
    ) {

        return jdbcTemplate.queryForObject(
                """
                INSERT INTO match_sides (
                    match_id,
                    side_key,
                    display_name,
                    tournament_team_id
                )
                SELECT
                    ?,
                    ?,
                    t.name,
                    tt.id
                FROM tournament_teams tt
                JOIN teams t ON t.id = tt.team_id
                WHERE tt.id = ?
                RETURNING id
                """,
                Long.class,
                matchId,
                sideKey,
                tournamentTeamId
        );
    }

    private Long createPlayingXiEntry(
            Long matchId,
            Long editionId,
            Long tournamentTeamId,
            Long registrationId,
            boolean captain,
            boolean wicketkeeper
    ) {

        return jdbcTemplate.queryForObject(
                """
                INSERT INTO playing_xi_entries (
                    match_id,
                    tournament_edition_id,
                    tournament_team_id,
                    match_side_id,
                    player_registration_id,
                    player_id,
                    is_captain,
                    is_wicketkeeper
                )
                SELECT
                    ?,
                    ?,
                    ?,
                    ms.id,
                    ?,
                    pr.player_id,
                    ?,
                    ?
                FROM match_sides ms
                JOIN player_registrations pr ON pr.id = ?
                WHERE ms.match_id = ?
                  AND ms.tournament_team_id = ?
                RETURNING id
                """,
                Long.class,
                matchId,
                editionId,
                tournamentTeamId,
                registrationId,
                captain,
                wicketkeeper,
                registrationId,
                matchId,
                tournamentTeamId
        );
    }

    private Long createInnings(
            Long matchId,
            Long editionId,
            int inningsNumber,
            Long battingTeamId,
            Long bowlingTeamId,
            Long strikerXiId,
            Long nonStrikerXiId,
            Long bowlerXiId
    ) {

        return jdbcTemplate.queryForObject(
                """
                INSERT INTO innings (
                    match_id,
                    tournament_edition_id,
                    innings_number,
                    batting_team_id,
                    bowling_team_id,
                    batting_match_side_id,
                    bowling_match_side_id,
                    current_striker_xi_id,
                    current_non_striker_xi_id,
                    current_bowler_xi_id,
                    created_at,
                    updated_at,
                    version
                )
                VALUES (
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    (SELECT id FROM match_sides WHERE match_id = ? AND tournament_team_id = ?),
                    (SELECT id FROM match_sides WHERE match_id = ? AND tournament_team_id = ?),
                    ?,
                    ?,
                    ?,
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP,
                    0
                )
                RETURNING id
                """,
                Long.class,
                matchId,
                editionId,
                inningsNumber,
                battingTeamId,
                bowlingTeamId,
                matchId,
                battingTeamId,
                matchId,
                bowlingTeamId,
                strikerXiId,
                nonStrikerXiId,
                bowlerXiId
        );
    }

    private Long createDelivery(
            Long inningsId,
            Long matchId,
            int sequenceNo,
            Long strikerXiId,
            Long nonStrikerXiId,
            Long bowlerXiId,
            int runsOffBat,
            int wideRuns,
            Long createdByUserId
    ) {

        return jdbcTemplate.queryForObject(
                """
                INSERT INTO deliveries (
                    innings_id,
                    match_id,
                    sequence_no,
                    striker_xi_id,
                    non_striker_xi_id,
                    bowler_xi_id,
                    runs_off_bat,
                    wide_runs,
                    no_ball_runs,
                    bye_runs,
                    leg_bye_runs,
                    penalty_runs,
                    swap_ends,
                    created_by_user_id,
                    created_at,
                    updated_at,
                    version
                )
                VALUES (
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    0,
                    0,
                    0,
                    0,
                    false,
                    ?,
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP,
                    0
                )
                RETURNING id
                """,
                Long.class,
                inningsId,
                matchId,
                sequenceNo,
                strikerXiId,
                nonStrikerXiId,
                bowlerXiId,
                runsOffBat,
                wideRuns,
                createdByUserId
        );
    }

    private void createWicket(
            Long deliveryId,
            Long matchId,
            Long dismissedXiId,
            String dismissalType,
            Long fielderXiId
    ) {

        jdbcTemplate.update(
                """
                INSERT INTO wickets (
                    delivery_id,
                    match_id,
                    dismissed_playing_xi_id,
                    dismissal_type,
                    fielder_playing_xi_id,
                    credited_to_bowler,
                    created_at
                )
                VALUES (
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    CURRENT_TIMESTAMP
                )
                """,
                deliveryId,
                matchId,
                dismissedXiId,
                dismissalType,
                fielderXiId,
                !"RUN_OUT".equals(dismissalType)
        );
    }


    private record MatchSides(
            Long teamASideId,
            Long teamBSideId
    ) {
    }
}
