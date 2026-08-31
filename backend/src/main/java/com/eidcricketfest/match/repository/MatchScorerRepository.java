package com.eidcricketfest.match.repository;

import com.eidcricketfest.match.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchScorerRepository
        extends JpaRepository<MatchScorer, MatchScorerId> {

    boolean existsByMatch_Id(Long matchId);

    boolean existsByMatch_IdAndUser_Id(
            Long matchId,
            Long userId
    );
}
