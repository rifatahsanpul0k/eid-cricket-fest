package com.eidcricketfest.match.repository;

import com.eidcricketfest.match.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface MatchScorerRepository
        extends JpaRepository<MatchScorer, MatchScorerId> {

    boolean existsByMatch_Id(Long matchId);

    boolean existsByMatch_IdAndUser_Id(
            Long matchId,
            Long userId
    );

    @Query("""
        SELECT DISTINCT scorer.match.id
        FROM MatchScorer scorer
        WHERE scorer.match.id IN :matchIds
    """)
    List<Long> findMatchIdsWithScorer(
            @Param("matchIds") Collection<Long> matchIds
    );

    @Query("""
        SELECT scorer
        FROM MatchScorer scorer
        JOIN FETCH scorer.user user
        WHERE scorer.match.id = :matchId
        ORDER BY scorer.primaryScorer DESC,
                 user.displayName
    """)
    List<MatchScorer> findDetailedByMatchId(
            @Param("matchId") Long matchId
    );
}
