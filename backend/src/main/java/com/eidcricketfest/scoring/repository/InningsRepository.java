package com.eidcricketfest.scoring.repository;

import com.eidcricketfest.scoring.entity.Innings;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface InningsRepository
        extends JpaRepository<Innings, Long> {

    boolean existsByMatch_IdAndInningsNumber(
            Long matchId,
            Short inningsNumber
    );

    boolean existsByMatch_Id(Long matchId);

    Optional<Innings>
    findByMatch_IdAndInningsNumber(
            Long matchId,
            Short inningsNumber
    );

    List<Innings>
    findByMatch_IdOrderByInningsNumber(
            Long matchId
    );

    @Query("""
        SELECT i
        FROM Innings i
        JOIN FETCH i.match m
        JOIN FETCH i.battingSide bs
        JOIN FETCH i.bowlingSide bws
        LEFT JOIN FETCH i.currentStriker striker
        LEFT JOIN FETCH striker.player
        LEFT JOIN FETCH i.currentNonStriker nonStriker
        LEFT JOIN FETCH nonStriker.player
        LEFT JOIN FETCH i.currentBowler bowler
        LEFT JOIN FETCH bowler.player
        WHERE m.id IN :matchIds
        ORDER BY m.id, i.inningsNumber
    """)
    List<Innings> findDetailedByMatchIds(
            @Param("matchIds") Set<Long> matchIds
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT i
        FROM Innings i
        WHERE i.id = :id
    """)
    Optional<Innings> findByIdForUpdate(
            @Param("id") Long id
    );
}
