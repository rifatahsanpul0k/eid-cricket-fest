package com.eidcricketfest.match.repository;

import com.eidcricketfest.match.entity.PlayingXiEntry;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
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
        SELECT xi.match.id,
               xi.tournamentTeam.id,
               COUNT(xi)
        FROM PlayingXiEntry xi
        WHERE xi.match.id IN :matchIds
        GROUP BY xi.match.id,
                 xi.tournamentTeam.id
    """)
    List<Object[]> countSubmittedByMatchAndTeam(
            @Param("matchIds") Collection<Long> matchIds
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
        SELECT p
        FROM PlayingXiEntry p
        JOIN FETCH p.tournamentTeam tt
        JOIN FETCH tt.team
        JOIN FETCH p.registration r
        JOIN FETCH r.player
        WHERE p.match.id = :matchId
        ORDER BY tt.id, p.captain DESC, r.player.fullName
    """)
    List<PlayingXiEntry> findDetailedByMatchId(
            @Param("matchId") Long matchId
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
        SELECT DISTINCT xi.match.id
        FROM PlayingXiEntry xi
        WHERE xi.edition.id = :editionId
          AND xi.registration.player.id = :playerId
    """)
    List<Long> findMatchIdsByEditionIdAndPlayerId(
            @Param("editionId") Long editionId,
            @Param("playerId") Long playerId
    );

    @Query("""
        SELECT COUNT(DISTINCT xi.match.id)
        FROM PlayingXiEntry xi
        WHERE xi.edition.id = :editionId
          AND xi.registration.player.id = :playerId
    """)
    long countMatchesPlayedInEdition(
            @Param("editionId") Long editionId,
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
