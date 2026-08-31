package com.eidcricketfest.scoring.repository;

import com.eidcricketfest.scoring.entity.Delivery;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryRepository
        extends JpaRepository<Delivery, Long> {

    @Query("""
        SELECT COALESCE(MAX(d.sequenceNo), 0)
        FROM Delivery d
        WHERE d.innings.id = :inningsId
    """)
    Integer findMaxSequence(
            @Param("inningsId") Long inningsId
    );

    Optional<Delivery>
    findFirstByInnings_IdAndVoidedAtIsNullOrderBySequenceNoDesc(
            Long inningsId
    );

    Optional<Delivery> findByClientEventId(
            UUID clientEventId
    );

    Optional<Delivery> findByUndoClientEventId(
            UUID clientEventId
    );

    boolean existsByInnings_IdAndSequenceNoGreaterThanAndVoidedAtIsNull(
            Long inningsId,
            Integer sequenceNo
    );

    @Query("""
    SELECT d
    FROM Delivery d
    JOIN FETCH d.striker s
    JOIN FETCH s.registration sr
    JOIN FETCH sr.player
    JOIN FETCH d.nonStriker ns
    JOIN FETCH ns.registration nsr
    JOIN FETCH nsr.player
    JOIN FETCH d.bowler b
    JOIN FETCH b.registration br
    JOIN FETCH br.player
    WHERE d.innings.id = :inningsId
      AND d.voidedAt IS NULL
    ORDER BY d.sequenceNo
""")
    List<Delivery> findActiveDeliveries(
            @Param("inningsId") Long inningsId
    );

    @Query("""
    SELECT d
    FROM Delivery d
    JOIN FETCH d.innings
    JOIN FETCH d.striker
    JOIN FETCH d.nonStriker
    JOIN FETCH d.bowler
    WHERE d.id = :id
      AND d.voidedAt IS NULL
""")
    Optional<Delivery> findActiveDetailedById(
            @Param("id") Long id
    );

    @Query("""
    SELECT d
    FROM Delivery d
    JOIN FETCH d.innings i
    JOIN FETCH d.striker s
    JOIN FETCH s.registration sr
    JOIN FETCH sr.player
    JOIN FETCH d.bowler b
    JOIN FETCH b.registration br
    JOIN FETCH br.player
    WHERE i.tournamentEdition.id = :editionId
      AND d.voidedAt IS NULL
    ORDER BY i.id, d.sequenceNo
""")
    List<Delivery> findActiveByEditionId(
            @Param("editionId") Long editionId
    );

    @Query("""
        SELECT d
        FROM Delivery d
        JOIN FETCH d.innings i
        JOIN FETCH d.striker s
        JOIN FETCH s.registration sr
        JOIN FETCH sr.player
        WHERE sr.player.id = :playerId
          AND d.voidedAt IS NULL
        ORDER BY i.id, d.sequenceNo
    """)
    List<Delivery> findActiveBattingDeliveriesByPlayerId(
            @Param("playerId") Long playerId
    );

    @Query("""
        SELECT d
        FROM Delivery d
        JOIN FETCH d.innings i
        JOIN FETCH d.bowler b
        JOIN FETCH b.registration br
        JOIN FETCH br.player
        WHERE br.player.id = :playerId
          AND d.voidedAt IS NULL
        ORDER BY i.id, d.sequenceNo
    """)
    List<Delivery> findActiveBowlingDeliveriesByPlayerId(
            @Param("playerId") Long playerId
    );
}
