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

    long countByMatch_IdAndMatchSide_Id(
            Long matchId,
            Long matchSideId
    );

    void deleteByMatch_IdAndTournamentTeam_Id(
            Long matchId,
            Long tournamentTeamId
    );

    void deleteByMatch_IdAndMatchSide_Id(
            Long matchId,
            Long matchSideId
    );

    @Query("""
        SELECT xi.match.id,
               xi.matchSide.id,
               COUNT(xi)
        FROM PlayingXiEntry xi
        WHERE xi.match.id IN :matchIds
        GROUP BY xi.match.id,
                 xi.matchSide.id
    """)
    List<Object[]> countSubmittedByMatchAndTeam(
            @Param("matchIds") Collection<Long> matchIds
    );

    @Query("""
    SELECT p
    FROM PlayingXiEntry p
    JOIN FETCH p.matchSide ms
    LEFT JOIN FETCH ms.tournamentTeam tt
    LEFT JOIN FETCH tt.team
    LEFT JOIN FETCH p.registration r
    JOIN FETCH p.player
    WHERE p.id = :id
""")
    Optional<PlayingXiEntry> findDetailedById(
            @Param("id") Long id
    );

    @Query("""
        SELECT p
        FROM PlayingXiEntry p
        JOIN FETCH p.matchSide ms
        LEFT JOIN FETCH ms.tournamentTeam tt
        LEFT JOIN FETCH tt.team
        LEFT JOIN FETCH p.registration r
        JOIN FETCH p.player player
        WHERE p.match.id = :matchId
        ORDER BY ms.sideKey, p.captain DESC, player.fullName
    """)
    List<PlayingXiEntry> findDetailedByMatchId(
            @Param("matchId") Long matchId
    );

    @Query("""
        SELECT COUNT(DISTINCT xi.match.id)
        FROM PlayingXiEntry xi
        WHERE xi.player.id = :playerId
    """)
    long countMatchesPlayed(
            @Param("playerId") Long playerId
    );

    @Query("""
        SELECT DISTINCT xi.match.id
        FROM PlayingXiEntry xi
        WHERE xi.edition.id = :editionId
          AND xi.player.id = :playerId
    """)
    List<Long> findMatchIdsByEditionIdAndPlayerId(
            @Param("editionId") Long editionId,
            @Param("playerId") Long playerId
    );

    @Query("""
        SELECT COUNT(DISTINCT xi.match.id)
        FROM PlayingXiEntry xi
        WHERE xi.edition.id = :editionId
          AND xi.player.id = :playerId
    """)
    long countMatchesPlayedInEdition(
            @Param("editionId") Long editionId,
            @Param("playerId") Long playerId
    );

    @Query("""
        SELECT COUNT(DISTINCT xi.edition.id)
        FROM PlayingXiEntry xi
        WHERE xi.edition IS NOT NULL
          AND xi.player.id = :playerId
    """)
    long countEditionsPlayed(
            @Param("playerId") Long playerId
    );
}
