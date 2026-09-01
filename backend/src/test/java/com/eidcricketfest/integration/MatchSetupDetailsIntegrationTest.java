package com.eidcricketfest.integration;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MatchSetupDetailsIntegrationTest
        extends AuthTestSupport {

    @Test
    void organizerCanReadSavedMatchSetupDetails()
            throws Exception {

        TestTokens organizer =
                organizer();

        String tag =
                marker();

        Long editionId =
                createEdition(tag);

        Long teamA =
                createTournamentTeam(
                        editionId,
                        "Setup Team A " + tag,
                        "SA" + tag.substring(0, 8)
                );

        Long teamB =
                createTournamentTeam(
                        editionId,
                        "Setup Team B " + tag,
                        "SB" + tag.substring(0, 8)
                );

        Long matchId =
                createMatch(
                        editionId,
                        teamA,
                        teamB
                );

        Long scorerUserId =
                createUser(
                        "Setup Scorer " + tag,
                        "setup-scorer-" + tag + "@test.com"
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

        Long aCaptain =
                createRegistration(
                        editionId,
                        createPlayer("Setup A Captain " + tag, categoryId),
                        categoryId
                );

        Long aKeeper =
                createRegistration(
                        editionId,
                        createPlayer("Setup A Keeper " + tag, categoryId),
                        categoryId
                );

        Long bCaptain =
                createRegistration(
                        editionId,
                        createPlayer("Setup B Captain " + tag, categoryId),
                        categoryId
                );

        Long bKeeper =
                createRegistration(
                        editionId,
                        createPlayer("Setup B Keeper " + tag, categoryId),
                        categoryId
                );

        createPlayingXiEntry(
                matchId,
                editionId,
                teamA,
                aCaptain,
                true,
                false
        );

        createPlayingXiEntry(
                matchId,
                editionId,
                teamA,
                aKeeper,
                false,
                true
        );

        createPlayingXiEntry(
                matchId,
                editionId,
                teamB,
                bCaptain,
                true,
                false
        );

        createPlayingXiEntry(
                matchId,
                editionId,
                teamB,
                bKeeper,
                false,
                true
        );

        mockMvc.perform(
                        get(
                                "/api/v1/matches/{matchId}/setup",
                                matchId
                        )
                                .header(
                                        "Authorization",
                                        bearer(organizer)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scorers[0].userId")
                        .value(scorerUserId))
                .andExpect(jsonPath("$.scorers[0].displayName")
                        .value("Setup Scorer " + tag))
                .andExpect(jsonPath("$.scorers[0].primary")
                        .value(true))
                .andExpect(jsonPath("$.teamAPlayingXi.tournamentTeamId")
                        .value(teamA))
                .andExpect(jsonPath("$.teamAPlayingXi.registrationIds[*]")
                        .value(containsInAnyOrder(
                                aCaptain.intValue(),
                                aKeeper.intValue()
                        )))
                .andExpect(jsonPath("$.teamAPlayingXi.wicketkeeperRegistrationId")
                        .value(aKeeper))
                .andExpect(jsonPath("$.teamBPlayingXi.tournamentTeamId")
                        .value(teamB))
                .andExpect(jsonPath("$.teamBPlayingXi.registrationIds[*]")
                        .value(containsInAnyOrder(
                                bCaptain.intValue(),
                                bKeeper.intValue()
                        )))
                .andExpect(jsonPath("$.teamBPlayingXi.wicketkeeperRegistrationId")
                        .value(bKeeper));
    }

    @Test
    void playerCannotReadMatchSetupDetails()
            throws Exception {

        TestTokens player =
                registerPlayer();

        mockMvc.perform(
                        get(
                                "/api/v1/matches/{matchId}/setup",
                                1L
                        )
                                .header(
                                        "Authorization",
                                        bearer(player)
                                )
                )
                .andExpect(status().isForbidden());
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
                        "Setup Tournament " + tag
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
                    2,
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
                "Setup Edition " + tag
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
                    200,
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
            Long teamBId
    ) {

        return jdbcTemplate.queryForObject(
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
                    'LEAGUE',
                    1,
                    1,
                    6,
                    'SCHEDULED',
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP,
                    0
                )
                RETURNING id
                """,
                Long.class,
                editionId,
                teamAId,
                teamBId
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
                    player_registration_id,
                    is_captain,
                    is_wicketkeeper
                )
                VALUES (
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?
                )
                """,
                matchId,
                editionId,
                tournamentTeamId,
                registrationId,
                captain,
                wicketkeeper
        );
    }
}
