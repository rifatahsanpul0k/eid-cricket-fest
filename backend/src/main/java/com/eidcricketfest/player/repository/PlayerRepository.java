package com.eidcricketfest.player.repository;

import com.eidcricketfest.player.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PlayerRepository
        extends JpaRepository<Player, Long>,
        JpaSpecificationExecutor<Player> {

    Optional<Player> findByUser_Id(Long userId);

    boolean existsByUser_Id(Long userId);

    @Query("""
        SELECT p
        FROM Player p
        LEFT JOIN FETCH p.primaryCategory
        ORDER BY LOWER(p.fullName)
    """)
    List<Player> findAllDetailedForFriendlyOptions();
}
