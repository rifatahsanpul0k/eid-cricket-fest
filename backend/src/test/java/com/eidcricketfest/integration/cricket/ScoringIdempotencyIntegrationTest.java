package com.eidcricketfest.integration.cricket;

import com.eidcricketfest.scoring.dto.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ScoringIdempotencyIntegrationTest
        extends CricketIntegrationSupport {

    @Test
    void duplicateUndoShouldNotUndoAnotherDelivery() {

        Scenario s =
                createScenario(2);

        Long inningsId =
                startFirstInnings(s).id();

        InningsResponse afterFirst =
                score(
                        s,
                        inningsId,
                        ball(1, 0, 0, 0, 0)
                );

        InningsResponse afterSecond =
                score(
                        s,
                        inningsId,
                        ball(4, 0, 0, 0, 0)
                );

        UUID undoId =
                UUID.randomUUID();

        InningsResponse undone =
                scoringService.undoLastDelivery(
                        inningsId,
                        s.actorUserId(),
                        true,
                        new UndoDeliveryRequest(
                                undoId,
                                "Retry-safe undo"
                        )
                );

        InningsResponse retried =
                scoringService.undoLastDelivery(
                        inningsId,
                        s.actorUserId(),
                        true,
                        new UndoDeliveryRequest(
                                undoId,
                                "Retry-safe undo"
                        )
                );

        assertThat(afterFirst.scoreRevision())
                .isEqualTo(afterSecond.scoreRevision() - 1);

        assertThat(undone.runs())
                .isEqualTo(1);

        assertThat(retried.runs())
                .isEqualTo(undone.runs());

        assertThat(retried.legalBalls())
                .isEqualTo(undone.legalBalls());

        assertThat(retried.scoreRevision())
                .isEqualTo(undone.scoreRevision());

        assertThat(activeDeliveryCount(inningsId))
                .isEqualTo(1);
    }

    @Test
    void newUndoClientEventShouldUndoNextActiveDelivery() {

        Scenario s =
                createScenario(2);

        Long inningsId =
                startFirstInnings(s).id();

        score(
                s,
                inningsId,
                ball(1, 0, 0, 0, 0)
        );

        InningsResponse afterSecond =
                score(
                        s,
                        inningsId,
                        ball(4, 0, 0, 0, 0)
                );

        InningsResponse firstUndo =
                scoringService.undoLastDelivery(
                        inningsId,
                        s.actorUserId(),
                        true,
                        new UndoDeliveryRequest(
                                UUID.randomUUID(),
                                "Undo second"
                        )
                );

        InningsResponse secondUndo =
                scoringService.undoLastDelivery(
                        inningsId,
                        s.actorUserId(),
                        true,
                        new UndoDeliveryRequest(
                                UUID.randomUUID(),
                                "Undo first"
                        )
                );

        assertThat(firstUndo.scoreRevision())
                .isEqualTo(afterSecond.scoreRevision() + 1);

        assertThat(secondUndo.scoreRevision())
                .isEqualTo(firstUndo.scoreRevision() + 1);

        assertThat(secondUndo.runs())
                .isZero();

        assertThat(activeDeliveryCount(inningsId))
                .isZero();
    }

    @Test
    @Timeout(15)
    void concurrentDuplicateUndoShouldApplyOnlyOnce()
            throws Exception {

        Scenario s =
                createScenario(2);

        Long inningsId =
                startFirstInnings(s).id();

        score(
                s,
                inningsId,
                ball(1, 0, 0, 0, 0)
        );

        InningsResponse beforeUndo =
                score(
                        s,
                        inningsId,
                        ball(4, 0, 0, 0, 0)
                );

        UUID undoId =
                UUID.randomUUID();

        UndoDeliveryRequest request =
                new UndoDeliveryRequest(
                        undoId,
                        "Concurrent retry"
                );

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch ready =
                new CountDownLatch(2);

        CountDownLatch fire =
                new CountDownLatch(1);

        Callable<InningsResponse> action = () -> {
            ready.countDown();
            fire.await();

            return scoringService.undoLastDelivery(
                    inningsId,
                    s.actorUserId(),
                    true,
                    request
            );
        };

        try {

            Future<InningsResponse> first =
                    executor.submit(action);

            Future<InningsResponse> second =
                    executor.submit(action);

            ready.await();
            fire.countDown();

            InningsResponse firstResult =
                    first.get();

            InningsResponse secondResult =
                    second.get();

            assertThat(firstResult.scoreRevision())
                    .isEqualTo(secondResult.scoreRevision());

        } finally {
            executor.shutdownNow();
        }

        var innings =
                currentInningsRow(inningsId);

        assertThat(((Number) innings.get("total_runs")).intValue())
                .isEqualTo(1);

        assertThat(((Number) innings.get("legal_balls")).intValue())
                .isEqualTo(1);

        assertThat(((Number) innings.get("score_revision")).longValue())
                .isEqualTo(beforeUndo.scoreRevision() + 1);

        assertThat(activeDeliveryCount(inningsId))
                .isEqualTo(1);
    }

    @Test
    void liveResponseShouldExposeScoreRevisionSequence()
            throws Exception {

        Scenario s =
                createScenario(2);

        InningsResponse started =
                startFirstInnings(s);

        mockMvc.perform(
                        get(
                                "/api/v1/matches/{matchId}/live",
                                s.matchId()
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.innings.scoreRevision")
                                .value(started.scoreRevision())
                );

        InningsResponse scored =
                score(
                        s,
                        started.id(),
                        ball(2, 0, 0, 0, 0)
                );

        assertThat(scored.scoreRevision())
                .isEqualTo(started.scoreRevision() + 1);

        mockMvc.perform(
                        get(
                                "/api/v1/matches/{matchId}/live",
                                s.matchId()
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.innings.scoreRevision")
                                .value(scored.scoreRevision())
                );
    }

    @Test
    void correctionRetryShouldNotAdvanceRevision() {

        Scenario s =
                createScenario(2);

        Long inningsId =
                startFirstInnings(s).id();

        InningsResponse scored =
                score(
                        s,
                        inningsId,
                        ball(4, 0, 0, 0, 0)
                );

        var original =
                deliveryRepository
                        .findActiveDeliveries(inningsId)
                        .get(0);

        UUID correctionId =
                UUID.randomUUID();

        CorrectDeliveryRequest request =
                new CorrectDeliveryRequest(
                        correctionId,
                        6,
                        0,
                        0,
                        0,
                        0,
                        0,
                        null,
                        null,
                        "Correct to six",
                        "Umpire correction"
                );

        InningsResponse corrected =
                scoringService.correctDelivery(
                        original.getId(),
                        s.actorUserId(),
                        true,
                        request
                );

        InningsResponse retried =
                scoringService.correctDelivery(
                        original.getId(),
                        s.actorUserId(),
                        true,
                        request
                );

        assertThat(corrected.scoreRevision())
                .isEqualTo(scored.scoreRevision() + 1);

        assertThat(retried.scoreRevision())
                .isEqualTo(corrected.scoreRevision());

        assertThat(retried.runs())
                .isEqualTo(6);
    }

    private Long activeDeliveryCount(Long inningsId) {
        return jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM deliveries
                WHERE innings_id = ?
                  AND voided_at IS NULL
                """,
                Long.class,
                inningsId
        );
    }

    private java.util.Map<String, Object> currentInningsRow(
            Long inningsId
    ) {
        return jdbcTemplate.queryForMap(
                """
                SELECT
                    total_runs,
                    legal_balls,
                    score_revision
                FROM innings
                WHERE id = ?
                """,
                inningsId
        );
    }
}
