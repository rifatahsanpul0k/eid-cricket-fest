package com.eidcricketfest.match.repository;

import com.eidcricketfest.match.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CricketMatchRepository
        extends JpaRepository<CricketMatch, Long>,
        JpaSpecificationExecutor<CricketMatch> {

    @Override
    @EntityGraph(
            attributePaths = {
                    "teamA",
                    "teamA.team",
                    "teamB",
                    "teamB.team",
                    "venue"
            }
    )
    Page<CricketMatch> findAll(
            Specification<CricketMatch> spec,
            Pageable pageable
    );

    boolean existsByTournamentEdition_IdAndStage(
            Long editionId,
            MatchStage stage
    );

    long countByTournamentEdition_IdAndStage(
            Long editionId,
            MatchStage stage
    );

    long countByTournamentEdition_IdAndStageAndResultTypeIsNotNull(
            Long editionId,
            MatchStage stage
    );

    @Query("""
        SELECT COALESCE(MAX(m.matchNumber), 0)
        FROM CricketMatch m
        WHERE m.tournamentEdition.id = :editionId
    """)
    Integer findMaxMatchNumber(
            @Param("editionId") Long editionId
    );

    @Query("""
        SELECT m
        FROM CricketMatch m
        JOIN FETCH m.teamA a
        JOIN FETCH a.team
        JOIN FETCH m.teamB b
        JOIN FETCH b.team
        LEFT JOIN FETCH m.venue
        WHERE m.tournamentEdition.id = :editionId
        ORDER BY m.matchNumber
    """)
    List<CricketMatch> findDetailedByEditionId(
            @Param("editionId") Long editionId
    );

    @Query("""
        SELECT m
        FROM CricketMatch m
        JOIN FETCH m.tournamentEdition
        JOIN FETCH m.teamA a
        JOIN FETCH a.team
        JOIN FETCH m.teamB b
        JOIN FETCH b.team
        LEFT JOIN FETCH m.winnerTeam w
        LEFT JOIN FETCH w.team
        LEFT JOIN FETCH m.sourceMatchA
        LEFT JOIN FETCH m.sourceMatchB
        WHERE m.tournamentEdition.id = :editionId
          AND m.stage = :stage
        ORDER BY m.matchNumber
    """)
    List<CricketMatch> findDetailedByEditionAndStage(
            @Param("editionId") Long editionId,
            @Param("stage") MatchStage stage
    );

    @Query("""
        SELECT m
        FROM CricketMatch m
        JOIN FETCH m.tournamentEdition
        JOIN FETCH m.teamA a
        JOIN FETCH a.team
        JOIN FETCH m.teamB b
        JOIN FETCH b.team
        LEFT JOIN FETCH m.venue
        WHERE m.id = :id
    """)
    Optional<CricketMatch> findDetailedById(
            @Param("id") Long id
    );

    @Query("""
        SELECT m
        FROM CricketMatch m
        JOIN FETCH m.teamA a
        JOIN FETCH a.team
        JOIN FETCH m.teamB b
        JOIN FETCH b.team
        LEFT JOIN FETCH m.winnerTeam w
        LEFT JOIN FETCH w.team
        WHERE m.tournamentEdition.id = :editionId
          AND m.stage = com.eidcricketfest.match.entity.MatchStage.FINAL
    """)
    Optional<CricketMatch> findFinalByEditionId(
            @Param("editionId") Long editionId
    );

    @Query("""
    SELECT m
    FROM CricketMatch m
    JOIN FETCH m.tournamentEdition e
    JOIN FETCH m.teamA a
    JOIN FETCH a.team
    JOIN FETCH m.teamB b
    JOIN FETCH b.team
    LEFT JOIN FETCH m.winnerTeam w
    LEFT JOIN FETCH w.team
    WHERE m.tournamentEdition.id = :editionId
      AND m.stage = com.eidcricketfest.match.entity.MatchStage.LEAGUE
      AND m.resultType IS NOT NULL
    ORDER BY m.matchNumber
""")
    List<CricketMatch> findLeagueResults(
            @Param("editionId") Long editionId
    );
}
