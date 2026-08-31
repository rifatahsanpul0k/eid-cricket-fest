package com.eidcricketfest.match.repository;

import com.eidcricketfest.match.entity.MatchToss;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MatchTossRepository
        extends JpaRepository<MatchToss, Long> {

    boolean existsByMatch_Id(Long matchId);

    Optional<MatchToss> findByMatch_Id(Long matchId);
}
