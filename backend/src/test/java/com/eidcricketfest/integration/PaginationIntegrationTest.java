package com.eidcricketfest.integration;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaginationIntegrationTest
        extends AuthTestSupport {

    @Test
    void playersShouldBePaginated()
            throws Exception {

        String prefix =
                "PagePlayers-"
                + marker();

        createPlayers(
                prefix,
                25,
                categoryId("BOWLER")
        );

        mockMvc.perform(
                        get("/api/v1/players")
                                .param("q", prefix)
                                .param("page", "0")
                                .param("size", "10")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()")
                        .value(10))
                .andExpect(jsonPath("$.page")
                        .value(0))
                .andExpect(jsonPath("$.size")
                        .value(10))
                .andExpect(jsonPath("$.totalElements")
                        .value(25))
                .andExpect(jsonPath("$.totalPages")
                        .value(3))
                .andExpect(jsonPath("$.first")
                        .value(true))
                .andExpect(jsonPath("$.last")
                        .value(false))
                .andExpect(jsonPath("$.hasNext")
                        .value(true));
    }

    @Test
    void playersShouldReturnSecondPage()
            throws Exception {

        String prefix =
                "SecondPage-"
                + marker();

        createPlayers(
                prefix,
                25,
                categoryId("BOWLER")
        );

        mockMvc.perform(
                        get("/api/v1/players")
                                .param("q", prefix)
                                .param("page", "1")
                                .param("size", "10")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page")
                        .value(1))
                .andExpect(jsonPath("$.content.length()")
                        .value(10))
                .andExpect(jsonPath("$.hasPrevious")
                        .value(true));
    }

    @Test
    void playerFiltersShouldCompose()
            throws Exception {

        String prefix =
                "ComposePlayers-"
                + marker();

        Short bowlerId =
                categoryId("BOWLER");

        Short batsmanId =
                categoryId("BATSMAN");

        createPlayer(
                prefix + " Rahim Ahmed",
                bowlerId
        );

        createPlayer(
                prefix + " Rahim Hasan",
                batsmanId
        );

        createPlayer(
                prefix + " Karim Ahmed",
                bowlerId
        );

        mockMvc.perform(
                        get("/api/v1/players")
                                .param("q", prefix + " Rahim")
                                .param("category", "BOWLER")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements")
                        .value(1))
                .andExpect(jsonPath("$.content[0].fullName")
                        .value(prefix + " Rahim Ahmed"));
    }

    @Test
    void excessivePageSizeShouldBeRejected()
            throws Exception {

        mockMvc.perform(
                        get("/api/v1/players")
                                .param("size", "101")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_REQUEST"));
    }

    @Test
    void negativePageShouldBeRejected()
            throws Exception {

        mockMvc.perform(
                        get("/api/v1/players")
                                .param("page", "-1")
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void unsupportedSortShouldBeRejected()
            throws Exception {

        mockMvc.perform(
                        get("/api/v1/players")
                                .param("sortBy", "passwordHash")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_REQUEST"));
    }

    @Test
    void invalidEnumParameterShouldBeRejected()
            throws Exception {

        mockMvc.perform(
                        get("/api/v1/tournament-editions/1/matches")
                                .param("status", "HELLO")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.detail")
                        .value("Invalid value for parameter: status"));
    }

    @Test
    void registrationsShouldFilterByStatusAndEdition()
            throws Exception {

        TestTokens organizer =
                organizer();

        String tag =
                marker();

        Long editionA =
                createEdition(tag + "-A");

        Long editionB =
                createEdition(tag + "-B");

        Short bowlerId =
                categoryId("BOWLER");

        createRegistration(
                editionA,
                createPlayer("Reg Approved " + tag, bowlerId),
                bowlerId,
                "APPROVED"
        );

        createRegistration(
                editionA,
                createPlayer("Reg Pending " + tag, bowlerId),
                bowlerId,
                "PENDING"
        );

        createRegistration(
                editionB,
                createPlayer("Other Edition " + tag, bowlerId),
                bowlerId,
                "APPROVED"
        );

        mockMvc.perform(
                        get(
                                "/api/v1/tournament-editions/{editionId}/registrations",
                                editionA
                        )
                                .header(
                                        "Authorization",
                                        bearer(organizer)
                                )
                                .param("status", "APPROVED")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements")
                        .value(1))
                .andExpect(jsonPath("$.content[0].tournamentEditionId")
                        .value(editionA))
                .andExpect(jsonPath("$.content[0].status")
                        .value("APPROVED"));

        mockMvc.perform(
                        get(
                                "/api/v1/tournament-editions/{editionId}/registrations",
                                editionA
                        )
                                .header(
                                        "Authorization",
                                        bearer(organizer)
                                )
                                .param("status", "PENDING")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements")
                        .value(1))
                .andExpect(jsonPath("$.content[0].tournamentEditionId")
                        .value(editionA))
                .andExpect(jsonPath("$.content[0].status")
                        .value("PENDING"));

        mockMvc.perform(
                        get(
                                "/api/v1/tournament-editions/{editionId}/registrations",
                                editionB
                        )
                                .header(
                                        "Authorization",
                                        bearer(organizer)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements")
                        .value(1))
                .andExpect(jsonPath("$.content[0].tournamentEditionId")
                        .value(editionB));
    }

    @Test
    void paymentsShouldFilterByMethodAndStatus()
            throws Exception {

        TestTokens organizer =
                organizer();

        String tag =
                marker();

        Long editionId =
                createEdition(tag);

        Short bowlerId =
                categoryId("BOWLER");

        Long registrationId =
                createRegistration(
                        editionId,
                        createPlayer("Payment Player " + tag, bowlerId),
                        bowlerId,
                        "PENDING"
                );

        createPayment(
                registrationId,
                "BKASH",
                "VERIFIED"
        );

        createPayment(
                registrationId,
                "BKASH",
                "PENDING"
        );

        createPayment(
                registrationId,
                "CASH",
                "PENDING"
        );

        mockMvc.perform(
                        get(
                                "/api/v1/tournament-editions/{editionId}/payments",
                                editionId
                        )
                                .header(
                                        "Authorization",
                                        bearer(organizer)
                                )
                                .param("method", "BKASH")
                                .param("status", "PENDING")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements")
                        .value(1))
                .andExpect(jsonPath("$.content[0].paymentMethod")
                        .value("BKASH"))
                .andExpect(jsonPath("$.content[0].status")
                        .value("PENDING"));
    }

    @Test
    void matchesShouldFilterByStageStatusAndTeam()
            throws Exception {

        String tag =
                marker();

        Long editionId =
                createEdition(tag);

        Long teamA =
                createTournamentTeam(
                        editionId,
                        "Team A " + tag,
                        "A" + tag.substring(0, 3)
                );

        Long teamB =
                createTournamentTeam(
                        editionId,
                        "Team B " + tag,
                        "B" + tag.substring(0, 3)
                );

        Long teamC =
                createTournamentTeam(
                        editionId,
                        "Team C " + tag,
                        "C" + tag.substring(0, 3)
                );

        createMatch(
                editionId,
                teamA,
                teamB,
                "LEAGUE",
                "COMPLETED",
                1
        );

        createMatch(
                editionId,
                teamA,
                teamC,
                "LEAGUE",
                "SCHEDULED",
                2
        );

        createMatch(
                editionId,
                teamB,
                teamC,
                "SEMI_FINAL",
                "COMPLETED",
                3
        );

        mockMvc.perform(
                        get(
                                "/api/v1/tournament-editions/{editionId}/matches",
                                editionId
                        )
                                .param("stage", "LEAGUE")
                                .param("status", "COMPLETED")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements")
                        .value(1))
                .andExpect(jsonPath("$.content[0].stage")
                        .value("LEAGUE"))
                .andExpect(jsonPath("$.content[0].status")
                        .value("COMPLETED"));

        mockMvc.perform(
                        get(
                                "/api/v1/tournament-editions/{editionId}/matches",
                                editionId
                        )
                                .param(
                                        "teamId",
                                        String.valueOf(teamA)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements")
                        .value(2));
    }

    @Test
    void matchesShouldExposeSetupReadiness()
            throws Exception {

        String tag =
                marker();

        Long editionId =
                createEdition(tag);

        jdbcTemplate.update(
                """
                UPDATE tournament_editions
                SET playing_xi_size = 2
                WHERE id = ?
                """,
                editionId
        );

        Long teamA =
                createTournamentTeam(
                        editionId,
                        "Ready Team A " + tag,
                        "RA" + tag.substring(0, 8)
                );

        Long teamB =
                createTournamentTeam(
                        editionId,
                        "Ready Team B " + tag,
                        "RB" + tag.substring(0, 8)
                );

        Long matchId =
                createMatch(
                        editionId,
                        teamA,
                        teamB,
                        "LEAGUE",
                        "SCHEDULED",
                        1
                );

        Long scorerUserId =
                createUser(
                        "Ready Scorer " + tag,
                        "ready-scorer-" + tag + "@test.com"
                );

        jdbcTemplate.update(
                """
                INSERT INTO match_scorers (
                    match_id,
                    user_id,
                    primary_scorer
                )
                VALUES (
                    ?,
                    ?,
                    true
                )
                """,
                matchId,
                scorerUserId
        );

        Short categoryId =
                categoryId("ALL_ROUNDER");

        Long firstRegistration =
                createRegistration(
                        editionId,
                        createPlayer("Ready A One " + tag, categoryId),
                        categoryId,
                        "APPROVED"
                );

        Long secondRegistration =
                createRegistration(
                        editionId,
                        createPlayer("Ready A Two " + tag, categoryId),
                        categoryId,
                        "APPROVED"
                );

        createPlayingXiEntry(
                matchId,
                editionId,
                teamA,
                firstRegistration,
                true,
                false
        );

        createPlayingXiEntry(
                matchId,
                editionId,
                teamA,
                secondRegistration,
                false,
                true
        );

        mockMvc.perform(
                        get(
                                "/api/v1/tournament-editions/{editionId}/matches",
                                editionId
                        )
                                .param(
                                        "teamId",
                                        String.valueOf(teamA)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id")
                        .value(matchId))
                .andExpect(jsonPath("$.content[0].scorerAssigned")
                        .value(true))
                .andExpect(jsonPath("$.content[0].teamAPlayingXiSubmitted")
                        .value(true))
                .andExpect(jsonPath("$.content[0].teamBPlayingXiSubmitted")
                        .value(false))
                .andExpect(jsonPath("$.content[0].tossCompleted")
                        .value(false));
    }

    private TestTokens organizer()
            throws Exception {

        TestTokens account =
                registerPlayer();

        addRole(
                account.email(),
                "ORGANIZER"
        );

        return login(
                account.email(),
                account.password()
        );
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

    private void createPlayers(
            String prefix,
            int count,
            Short categoryId
    ) {

        for (int i = 0; i < count; i++) {
            createPlayer(
                    "%s Player %02d".formatted(
                            prefix,
                            i
                    ),
                    categoryId
            );
        }
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
                        "Pagination Tournament " + tag
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
                    6,
                    11,
                    11,
                    200,
                    'BDT',
                    2,
                    1,
                    1,
                    0,
                    'REGISTRATION_OPEN',
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                )
                RETURNING id
                """,
                Long.class,
                tournamentId,
                "Pagination Edition " + tag
        );
    }

    private Long createRegistration(
            Long editionId,
            Long playerId,
            Short categoryId,
            String status
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
                    200,
                    ?,
                    CURRENT_TIMESTAMP,
                    CASE WHEN ? = 'APPROVED'
                        THEN CURRENT_TIMESTAMP
                        ELSE NULL
                    END,
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP,
                    0
                )
                RETURNING id
                """,
                Long.class,
                editionId,
                playerId,
                categoryId,
                status,
                status
        );
    }

    private void createPayment(
            Long registrationId,
            String method,
            String status
    ) {

        jdbcTemplate.update(
                """
                INSERT INTO registration_payments (
                    registration_id,
                    amount,
                    payment_method,
                    transaction_reference,
                    status,
                    paid_at,
                    verified_at,
                    created_at,
                    updated_at,
                    version
                )
                VALUES (
                    ?,
                    100,
                    ?,
                    ?,
                    ?,
                    CURRENT_TIMESTAMP,
                    CASE WHEN ? = 'VERIFIED'
                        THEN CURRENT_TIMESTAMP
                        ELSE NULL
                    END,
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP,
                    0
                )
                """,
                registrationId,
                method,
                "ref-" + marker(),
                status,
                status
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

    private Long createMatch(
            Long editionId,
            Long teamAId,
            Long teamBId,
            String stage,
            String status,
            int matchNumber
    ) {

        Long matchId =
                jdbcTemplate.queryForObject(
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
                    created_at,
                    updated_at,
                    version
                )
                VALUES (
                    ?,
                    ?,
                    ?,
                    ?,
                    1,
                    ?,
                    6,
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
                stage,
                matchNumber,
                status
        );

        createMatchSides(
                matchId,
                teamAId,
                teamBId
        );

        return matchId;
    }

    private void createMatchSides(
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

    private Long createUser(
            String displayName,
            String email
    ) {

        return jdbcTemplate.queryForObject(
                """
                INSERT INTO users (
                    display_name,
                    email,
                    password_hash,
                    enabled,
                    created_at,
                    updated_at
                )
                VALUES (
                    ?,
                    ?,
                    '{noop}password',
                    true,
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                )
                RETURNING id
                """,
                Long.class,
                displayName,
                email
        );
    }

    private void createPlayingXiEntry(
            Long matchId,
            Long editionId,
            Long tournamentTeamId,
            Long registrationId,
            boolean captain,
            boolean wicketkeeper
    ) {

        jdbcTemplate.update(
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
                """,
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
}
