package com.eidcricketfest.scoring.repository;

import com.eidcricketfest.scoring.entity.Wicket;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WicketRepository
        extends JpaRepository<Wicket, Long> {

    Optional<Wicket> findByDelivery_Id(Long deliveryId);

    @Query("""
        SELECT CASE WHEN COUNT(w) > 0 THEN true ELSE false END
        FROM Wicket w
        JOIN w.delivery d
        WHERE d.innings.id = :inningsId
          AND w.dismissedPlayer.id = :playingXiId
          AND d.voidedAt IS NULL
    """)
    boolean isDismissed(
            @Param("inningsId") Long inningsId,
            @Param("playingXiId") Long playingXiId
    );

    @Query("""
    SELECT w
    FROM Wicket w
    JOIN FETCH w.delivery d
    JOIN FETCH w.dismissedPlayer dp
    JOIN FETCH dp.registration dpr
    JOIN FETCH dpr.player
    LEFT JOIN FETCH w.fielder f
    LEFT JOIN FETCH f.registration fr
    LEFT JOIN FETCH fr.player
    WHERE d.innings.id = :inningsId
      AND d.voidedAt IS NULL
""")
    List<Wicket> findActiveByInningsId(
            @Param("inningsId") Long inningsId
    );

    @Query("""
    SELECT w
    FROM Wicket w
    JOIN FETCH w.delivery d
    JOIN FETCH d.innings i
    JOIN FETCH d.bowler bowler
    JOIN FETCH bowler.registration br
    JOIN FETCH br.player
    JOIN FETCH w.dismissedPlayer dp
    JOIN FETCH dp.registration dr
    JOIN FETCH dr.player
    WHERE i.tournamentEdition.id = :editionId
      AND d.voidedAt IS NULL
""")
    List<Wicket> findActiveByEditionId(
            @Param("editionId") Long editionId
    );

    @Query("""
        SELECT w
        FROM Wicket w
        JOIN FETCH w.delivery d
        JOIN FETCH d.innings
        JOIN FETCH w.dismissedPlayer dp
        JOIN FETCH dp.registration dr
        WHERE dr.player.id = :playerId
          AND d.voidedAt IS NULL
    """)
    List<Wicket> findDismissalsForPlayer(
            @Param("playerId") Long playerId
    );

    @Query("""
        SELECT w
        FROM Wicket w
        JOIN FETCH w.delivery d
        JOIN FETCH d.innings
        JOIN FETCH d.bowler b
        JOIN FETCH b.registration br
        WHERE br.player.id = :playerId
          AND w.creditedToBowler = true
          AND d.voidedAt IS NULL
    """)
    List<Wicket> findBowlerWicketsForPlayer(
            @Param("playerId") Long playerId
    );
}
