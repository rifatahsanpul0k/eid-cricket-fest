package com.eidcricketfest.integration.cricket;

import com.eidcricketfest.common.exception.ConflictException;
import com.eidcricketfest.scoring.dto.*;
import com.eidcricketfest.scoring.entity.*;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ScoringRulesIntegrationTest
        extends CricketIntegrationSupport {

    @Test
    void shouldScoreDotOneTwoFourSixWideAndNoBall() {

        Scenario s =
                createScenario(2);

        InningsResponse innings =
                startFirstInnings(s);

        Long id = innings.id();

        score(s, id, ball(0, 0, 0, 0, 0)); // dot

        score(s, id, ball(1, 0, 0, 0, 0));

        score(s, id, ball(2, 0, 0, 0, 0));

        score(s, id, ball(4, 0, 0, 0, 0));

        score(s, id, ball(6, 0, 0, 0, 0));

        /*
         * Wide:
         * +1 run
         * no legal delivery
         */
        score(s, id, ball(0, 1, 0, 0, 0));

        /*
         * No-ball + FOUR:
         *
         * 1 NB + 4 batter = 5
         */
        InningsResponse result =
                score(
                        s,
                        id,
                        ball(4, 0, 1, 0, 0)
                );

        assertThat(result.runs())
                .isEqualTo(19);

        assertThat(result.legalBalls())
                .isEqualTo(5);

        assertThat(result.overs())
                .isEqualTo("0.5");

        assertThat(result.wides())
                .isEqualTo(1);

        assertThat(result.noBalls())
                .isEqualTo(1);
    }


    @Test
    void wideAndNoBallTogetherShouldBeRejected() {

        Scenario s =
                createScenario(2);

        Long inningsId =
                startFirstInnings(s).id();

        assertThatThrownBy(() ->
                score(
                        s,
                        inningsId,
                        ball(
                                0,
                                1,
                                1,
                                0,
                                0
                        )
                )
        )
                .isInstanceOf(
                        ConflictException.class
                );
    }


    @Test
    void byeAndLegByeShouldBeRecordedAsExtras() {

        Scenario s =
                createScenario(2);

        Long inningsId =
                startFirstInnings(s).id();

        score(
                s,
                inningsId,
                ball(0, 0, 0, 2, 0)
        );

        InningsResponse result =
                score(
                        s,
                        inningsId,
                        ball(0, 0, 0, 0, 1)
                );

        assertThat(result.runs())
                .isEqualTo(3);

        assertThat(result.legalBalls())
                .isEqualTo(2);

        assertThat(result.byes())
                .isEqualTo(2);

        assertThat(result.legByes())
                .isEqualTo(1);
    }


    @Test
    void batRunsAndByesTogetherShouldBeRejected() {

        Scenario s =
                createScenario(2);

        Long inningsId =
                startFirstInnings(s).id();

        assertThatThrownBy(() ->
                score(
                        s,
                        inningsId,
                        ball(
                                1,
                                0,
                                0,
                                1,
                                0
                        )
                )
        )
                .isInstanceOf(
                        ConflictException.class
                );
    }

    @Test
    void sixLegalBallsShouldEndOverSwapEndsAndRequireNewBowler() {

        Scenario s =
                createScenario(2);

        Long inningsId =
                startFirstInnings(s).id();

        for (int i = 0; i < 6; i++) {

            score(
                    s,
                    inningsId,
                    ball(0, 0, 0, 0, 0)
            );
        }

        var state =
                jdbcTemplate.queryForMap(
                        """
                        SELECT
                            legal_balls,
                            current_striker_xi_id,
                            current_non_striker_xi_id,
                            current_bowler_xi_id
                        FROM innings
                        WHERE id = ?
                        """,
                        inningsId
                );

        assertThat(
                ((Number) state.get("legal_balls"))
                        .intValue()
        ).isEqualTo(6);

        /*
         * Ends switch at the end of the over.
         */
        assertThat(
                ((Number) state.get(
                        "current_striker_xi_id"
                )).longValue()
        ).isEqualTo(
                s.a2().xiId()
        );

        assertThat(
                ((Number) state.get(
                        "current_non_striker_xi_id"
                )).longValue()
        ).isEqualTo(
                s.a1().xiId()
        );

        /*
         * New over requires selecting bowler.
         */
        assertThat(
                state.get("current_bowler_xi_id")
        ).isNull();
    }

    @Test
    void creditedWicketShouldPreserveSurvivingBatterUntilIncomingBatterIsSelected() {

        Scenario s =
                createScenario(2);

        Long inningsId =
                startFirstInnings(s).id();

        InningsResponse result =
                score(
                        s,
                        inningsId,

                        wicketBall(
                                0,
                                0,
                                0,
                                0,
                                0,

                                new WicketRequest(
                                        DismissalType.BOWLED,
                                        s.a1().xiId(),
                                        null
                                )
                        )
                );

        assertThat(result.wickets())
                .isEqualTo(1);

        var state =
                jdbcTemplate.queryForMap(
                        """
                        SELECT
                            current_striker_xi_id,
                            current_non_striker_xi_id
                        FROM innings
                        WHERE id = ?
                        """,
                        inningsId
                );

        assertThat(
                state.get("current_striker_xi_id")
        ).isNull();

        assertThat(
                ((Number) state.get(
                        "current_non_striker_xi_id"
                )).longValue()
        ).isEqualTo(s.a2().xiId());


        /*
         * Dismissed A1 cannot return.
         */
        assertThatThrownBy(() ->
                scoringService.setBatters(
                        inningsId,
                        s.actorUserId(),
                        true,

                        new SetBattersRequest(
                                s.a1().xiId(),
                                s.a2().xiId()
                        )
                )
        )
                .isInstanceOf(
                        ConflictException.class
                );


        /*
         * A3 replaces A1.
         */
        scoringService.setBatters(
                inningsId,
                s.actorUserId(),
                true,

                new SetBattersRequest(
                        s.a3().xiId(),
                        s.a2().xiId()
                )
        );
    }

    @Test
    void ambiguousRunOutShouldStillRequireExplicitBatterSelection() {

        Scenario s =
                createScenario(2);

        Long inningsId =
                startFirstInnings(s).id();

        score(
                s,
                inningsId,

                wicketBall(
                        1,
                        0,
                        0,
                        0,
                        0,

                        new WicketRequest(
                                DismissalType.RUN_OUT,
                                s.a1().xiId(),
                                s.b2().xiId()
                        )
                )
        );

        var state =
                jdbcTemplate.queryForMap(
                        """
                        SELECT
                            current_striker_xi_id,
                            current_non_striker_xi_id
                        FROM innings
                        WHERE id = ?
                        """,
                        inningsId
                );

        assertThat(state.get("current_striker_xi_id"))
                .isNull();

        assertThat(state.get("current_non_striker_xi_id"))
                .isNull();
    }

    @Test
    void scorerStateShouldExposeDismissedPlayersAndUndoShouldRestoreEligibility() {

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
                        new WicketRequest(
                                DismissalType.BOWLED,
                                s.a1().xiId(),
                                null
                        )
                )
        );

        ScorerMatchStateResponse afterWicket =
                scorerMatchQueryService.matchState(
                        s.matchId(),
                        s.actorUserId(),
                        true
                );

        assertThat(afterWicket.dismissedPlayingXiIds())
                .containsExactly(s.a1().xiId());

        scoringService.undoLastDelivery(
                inningsId,
                s.actorUserId(),
                true,
                new UndoDeliveryRequest(
                        java.util.UUID.randomUUID(),
                        "Undo wicket"
                )
        );

        ScorerMatchStateResponse afterUndo =
                scorerMatchQueryService.matchState(
                        s.matchId(),
                        s.actorUserId(),
                        true
                );

        assertThat(afterUndo.dismissedPlayingXiIds())
                .isEmpty();

        assertThat(afterUndo.live().innings().striker().playerId())
                .isEqualTo(s.a1().playerId());
    }

    @Test
    void previousOverBowlerCannotBowlConsecutiveCompletedOvers() {

        Scenario s =
                createScenario(3);

        Long inningsId =
                startFirstInnings(s).id();

        for (int i = 0; i < 6; i++) {
            score(s, inningsId, ball(0, 0, 0, 0, 0));
        }

        ScorerMatchStateResponse afterFirstOver =
                scorerMatchQueryService.matchState(
                        s.matchId(),
                        s.actorUserId(),
                        true
                );

        assertThat(afterFirstOver.previousOverBowlerPlayingXiId())
                .isEqualTo(s.b1().xiId());

        assertThatThrownBy(() ->
                scoringService.setBowler(
                        inningsId,
                        s.actorUserId(),
                        true,
                        new SetBowlerRequest(s.b1().xiId())
                )
        )
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("consecutive");

        scoringService.setBowler(
                inningsId,
                s.actorUserId(),
                true,
                new SetBowlerRequest(s.b2().xiId())
        );

        for (int i = 0; i < 6; i++) {
            score(s, inningsId, ball(0, 0, 0, 0, 0));
        }

        ScorerMatchStateResponse afterSecondOver =
                scorerMatchQueryService.matchState(
                        s.matchId(),
                        s.actorUserId(),
                        true
                );

        assertThat(afterSecondOver.previousOverBowlerPlayingXiId())
                .isEqualTo(s.b2().xiId());

        assertThatCode(() ->
                scoringService.setBowler(
                        inningsId,
                        s.actorUserId(),
                        true,
                        new SetBowlerRequest(s.b1().xiId())
                )
        ).doesNotThrowAnyException();
    }

    @Test
    void widesAndNoBallsDoNotCompleteOverOrExcludeBowlerEarly() {

        Scenario s =
                createScenario(2);

        Long inningsId =
                startFirstInnings(s).id();

        for (int i = 0; i < 5; i++) {
            score(s, inningsId, ball(0, 0, 0, 0, 0));
        }

        score(s, inningsId, ball(0, 1, 0, 0, 0));
        score(s, inningsId, ball(0, 0, 1, 0, 0));

        ScorerMatchStateResponse beforeSixthLegal =
                scorerMatchQueryService.matchState(
                        s.matchId(),
                        s.actorUserId(),
                        true
                );

        assertThat(beforeSixthLegal.live().innings().bowler().playerId())
                .isEqualTo(s.b1().playerId());

        assertThat(beforeSixthLegal.previousOverBowlerPlayingXiId())
                .isNull();

        score(s, inningsId, ball(0, 0, 0, 0, 0));

        ScorerMatchStateResponse afterSixthLegal =
                scorerMatchQueryService.matchState(
                        s.matchId(),
                        s.actorUserId(),
                        true
                );

        assertThat(afterSixthLegal.live().innings().bowler())
                .isNull();

        assertThat(afterSixthLegal.previousOverBowlerPlayingXiId())
                .isEqualTo(s.b1().xiId());
    }

    @Test
    void undoSixthLegalBallShouldRestoreCurrentBowlerAndRemoveTransition() {

        Scenario s =
                createScenario(2);

        Long inningsId =
                startFirstInnings(s).id();

        for (int i = 0; i < 6; i++) {
            score(s, inningsId, ball(0, 0, 0, 0, 0));
        }

        ScorerMatchStateResponse afterOver =
                scorerMatchQueryService.matchState(
                        s.matchId(),
                        s.actorUserId(),
                        true
                );

        assertThat(afterOver.live().innings().bowler())
                .isNull();

        assertThat(afterOver.previousOverBowlerPlayingXiId())
                .isEqualTo(s.b1().xiId());

        scoringService.undoLastDelivery(
                inningsId,
                s.actorUserId(),
                true,
                new UndoDeliveryRequest(
                        java.util.UUID.randomUUID(),
                        "Undo sixth legal"
                )
        );

        ScorerMatchStateResponse afterUndo =
                scorerMatchQueryService.matchState(
                        s.matchId(),
                        s.actorUserId(),
                        true
                );

        assertThat(afterUndo.live().innings().overs())
                .isEqualTo("0.5");

        assertThat(afterUndo.live().innings().bowler().playerId())
                .isEqualTo(s.b1().playerId());

        assertThat(afterUndo.previousOverBowlerPlayingXiId())
                .isNull();
    }

    @Test
    void correctingSixthLegalBallToIllegalShouldRestoreCurrentBowler() {

        Scenario s =
                createScenario(2);

        Long inningsId =
                startFirstInnings(s).id();

        for (int i = 0; i < 6; i++) {
            score(s, inningsId, ball(0, 0, 0, 0, 0));
        }

        Long deliveryId =
                deliveryRepository
                        .findFirstByInnings_IdAndVoidedAtIsNullOrderBySequenceNoDesc(
                                inningsId
                        )
                        .orElseThrow()
                        .getId();

        scoringService.correctDelivery(
                deliveryId,
                s.actorUserId(),
                true,
                new CorrectDeliveryRequest(
                        java.util.UUID.randomUUID(),
                        0,
                        1,
                        0,
                        0,
                        0,
                        0,
                        null,
                        null,
                        null,
                        "Correct sixth legal to wide"
                )
        );

        ScorerMatchStateResponse state =
                scorerMatchQueryService.matchState(
                        s.matchId(),
                        s.actorUserId(),
                        true
                );

        assertThat(state.live().innings().overs())
                .isEqualTo("0.5");

        assertThat(state.live().innings().bowler().playerId())
                .isEqualTo(s.b1().playerId());

        assertThat(state.previousOverBowlerPlayingXiId())
                .isNull();
    }

    @Test
    void correctingIllegalDeliveryToSixthLegalBallShouldRequireNextBowler() {

        Scenario s =
                createScenario(2);

        Long inningsId =
                startFirstInnings(s).id();

        for (int i = 0; i < 5; i++) {
            score(s, inningsId, ball(0, 0, 0, 0, 0));
        }

        score(s, inningsId, ball(0, 1, 0, 0, 0));

        Long deliveryId =
                deliveryRepository
                        .findFirstByInnings_IdAndVoidedAtIsNullOrderBySequenceNoDesc(
                                inningsId
                        )
                        .orElseThrow()
                        .getId();

        scoringService.correctDelivery(
                deliveryId,
                s.actorUserId(),
                true,
                new CorrectDeliveryRequest(
                        java.util.UUID.randomUUID(),
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        null,
                        null,
                        null,
                        "Correct wide to sixth legal"
                )
        );

        ScorerMatchStateResponse state =
                scorerMatchQueryService.matchState(
                        s.matchId(),
                        s.actorUserId(),
                        true
                );

        assertThat(state.live().innings().overs())
                .isEqualTo("1.0");

        assertThat(state.live().innings().bowler())
                .isNull();

        assertThat(state.previousOverBowlerPlayingXiId())
                .isEqualTo(s.b1().xiId());
    }

    @Test
    void correctionRemovingWicketRestoresBatterEligibility() {

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
                        new WicketRequest(
                                DismissalType.BOWLED,
                                s.a1().xiId(),
                                null
                        )
                )
        );

        Long deliveryId =
                deliveryRepository
                        .findFirstByInnings_IdAndVoidedAtIsNullOrderBySequenceNoDesc(
                                inningsId
                        )
                        .orElseThrow()
                        .getId();

        scoringService.correctDelivery(
                deliveryId,
                s.actorUserId(),
                true,
                new CorrectDeliveryRequest(
                        java.util.UUID.randomUUID(),
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        null,
                        null,
                        null,
                        "Remove wicket"
                )
        );

        ScorerMatchStateResponse state =
                scorerMatchQueryService.matchState(
                        s.matchId(),
                        s.actorUserId(),
                        true
                );

        assertThat(state.dismissedPlayingXiIds())
                .isEmpty();

        assertThat(state.live().innings().striker().playerId())
                .isEqualTo(s.a1().playerId());
    }

    @Test
    void bowledShouldNotBeAllowedFromNoBall() {

        Scenario s =
                createScenario(2);

        Long inningsId =
                startFirstInnings(s).id();

        assertThatThrownBy(() ->
                score(
                        s,
                        inningsId,

                        wicketBall(
                                0,
                                0,
                                1,
                                0,
                                0,

                                new WicketRequest(
                                        DismissalType.BOWLED,
                                        s.a1().xiId(),
                                        null
                                )
                        )
                )
        )
                .isInstanceOf(
                        ConflictException.class
                );
    }

    @Test
    void runOutShouldBeAllowedFromNoBall() {

        Scenario s =
                createScenario(2);

        Long inningsId =
                startFirstInnings(s).id();

        InningsResponse result =
                score(
                        s,
                        inningsId,

                        wicketBall(
                                0,
                                0,
                                1,
                                0,
                                0,

                                new WicketRequest(
                                        DismissalType.RUN_OUT,
                                        s.a1().xiId(),
                                        s.b2().xiId()
                                )
                        )
                );

        assertThat(result.wickets())
                .isEqualTo(1);

        /*
         * No-ball is not a legal delivery.
         */
        assertThat(result.legalBalls())
                .isZero();

        assertThat(result.runs())
                .isEqualTo(1);
    }

    @Test
    void inningsShouldCompleteWhenAllOut() {

        Scenario s =
                createScenario(2);

        Long inningsId =
                startFirstInnings(s).id();

        /*
         * Playing XI = 3
         *
         * Maximum wickets = 2
         */

        score(
                s,
                inningsId,

                wicketBall(
                        0, 0, 0, 0, 0,
                        new WicketRequest(
                                DismissalType.BOWLED,
                                s.a1().xiId(),
                                null
                        )
                )
        );

        scoringService.setBatters(
                inningsId,
                s.actorUserId(),
                true,

                new SetBattersRequest(
                        s.a3().xiId(),
                        s.a2().xiId()
                )
        );

        InningsResponse result =
                score(
                        s,
                        inningsId,

                        wicketBall(
                                0, 0, 0, 0, 0,
                                new WicketRequest(
                                        DismissalType.BOWLED,
                                        s.a3().xiId(),
                                        null
                                )
                        )
                );

        assertThat(result.wickets())
                .isEqualTo(2);

        assertThat(result.status())
                .isEqualTo(
                        InningsStatus.COMPLETED
                );

        var match =
                matchRepository
                        .findDetailedById(
                                s.matchId()
                        )
                        .orElseThrow();

        assertThat(match.getStatus())
                .isEqualTo(
                        com.eidcricketfest.match.entity.MatchStatus
                                .INNINGS_BREAK
                );
    }

    @Test
    void secondInningsShouldCompleteImmediatelyWhenTargetReached() {

        Scenario s =
                createScenario(1);

        Long firstId =
                startFirstInnings(s).id();

        /*
         * Team A = 6/0 from 1 over.
         */
        score(
                s,
                firstId,
                ball(6, 0, 0, 0, 0)
        );

        for (int i = 0; i < 5; i++) {

            score(
                    s,
                    firstId,
                    ball(0, 0, 0, 0, 0)
            );
        }

        /*
         * Target = 7
         */

        InningsResponse second =
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

        score(
                s,
                second.id(),
                ball(4, 0, 0, 0, 0)
        );

        InningsResponse chase =
                score(
                        s,
                        second.id(),
                        ball(3, 0, 0, 0, 0)
                );

        assertThat(chase.runs())
                .isEqualTo(7);

        assertThat(chase.status())
                .isEqualTo(
                        InningsStatus.COMPLETED
                );

        var match =
                matchRepository
                        .findDetailedById(
                                s.matchId()
                        )
                        .orElseThrow();

        assertThat(match.getStatus())
                .isEqualTo(
                        com.eidcricketfest.match.entity.MatchStatus
                                .COMPLETED
                );

        assertThat(
                match.getWinnerTeam().getId()
        ).isEqualTo(
                s.teamBId()
        );

        assertThat(match.getResultType())
                .isEqualTo(
                        com.eidcricketfest.match.entity.MatchResultType
                                .WICKETS
                );
    }
}
