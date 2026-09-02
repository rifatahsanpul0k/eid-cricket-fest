package com.eidcricketfest.match.repository;

import com.eidcricketfest.match.entity.MatchSide;
import com.eidcricketfest.match.entity.MatchSideKey;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MatchSideRepository
        extends JpaRepository<MatchSide, Long> {

    @Query("""
        SELECT s
        FROM MatchSide s
        LEFT JOIN FETCH s.tournamentTeam tt
        LEFT JOIN FETCH tt.team
        WHERE s.match.id = :matchId
        ORDER BY s.sideKey
    """)
    List<MatchSide> findDetailedByMatchId(
            @Param("matchId") Long matchId
    );

    Optional<MatchSide> findByMatch_IdAndSideKey(
            Long matchId,
            MatchSideKey sideKey
    );
}
