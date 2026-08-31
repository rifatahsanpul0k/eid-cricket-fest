package com.eidcricketfest.integration.cricket;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ScorecardIntegrationTest
        extends CricketIntegrationSupport {

    @Test
    void shouldCalculateBattingAndBowlingFigures() {

        Scenario s =
                createScenario(2);

        Long inningsId =
                startFirstInnings(s).id();

        /*
         * A1 = 4
         */
        score(
                s,
                inningsId,
                ball(4, 0, 0, 0, 0)
        );

        /*
         * A1 = +1
         * A1 total 5 off 2
         *
         * strike changes to A2
         */
        score(
                s,
                inningsId,
                ball(1, 0, 0, 0, 0)
        );

        /*
         * A2 = 2 off 1
         */
        score(
                s,
                inningsId,
                ball(2, 0, 0, 0, 0)
        );

        var scorecard =
                scorecardService
                        .getScorecard(
                                s.matchId()
                        );

        var firstInnings =
                scorecard.innings().get(0);

        assertThat(firstInnings.runs())
                .isEqualTo(7);

        var a1 =
                firstInnings.batting()
                        .stream()
                        .filter(row ->
                                row.playerId()
                                        .equals(
                                                s.a1()
                                                        .playerId()
                                        )
                        )
                        .findFirst()
                        .orElseThrow();

        assertThat(a1.runs())
                .isEqualTo(5);

        assertThat(a1.balls())
                .isEqualTo(2);

        assertThat(a1.fours())
                .isEqualTo(1);

        assertThat(a1.strikeRate())
                .isEqualByComparingTo(
                        "250.00"
                );

        var bowler =
                firstInnings
                        .bowling()
                        .get(0);

        assertThat(bowler.runs())
                .isEqualTo(7);

        assertThat(bowler.overs())
                .isEqualTo("0.3");

        assertThat(bowler.economy())
                .isEqualByComparingTo(
                        "14.00"
                );
    }

    @Test
    void noBallShouldCountAsBallFacedButWideShouldNot() {

        Scenario s =
                createScenario(2);

        Long inningsId =
                startFirstInnings(s).id();

        /*
         * NB + 2 bat runs
         *
         * Team gets 3.
         * Batter gets 2.
         */
        score(
                s,
                inningsId,
                ball(2, 0, 1, 0, 0)
        );

        /*
         * Wide
         */
        score(
                s,
                inningsId,
                ball(0, 1, 0, 0, 0)
        );

        var innings =
                scorecardService
                        .getScorecard(
                                s.matchId()
                        )
                        .innings()
                        .get(0);

        var batter =
                innings.batting()
                        .stream()
                        .filter(row ->
                                row.playerId()
                                        .equals(
                                                s.a1()
                                                        .playerId()
                                        )
                        )
                        .findFirst()
                        .orElseThrow();

        assertThat(batter.runs())
                .isEqualTo(2);

        /*
         * No-ball counts as ball faced.
         * Wide does not.
         */
        assertThat(batter.balls())
                .isEqualTo(1);

        var bowler =
                innings.bowling().get(0);

        /*
         * 2 bat
         * + 1 NB
         * + 1 Wide
         */
        assertThat(bowler.runs())
                .isEqualTo(4);
    }

    @Test
    void byesAndLegByesShouldNotBeChargedToBowler() {

        Scenario s =
                createScenario(2);

        Long inningsId =
                startFirstInnings(s).id();

        score(
                s,
                inningsId,
                ball(0, 0, 0, 2, 0)
        );

        score(
                s,
                inningsId,
                ball(0, 0, 0, 0, 1)
        );

        var innings =
                scorecardService
                        .getScorecard(
                                s.matchId()
                        )
                        .innings()
                        .get(0);

        assertThat(innings.runs())
                .isEqualTo(3);

        var bowler =
                innings.bowling().get(0);

        assertThat(bowler.runs())
                .isZero();
    }

    @Test
    void runOutShouldNotBeCreditedToBowler() {

        Scenario s =
                createScenario(2);

        Long inningsId =
                startFirstInnings(s).id();

        score(
                s,
                inningsId,

                wicketBall(
                        0,
                        0,
                        0,
                        0,
                        0,

                        new com.eidcricketfest.scoring.dto.WicketRequest(
                                com.eidcricketfest.scoring.entity.DismissalType
                                        .RUN_OUT,

                                s.a1().xiId(),

                                s.b2().xiId()
                        )
                )
        );

        var innings =
                scorecardService
                        .getScorecard(
                                s.matchId()
                        )
                        .innings()
                        .get(0);

        assertThat(
                innings.bowling()
                        .get(0)
                        .wickets()
        ).isZero();
    }
}
