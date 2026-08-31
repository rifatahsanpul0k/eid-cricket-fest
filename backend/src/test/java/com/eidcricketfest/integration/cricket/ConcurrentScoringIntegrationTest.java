package com.eidcricketfest.integration.cricket;

import com.eidcricketfest.scoring.dto.RecordDeliveryRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;

class ConcurrentScoringIntegrationTest
        extends CricketIntegrationSupport {

    @Test
    @Timeout(15)
    void sameClientEventShouldBeAppliedOnlyOnce()
            throws Exception {

        Scenario s =
                createScenario(2);

        Long inningsId =
                startFirstInnings(s).id();

        UUID clientEventId =
                UUID.randomUUID();

        RecordDeliveryRequest request =
                new RecordDeliveryRequest(
                        clientEventId,

                        4,
                        0,
                        0,
                        0,
                        0,
                        0,

                        null,
                        null
                );

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch ready =
                new CountDownLatch(2);

        CountDownLatch fire =
                new CountDownLatch(1);

        Callable<Void> action = () -> {

            ready.countDown();

            fire.await();

            scoringService.recordDelivery(
                    inningsId,
                    s.actorUserId(),
                    true,
                    request
            );

            return null;
        };

        try {

            Future<Void> first =
                    executor.submit(action);

            Future<Void> second =
                    executor.submit(action);

            ready.await();

            fire.countDown();

            first.get();
            second.get();

        } finally {

            executor.shutdownNow();
        }

        Long activeDeliveries =
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

        var innings =
                jdbcTemplate.queryForMap(
                        """
                        SELECT
                            total_runs,
                            legal_balls
                        FROM innings
                        WHERE id = ?
                        """,
                        inningsId
                );

        /*
         * Two HTTP calls.
         *
         * But they represent one scoring event.
         */
        assertThat(activeDeliveries)
                .isEqualTo(1);

        assertThat(
                ((Number) innings.get("total_runs"))
                        .intValue()
        ).isEqualTo(4);

        assertThat(
                ((Number) innings.get("legal_balls"))
                        .intValue()
        ).isEqualTo(1);
    }

    @Test
    @Timeout(15)
    void differentClientEventsShouldBothBeRecorded()
            throws Exception {

        Scenario s =
                createScenario(2);

        Long inningsId =
                startFirstInnings(s).id();

        RecordDeliveryRequest first =
                new RecordDeliveryRequest(
                        UUID.randomUUID(),
                        1,
                        0, 0, 0, 0, 0,
                        null,
                        null
                );

        RecordDeliveryRequest second =
                new RecordDeliveryRequest(
                        UUID.randomUUID(),
                        1,
                        0, 0, 0, 0, 0,
                        null,
                        null
                );

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch fire =
                new CountDownLatch(1);

        try {

            Future<?> f1 =
                    executor.submit(() -> {

                        try {
                            fire.await();

                            scoringService.recordDelivery(
                                    inningsId,
                                    s.actorUserId(),
                                    true,
                                    first
                            );

                        } catch (InterruptedException ex) {
                            Thread.currentThread()
                                    .interrupt();
                        }
                    });

            Future<?> f2 =
                    executor.submit(() -> {

                        try {
                            fire.await();

                            scoringService.recordDelivery(
                                    inningsId,
                                    s.actorUserId(),
                                    true,
                                    second
                            );

                        } catch (InterruptedException ex) {
                            Thread.currentThread()
                                    .interrupt();
                        }
                    });

            fire.countDown();

            f1.get();
            f2.get();

        } finally {

            executor.shutdownNow();
        }

        Long count =
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

        assertThat(count)
                .isEqualTo(2);

        /*
         * Pessimistic innings lock ensures sequence
         * numbers become 1 and 2, not both 1.
         */
        var sequences =
                jdbcTemplate.queryForList(
                        """
                        SELECT sequence_no
                        FROM deliveries
                        WHERE innings_id = ?
                          AND voided_at IS NULL
                        ORDER BY sequence_no
                        """,
                        Integer.class,
                        inningsId
                );

        assertThat(sequences)
                .containsExactly(
                        1,
                        2
                );
    }
}
