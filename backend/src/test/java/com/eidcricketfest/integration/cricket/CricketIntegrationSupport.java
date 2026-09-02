package com.eidcricketfest.integration.cricket;

import com.eidcricketfest.integration.AbstractIntegrationTest;
import com.eidcricketfest.match.repository.CricketMatchRepository;
import com.eidcricketfest.scoring.dto.*;
import com.eidcricketfest.scoring.repository.*;
import com.eidcricketfest.scoring.service.*;
import com.eidcricketfest.standings.service.StandingsService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

public abstract class CricketIntegrationSupport
        extends AbstractIntegrationTest {

    @Autowired
    protected ScoringService scoringService;

    @Autowired
    protected ScorerMatchQueryService scorerMatchQueryService;

    @Autowired
    protected ScorecardService scorecardService;

    @Autowired
    protected StandingsService standingsService;

    @Autowired
    protected InningsRepository inningsRepository;

    @Autowired
    protected DeliveryRepository deliveryRepository;

    @Autowired
    protected WicketRepository wicketRepository;

    @Autowired
    protected CricketMatchRepository matchRepository;


    protected Scenario createScenario(
            int overs
    ) {

        String tag =
                UUID.randomUUID()
                        .toString()
                        .substring(0, 8);

        Long actorId =
                jdbcTemplate.queryForObject(
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
                        "Test Organizer " + tag,
                        "organizer-" + tag + "@test.com"
                );

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
                        "Test Tournament " + tag
                );

        Long editionId =
                jdbcTemplate.queryForObject(
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

                            ?,
                            3,
                            3,

                            0,
                            'BDT',

                            2,
                            1,
                            1,
                            0,

                            'ONGOING',

                            CURRENT_TIMESTAMP,
                            CURRENT_TIMESTAMP
                        )
                        RETURNING id
                        """,
                        Long.class,
                        tournamentId,
                        "Test Edition " + tag,
                        overs
                );

        Long permanentTeamAId =
                createPermanentTeam(
                        "Tigers-" + tag,
                        "TIG"
                );

        Long permanentTeamBId =
                createPermanentTeam(
                        "Warriors-" + tag,
                        "WAR"
                );

        Long teamAId =
                createTournamentTeam(
                        editionId,
                        permanentTeamAId
                );

        Long teamBId =
                createTournamentTeam(
                        editionId,
                        permanentTeamBId
                );

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

                            'LEAGUE',
                            1,
                            1,

                            ?,
                            'TOSS_COMPLETED',

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
                        overs
                );

        MatchSides matchSides =
                createMatchSides(
                        matchId,
                        teamAId,
                        teamBId
                );

        Short categoryId =
                jdbcTemplate.queryForObject(
                        """
                        SELECT id
                        FROM player_categories
                        WHERE code = 'ALL_ROUNDER'
                        """,
                        Short.class
                );

        XiPlayer a1 =
                createXiPlayer(
                        matchId,
                        editionId,
                        teamAId,
                        categoryId,
                        "A1-" + tag
                );

        XiPlayer a2 =
                createXiPlayer(
                        matchId,
                        editionId,
                        teamAId,
                        categoryId,
                        "A2-" + tag
                );

        XiPlayer a3 =
                createXiPlayer(
                        matchId,
                        editionId,
                        teamAId,
                        categoryId,
                        "A3-" + tag
                );

        XiPlayer b1 =
                createXiPlayer(
                        matchId,
                        editionId,
                        teamBId,
                        categoryId,
                        "B1-" + tag
                );

        XiPlayer b2 =
                createXiPlayer(
                        matchId,
                        editionId,
                        teamBId,
                        categoryId,
                        "B2-" + tag
                );

        XiPlayer b3 =
                createXiPlayer(
                        matchId,
                        editionId,
                        teamBId,
                        categoryId,
                        "B3-" + tag
                );

        jdbcTemplate.update(
                """
                INSERT INTO match_tosses (
                    match_id,
                    tournament_edition_id,
                    winner_team_id,
                    winner_match_side_id,
                    decision,
                    recorded_by_user_id,
                    recorded_at
                )
                VALUES (
                    ?, ?, ?, ?, 'BAT', ?, CURRENT_TIMESTAMP
                )
                """,
                matchId,
                editionId,
                teamAId,
                matchSides.teamASideId(),
                actorId
        );

        return new Scenario(
                actorId,
                tournamentId,
                editionId,
                matchId,

                teamAId,
                teamBId,

                a1,
                a2,
                a3,

                b1,
                b2,
                b3
        );
    }


    private Long createPermanentTeam(
            String name,
            String shortName
    ) {

        return jdbcTemplate.queryForObject(
                """
                INSERT INTO teams (
                    name,
                    short_name,
                    created_at,
                    updated_at
                )
                VALUES (
                    ?, ?,
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                )
                RETURNING id
                """,
                Long.class,
                name,
                shortName
        );
    }


    private Long createTournamentTeam(
            Long editionId,
            Long teamId
    ) {

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


    private XiPlayer createXiPlayer(
            Long matchId,
            Long editionId,
            Long tournamentTeamId,
            Short categoryId,
            String name
    ) {

        Long playerId =
                jdbcTemplate.queryForObject(
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
                        name,
                        categoryId
                );

        Long registrationId =
                jdbcTemplate.queryForObject(
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
                            updated_at
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
                            CURRENT_TIMESTAMP
                        )
                        RETURNING id
                        """,
                        Long.class,
                        editionId,
                        playerId,
                        categoryId
                );

        Long xiId =
                jdbcTemplate.queryForObject(
                        """
                        INSERT INTO playing_xi_entries (
                            match_id,
                            tournament_edition_id,
                            tournament_team_id,
                            match_side_id,
                            player_registration_id,
                            player_id,

                            is_captain,
                            is_wicketkeeper,

                            created_at
                        )
                        SELECT
                            ?,
                            ?,
                            ?,
                            ms.id,
                            ?,
                            pr.player_id,
                            false,
                            false,
                            CURRENT_TIMESTAMP
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
                        registrationId,
                        matchId,
                        tournamentTeamId
                );

        return new XiPlayer(
                playerId,
                registrationId,
                xiId
        );
    }


    protected InningsResponse startFirstInnings(
            Scenario scenario
    ) {

        return scoringService.startInnings(
                scenario.matchId(),
                scenario.actorUserId(),
                true,

                new StartInningsRequest(
                        scenario.a1().xiId(),
                        scenario.a2().xiId(),
                        scenario.b1().xiId()
                )
        );
    }


    protected RecordDeliveryRequest ball(
            int bat,
            int wide,
            int noBall,
            int bye,
            int legBye
    ) {

        return new RecordDeliveryRequest(
                UUID.randomUUID(),

                bat,
                wide,
                noBall,
                bye,
                legBye,
                0,

                null,
                null
        );
    }


    protected RecordDeliveryRequest wicketBall(
            int bat,
            int wide,
            int noBall,
            int bye,
            int legBye,
            WicketRequest wicket
    ) {

        return new RecordDeliveryRequest(
                UUID.randomUUID(),

                bat,
                wide,
                noBall,
                bye,
                legBye,
                0,

                null,
                wicket
        );
    }


    protected InningsResponse score(
            Scenario scenario,
            Long inningsId,
            RecordDeliveryRequest request
    ) {

        return scoringService.recordDelivery(
                inningsId,
                scenario.actorUserId(),
                true,
                request
        );
    }


    protected record XiPlayer(
            Long playerId,
            Long registrationId,
            Long xiId
    ) {}


    private record MatchSides(
            Long teamASideId,
            Long teamBSideId
    ) {}


    protected record Scenario(

            Long actorUserId,

            Long tournamentId,
            Long editionId,
            Long matchId,

            Long teamAId,
            Long teamBId,

            XiPlayer a1,
            XiPlayer a2,
            XiPlayer a3,

            XiPlayer b1,
            XiPlayer b2,
            XiPlayer b3
    ) {}
}
