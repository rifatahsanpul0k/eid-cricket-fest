package com.eidcricketfest.integration.cricket;

import com.eidcricketfest.common.exception.ConflictException;
import com.eidcricketfest.scoring.dto.*;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class ScoringCorrectionIntegrationTest
        extends CricketIntegrationSupport {

    @Test
    void undoShouldRestoreScoreAndKeepAuditRecord() {

        Scenario s =
                createScenario(2);

        Long inningsId =
                startFirstInnings(s).id();

        score(
                s,
                inningsId,
                ball(4, 0, 0, 0, 0)
        );

        InningsResponse result =
                scoringService
                        .undoLastDelivery(
                                inningsId,
                                s.actorUserId(),
                                true,

                                new UndoDeliveryRequest(
                                        UUID.randomUUID(),
                                        "Incorrect score"
                                )
                        );

        assertThat(result.runs())
                .isZero();

        assertThat(result.legalBalls())
                .isZero();

        Long active =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM deliveries
                        WHERE innings_id = ?
                          AND voided_at IS NULL
                        """,
                        Long.class,
                        inningsId
                );

        Long historical =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM deliveries
                        WHERE innings_id = ?
                        """,
                        Long.class,
                        inningsId
                );

        assertThat(active)
                .isZero();

        assertThat(historical)
                .isEqualTo(1);
    }


    @Test
    void correctingLatestFourToSixShouldReplaceDelivery() {

        Scenario s =
                createScenario(2);

        Long inningsId =
                startFirstInnings(s).id();

        score(
                s,
                inningsId,
                ball(4, 0, 0, 0, 0)
        );

        var original =
                deliveryRepository
                        .findActiveDeliveries(
                                inningsId
                        )
                        .get(0);

        InningsResponse result =
                scoringService.correctDelivery(
                        original.getId(),
                        s.actorUserId(),
                        true,

                        new CorrectDeliveryRequest(
                                UUID.randomUUID(),

                                6,
                                0,
                                0,
                                0,
                                0,
                                0,

                                null,
                                null,

                                "Corrected to six",
                                "Umpire signalled six"
                        )
                );

        assertThat(result.runs())
                .isEqualTo(6);

        assertThat(result.legalBalls())
                .isEqualTo(1);

        Long totalVersions =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM deliveries
                        WHERE innings_id = ?
                        """,
                        Long.class,
                        inningsId
                );

        Long activeVersions =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM deliveries
                        WHERE innings_id = ?
                          AND voided_at IS NULL
                        """,
                        Long.class,
                        inningsId
                );

        assertThat(totalVersions)
                .isEqualTo(2);

        assertThat(activeVersions)
                .isEqualTo(1);
    }


    @Test
    void stateChangingHistoricalCorrectionShouldRequireUndoingLaterBalls() {

        Scenario s =
                createScenario(2);

        Long inningsId =
                startFirstInnings(s).id();

        score(
                s,
                inningsId,
                ball(1, 0, 0, 0, 0)
        );

        score(
                s,
                inningsId,
                ball(4, 0, 0, 0, 0)
        );

        var first =
                deliveryRepository
                        .findActiveDeliveries(
                                inningsId
                        )
                        .get(0);

        assertThatThrownBy(() ->
                scoringService.correctDelivery(
                        first.getId(),
                        s.actorUserId(),
                        true,

                        new CorrectDeliveryRequest(
                                UUID.randomUUID(),

                                2,
                                0,
                                0,
                                0,
                                0,
                                0,

                                null,
                                null,

                                null,

                                "First ball should be two runs"
                        )
                )
        )
                .isInstanceOf(
                        ConflictException.class
                )
                .hasMessageContaining(
                        "Undo later deliveries"
                );
    }
}
