package com.eidcricketfest.integration.cricket;

import com.eidcricketfest.scoring.dto.StartInningsRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class StandingsNrrIntegrationTest
        extends CricketIntegrationSupport {

    @Test
    void completedLeagueMatchShouldUpdatePointsAndNrr() {

        Scenario s =
                createScenario(1);

        Long first =
                startFirstInnings(s).id();

        /*
         * Team A = 6/0 after 1 over
         */
        score(
                s,
                first,
                ball(6, 0, 0, 0, 0)
        );

        for (int i = 0; i < 5; i++) {

            score(
                    s,
                    first,
                    ball(0, 0, 0, 0, 0)
            );
        }

        var second =
                scoringService.startInnings(
                        s.matchId(),
                        s.actorUserId(),
                        true,

                        new StartInningsRequest(
                                s.b1().xiId(),
                                s.b2().xiId(),
                                s.a1().xiId()
                        )
                );

        /*
         * Team B = 3/0 after 1 over.
         */
        score(
                s,
                second.id(),
                ball(3, 0, 0, 0, 0)
        );

        for (int i = 0; i < 5; i++) {

            score(
                    s,
                    second.id(),
                    ball(0, 0, 0, 0, 0)
            );
        }

        var standings =
                standingsService.getStandings(
                        s.editionId()
                );

        var teamA =
                standings.standings()
                        .stream()
                        .filter(row ->
                                row.tournamentTeamId()
                                        .equals(
                                                s.teamAId()
                                        )
                        )
                        .findFirst()
                        .orElseThrow();

        var teamB =
                standings.standings()
                        .stream()
                        .filter(row ->
                                row.tournamentTeamId()
                                        .equals(
                                                s.teamBId()
                                        )
                        )
                        .findFirst()
                        .orElseThrow();

        assertThat(teamA.won())
                .isEqualTo(1);

        assertThat(teamA.points())
                .isEqualByComparingTo(
                        "2.00"
                );

        /*
         * A:
         * 6 runs / 1 over = 6 RPO
         * conceded 3 / 1 = 3
         *
         * NRR = +3
         */
        assertThat(teamA.netRunRate())
                .isEqualByComparingTo(
                        "3.000"
                );

        assertThat(teamB.netRunRate())
                .isEqualByComparingTo(
                        "-3.000"
                );
    }

    @Test
    void allOutEarlyShouldUseFullOverQuotaForNrr() {

        Scenario s =
                createScenario(2);

        Long first =
                startFirstInnings(s).id();

        /*
         * A = 4
         */
        score(
                s,
                first,
                ball(4, 0, 0, 0, 0)
        );

        /*
         * A1 out
         */
        score(
                s,
                first,

                wicketBall(
                        0, 0, 0, 0, 0,

                        new com.eidcricketfest.scoring.dto.WicketRequest(
                                com.eidcricketfest.scoring.entity.DismissalType
                                        .BOWLED,

                                s.a1().xiId(),
                                null
                        )
                )
        );

        scoringService.setBatters(
                first,
                s.actorUserId(),
                true,

                new com.eidcricketfest.scoring.dto.SetBattersRequest(
                        s.a3().xiId(),
                        s.a2().xiId()
                )
        );

        /*
         * A3 out.
         *
         * A = 4 all out after only 3 balls.
         */
        score(
                s,
                first,

                wicketBall(
                        0, 0, 0, 0, 0,

                        new com.eidcricketfest.scoring.dto.WicketRequest(
                                com.eidcricketfest.scoring.entity.DismissalType
                                        .BOWLED,

                                s.a3().xiId(),
                                null
                        )
                )
        );

        var second =
                scoringService.startInnings(
                        s.matchId(),
                        s.actorUserId(),
                        true,

                        new StartInningsRequest(
                                s.b1().xiId(),
                                s.b2().xiId(),
                                s.a1().xiId()
                        )
                );

        /*
         * Target = 5.
         *
         * B hits 6 first ball and wins.
         */
        score(
                s,
                second.id(),
                ball(6, 0, 0, 0, 0)
        );

        var table =
                standingsService
                        .getStandings(
                                s.editionId()
                        );

        var teamA =
                table.standings()
                        .stream()
                        .filter(row ->
                                row.tournamentTeamId()
                                        .equals(
                                                s.teamAId()
                                        )
                        )
                        .findFirst()
                        .orElseThrow();

        /*
         * A all out:
         *
         * 4 / full 2 overs = 2 RPO.
         *
         * B:
         * 6 from 1 ball = 36 RPO.
         *
         * A NRR = 2 - 36 = -34.
         */
        assertThat(teamA.netRunRate())
                .isEqualByComparingTo(
                        "-34.000"
                );
    }
}
