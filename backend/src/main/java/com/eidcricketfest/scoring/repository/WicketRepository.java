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
    JOIN FETCH dp.player
    LEFT JOIN FETCH w.fielder f
    LEFT JOIN FETCH f.player
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
    JOIN i.match m
    JOIN FETCH d.bowler bowler
    JOIN FETCH bowler.player
    JOIN FETCH w.dismissedPlayer dp
    JOIN FETCH dp.player
    WHERE i.tournamentEdition.id = :editionId
      AND m.matchType = com.eidcricketfest.match.entity.MatchType.TOURNAMENT
      AND m.status = com.eidcricketfest.match.entity.MatchStatus.COMPLETED
      AND m.resultStatus = com.eidcricketfest.match.entity.MatchResultStatus.OFFICIAL
      AND d.voidedAt IS NULL
""")
    List<Wicket> findActiveByEditionId(
            @Param("editionId") Long editionId
    );

    @Query("""
        SELECT w
        FROM Wicket w
        JOIN FETCH w.delivery d
        JOIN FETCH d.innings i
        JOIN FETCH w.dismissedPlayer dp
        JOIN FETCH dp.player p
        WHERE p.id = :playerId
          AND d.voidedAt IS NULL
    """)
    List<Wicket> findDismissalsForPlayer(
            @Param("playerId") Long playerId
    );

    @Query("""
        SELECT w
        FROM Wicket w
        JOIN FETCH w.delivery d
        JOIN FETCH d.innings i
        JOIN FETCH w.dismissedPlayer dp
        JOIN FETCH dp.player p
        WHERE i.tournamentEdition.id = :editionId
          AND p.id = :playerId
          AND d.voidedAt IS NULL
    """)
    List<Wicket> findDismissalsForPlayerInEdition(
            @Param("editionId") Long editionId,
            @Param("playerId") Long playerId
    );

    @Query("""
        SELECT w
        FROM Wicket w
        JOIN FETCH w.delivery d
        JOIN FETCH d.innings i
        JOIN FETCH d.bowler b
        JOIN FETCH b.player p
        WHERE p.id = :playerId
          AND w.creditedToBowler = true
          AND d.voidedAt IS NULL
    """)
    List<Wicket> findBowlerWicketsForPlayer(
            @Param("playerId") Long playerId
    );

    @Query("""
        SELECT w
        FROM Wicket w
        JOIN FETCH w.delivery d
        JOIN FETCH d.innings i
        JOIN FETCH d.bowler b
        JOIN FETCH b.player p
        WHERE i.tournamentEdition.id = :editionId
          AND p.id = :playerId
          AND w.creditedToBowler = true
          AND d.voidedAt IS NULL
    """)
    List<Wicket> findBowlerWicketsForPlayerInEdition(
            @Param("editionId") Long editionId,
            @Param("playerId") Long playerId
    );

    @Query("""
        SELECT w
        FROM Wicket w
        JOIN FETCH w.delivery d
        JOIN FETCH d.innings i
        JOIN FETCH w.fielder f
        JOIN FETCH f.player p
        WHERE i.tournamentEdition.id = :editionId
          AND p.id = :playerId
          AND d.voidedAt IS NULL
    """)
    List<Wicket> findFieldingDismissalsForPlayerInEdition(
            @Param("editionId") Long editionId,
            @Param("playerId") Long playerId
    );
}
