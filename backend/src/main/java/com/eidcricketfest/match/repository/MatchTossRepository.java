package com.eidcricketfest.match.repository;

import com.eidcricketfest.match.entity.MatchToss;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface MatchTossRepository
        extends JpaRepository<MatchToss, Long> {

    boolean existsByMatch_Id(Long matchId);

    Optional<MatchToss> findByMatch_Id(Long matchId);

    @Query("""
        SELECT t
        FROM MatchToss t
        JOIN FETCH t.match m
        JOIN FETCH t.winnerSide ws
        LEFT JOIN FETCH ws.tournamentTeam tt
        LEFT JOIN FETCH tt.team
        WHERE m.id IN :matchIds
    """)
    List<MatchToss> findDetailedByMatchIds(
            @Param("matchIds") Set<Long> matchIds
    );
}
