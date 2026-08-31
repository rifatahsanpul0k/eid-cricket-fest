package com.eidcricketfest.match.repository;

import com.eidcricketfest.match.entity.PlayingXiEntry;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PlayingXiEntryRepository
        extends JpaRepository<PlayingXiEntry, Long> {

    long countByMatch_IdAndTournamentTeam_Id(
            Long matchId,
            Long tournamentTeamId
    );

    void deleteByMatch_IdAndTournamentTeam_Id(
            Long matchId,
            Long tournamentTeamId
    );

    @Query("""
    SELECT p
    FROM PlayingXiEntry p
    JOIN FETCH p.tournamentTeam tt
    JOIN FETCH p.registration r
    JOIN FETCH r.player
    WHERE p.id = :id
""")
    Optional<PlayingXiEntry> findDetailedById(
            @Param("id") Long id
    );

    @Query("""
        SELECT COUNT(DISTINCT xi.match.id)
        FROM PlayingXiEntry xi
        WHERE xi.registration.player.id = :playerId
    """)
    long countMatchesPlayed(
            @Param("playerId") Long playerId
    );

    @Query("""
        SELECT COUNT(DISTINCT xi.edition.id)
        FROM PlayingXiEntry xi
        WHERE xi.registration.player.id = :playerId
    """)
    long countEditionsPlayed(
            @Param("playerId") Long playerId
    );
}
