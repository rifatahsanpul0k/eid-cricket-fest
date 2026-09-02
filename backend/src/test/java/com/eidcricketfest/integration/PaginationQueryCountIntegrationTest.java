package com.eidcricketfest.integration;

import com.eidcricketfest.match.entity.MatchStage;
import com.eidcricketfest.match.entity.MatchStatus;
import com.eidcricketfest.match.service.FixtureService;
import com.eidcricketfest.player.service.PlayerService;
import com.eidcricketfest.registration.entity.PaymentMethod;
import com.eidcricketfest.registration.entity.PaymentStatus;
import com.eidcricketfest.registration.entity.RegistrationStatus;
import com.eidcricketfest.registration.service.PaymentService;
import com.eidcricketfest.registration.service.RegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaginationQueryCountIntegrationTest
        extends QueryCountSupport {

    @Autowired
    private PlayerService playerService;

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private FixtureService fixtureService;

    @Test
    void playersShouldNotScaleQueriesWithPageSize() {

        String prefix =
                "QueryPlayers-"
                + marker();

        createPlayers(
                prefix,
                25,
                categoryId("BOWLER")
        );

        long smallPageQueries =
                measureStatements(() ->
                        playerService.search(
                                prefix,
                                null,
                                0,
                                5,
                                "name",
                                "asc"
                        )
                );

        long largePageQueries =
                measureStatements(() ->
                        playerService.search(
                                prefix,
                                null,
                                0,
                                20,
                                "name",
                                "asc"
                        )
                );

        report(
                "Players",
                smallPageQueries,
                largePageQueries
        );

        assertStable(
                smallPageQueries,
                largePageQueries,
                5
        );
    }

    @Test
    void registrationsShouldNotScaleQueriesWithPageSize() {

        String tag =
                marker();

        Long editionId =
                createEdition(tag);

        Short categoryId =
                categoryId("BOWLER");

        for (int i = 0; i < 25; i++) {
            createRegistration(
                    editionId,
                    createPlayer(
                            "Query Registration "
                            + tag
                            + " "
                            + i,
                            categoryId
                    ),
                    categoryId,
                    "APPROVED"
            );
        }

        long smallPageQueries =
                measureStatements(() ->
                        registrationService.searchRegistrations(
                                editionId,
                                RegistrationStatus.APPROVED,
                                null,
                                null,
                                0,
                                5,
                                "registeredAt",
                                "desc"
                        )
                );

        long largePageQueries =
                measureStatements(() ->
                        registrationService.searchRegistrations(
                                editionId,
                                RegistrationStatus.APPROVED,
                                null,
                                null,
                                0,
                                20,
                                "registeredAt",
                                "desc"
                        )
                );

        report(
                "Registrations",
                smallPageQueries,
                largePageQueries
        );

        assertStable(
                smallPageQueries,
                largePageQueries,
                5
        );
    }

    @Test
    void paymentsShouldNotScaleQueriesWithPageSize() {

        String tag =
                marker();

        Long editionId =
                createEdition(tag);

        Short categoryId =
                categoryId("BOWLER");

        for (int i = 0; i < 25; i++) {
            Long registrationId =
                    createRegistration(
                            editionId,
                            createPlayer(
                                    "Query Payment "
                                    + tag
                                    + " "
                                    + i,
                                    categoryId
                            ),
                            categoryId,
                            "PENDING"
                    );

            createPayment(
                    registrationId,
                    "BKASH",
                    "PENDING"
            );
        }

        long smallPageQueries =
                measureStatements(() ->
                        paymentService.searchPayments(
                                editionId,
                                PaymentStatus.PENDING,
                                PaymentMethod.BKASH,
                                null,
                                0,
                                5,
                                "createdAt",
                                "desc"
                        )
                );

        long largePageQueries =
                measureStatements(() ->
                        paymentService.searchPayments(
                                editionId,
                                PaymentStatus.PENDING,
                                PaymentMethod.BKASH,
                                null,
                                0,
                                20,
                                "createdAt",
                                "desc"
                        )
                );

        report(
                "Payments",
                smallPageQueries,
                largePageQueries
        );

        assertStable(
                smallPageQueries,
                largePageQueries,
                5
        );
    }

    @Test
    void matchesShouldNotScaleQueriesWithPageSize() {

        String tag =
                marker();

        Long editionId =
                createEdition(tag);

        for (int i = 0; i < 25; i++) {
            Long teamA =
                    createTournamentTeam(
                            editionId,
                            "Query Team A " + tag + " " + i,
                            shortName("QA", tag, i)
                    );

            Long teamB =
                    createTournamentTeam(
                            editionId,
                            "Query Team B " + tag + " " + i,
                            shortName("QB", tag, i)
                    );

            createMatch(
                    editionId,
                    teamA,
                    teamB,
                    "LEAGUE",
                    "SCHEDULED",
                    i + 1
            );
        }

        long smallPageQueries =
                measureStatements(() ->
                        fixtureService.searchMatches(
                                editionId,
                                MatchStatus.SCHEDULED,
                                MatchStage.LEAGUE,
                                null,
                                0,
                                5,
                                "matchNumber",
                                "asc"
                        )
                );

        long largePageQueries =
                measureStatements(() ->
                        fixtureService.searchMatches(
                                editionId,
                                MatchStatus.SCHEDULED,
                                MatchStage.LEAGUE,
                                null,
                                0,
                                20,
                                "matchNumber",
                                "asc"
                        )
                );

        report(
                "Matches",
                smallPageQueries,
                largePageQueries
        );

        assertStable(
                smallPageQueries,
                largePageQueries,
                5
        );
    }

    private void assertStable(
            long smallPageQueries,
            long largePageQueries,
            long upperBound
    ) {

        assertThat(largePageQueries)
                .isLessThanOrEqualTo(
                        smallPageQueries + 1
                );

        assertThat(largePageQueries)
                .isLessThanOrEqualTo(upperBound);
    }

    private void report(
            String label,
            long smallPageQueries,
            long largePageQueries
    ) {

        System.out.printf(
                "%s query count: size=5 %d, size=20 %d%n",
                label,
                smallPageQueries,
                largePageQueries
        );
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
                        "Query Count Tournament " + tag
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
                "Query Count Edition " + tag
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
                "query-ref-" + marker(),
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

    private String shortName(
            String prefix,
            String tag,
            int index
    ) {

        return prefix
               + tag.substring(0, 3)
               + index;
    }

    private void createMatch(
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
}
