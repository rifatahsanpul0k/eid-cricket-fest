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
import java.time.Instant;

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
                    "teamASide",
                    "teamASide.tournamentTeam",
                    "teamASide.tournamentTeam.team",
                    "teamBSide",
                    "teamBSide.tournamentTeam",
                    "teamBSide.tournamentTeam.team",
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
        LEFT JOIN FETCH m.teamA a
        LEFT JOIN FETCH a.team
        LEFT JOIN FETCH m.teamB b
        LEFT JOIN FETCH b.team
        LEFT JOIN FETCH m.teamASide asd
        LEFT JOIN FETCH asd.tournamentTeam astt
        LEFT JOIN FETCH astt.team
        LEFT JOIN FETCH m.teamBSide bsd
        LEFT JOIN FETCH bsd.tournamentTeam bstt
        LEFT JOIN FETCH bstt.team
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
        LEFT JOIN FETCH m.teamASide asd
        LEFT JOIN FETCH asd.tournamentTeam astt
        LEFT JOIN FETCH astt.team
        LEFT JOIN FETCH m.teamBSide bsd
        LEFT JOIN FETCH bsd.tournamentTeam bstt
        LEFT JOIN FETCH bstt.team
        LEFT JOIN FETCH m.winnerTeam w
        LEFT JOIN FETCH w.team
        LEFT JOIN FETCH m.winnerSide ws
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
        LEFT JOIN FETCH m.tournamentEdition
        LEFT JOIN FETCH m.teamA a
        LEFT JOIN FETCH a.team
        LEFT JOIN FETCH m.teamB b
        LEFT JOIN FETCH b.team
        LEFT JOIN FETCH m.teamASide asd
        LEFT JOIN FETCH asd.tournamentTeam astt
        LEFT JOIN FETCH astt.team
        LEFT JOIN FETCH m.teamBSide bsd
        LEFT JOIN FETCH bsd.tournamentTeam bstt
        LEFT JOIN FETCH bstt.team
        LEFT JOIN FETCH m.venue
        LEFT JOIN FETCH m.rematchOfMatch
        LEFT JOIN FETCH m.supersededByMatch
        WHERE m.id = :id
    """)
    Optional<CricketMatch> findDetailedById(
            @Param("id") Long id
    );

    @Query("""
        SELECT m
        FROM CricketMatch m
        LEFT JOIN FETCH m.teamA a
        LEFT JOIN FETCH a.team
        LEFT JOIN FETCH m.teamB b
        LEFT JOIN FETCH b.team
        LEFT JOIN FETCH m.teamASide asd
        LEFT JOIN FETCH m.teamBSide bsd
        WHERE m.sourceMatchA.id = :sourceMatchId
           OR m.sourceMatchB.id = :sourceMatchId
    """)
    List<CricketMatch> findDetailedDependents(
            @Param("sourceMatchId") Long sourceMatchId
    );

    @Modifying
    @Query(value = """
        UPDATE matches
        SET team_a_side_id = NULL,
            team_b_side_id = NULL,
            winner_side_id = NULL
        WHERE (
            source_match_a_id = :sourceMatchId
            OR source_match_b_id = :sourceMatchId
        )
          AND status IN ('PLANNED', 'SCHEDULED', 'READY', 'POSTPONED', 'CANCELLED')
    """, nativeQuery = true)
    int detachUnstartedDependents(
            @Param("sourceMatchId") Long sourceMatchId
    );

    @Modifying
    @Query(value = """
        DELETE FROM matches
        WHERE (
            source_match_a_id = :sourceMatchId
            OR source_match_b_id = :sourceMatchId
        )
          AND status IN ('PLANNED', 'SCHEDULED', 'READY', 'POSTPONED', 'CANCELLED')
    """, nativeQuery = true)
    int deleteUnstartedDependents(
            @Param("sourceMatchId") Long sourceMatchId
    );

    @Query("""
        SELECT DISTINCT m
        FROM MatchScorer ms
        JOIN ms.match m
        LEFT JOIN FETCH m.tournamentEdition
        LEFT JOIN FETCH m.teamA a
        LEFT JOIN FETCH a.team
        LEFT JOIN FETCH m.teamB b
        LEFT JOIN FETCH b.team
        LEFT JOIN FETCH m.teamASide asd
        LEFT JOIN FETCH asd.tournamentTeam astt
        LEFT JOIN FETCH astt.team
        LEFT JOIN FETCH m.teamBSide bsd
        LEFT JOIN FETCH bsd.tournamentTeam bstt
        LEFT JOIN FETCH bstt.team
        LEFT JOIN FETCH m.venue
        WHERE ms.user.id = :userId
        ORDER BY m.scheduledAt NULLS LAST, m.matchNumber
    """)
    List<CricketMatch> findDetailedAssignedToScorer(
            @Param("userId") Long userId
    );

    @Query("""
        SELECT m
        FROM CricketMatch m
        JOIN FETCH m.tournamentEdition
        JOIN FETCH m.teamA a
        JOIN FETCH a.team
        JOIN FETCH m.teamB b
        JOIN FETCH b.team
        LEFT JOIN FETCH m.teamASide asd
        LEFT JOIN FETCH asd.tournamentTeam astt
        LEFT JOIN FETCH astt.team
        LEFT JOIN FETCH m.teamBSide bsd
        LEFT JOIN FETCH bsd.tournamentTeam bstt
        LEFT JOIN FETCH bstt.team
        LEFT JOIN FETCH m.venue
        LEFT JOIN FETCH m.winnerTeam w
        LEFT JOIN FETCH w.team
        LEFT JOIN FETCH m.winnerSide ws
        WHERE m.tournamentEdition.id = :editionId
          AND (
            m.teamA.id = :tournamentTeamId
            OR m.teamB.id = :tournamentTeamId
          )
        ORDER BY m.scheduledAt NULLS LAST, m.matchNumber
    """)
    List<CricketMatch> findDetailedByEditionAndTeamId(
            @Param("editionId") Long editionId,
            @Param("tournamentTeamId") Long tournamentTeamId
    );

    @Query("""
        SELECT m
        FROM CricketMatch m
        JOIN FETCH m.teamA a
        JOIN FETCH a.team
        JOIN FETCH m.teamB b
        JOIN FETCH b.team
        LEFT JOIN FETCH m.teamASide asd
        LEFT JOIN FETCH asd.tournamentTeam astt
        LEFT JOIN FETCH astt.team
        LEFT JOIN FETCH m.teamBSide bsd
        LEFT JOIN FETCH bsd.tournamentTeam bstt
        LEFT JOIN FETCH bstt.team
        LEFT JOIN FETCH m.winnerTeam w
        LEFT JOIN FETCH w.team
        LEFT JOIN FETCH m.winnerSide ws
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
      AND m.status = com.eidcricketfest.match.entity.MatchStatus.COMPLETED
      AND m.matchType = com.eidcricketfest.match.entity.MatchType.TOURNAMENT
      AND m.resultStatus = com.eidcricketfest.match.entity.MatchResultStatus.OFFICIAL
    ORDER BY m.matchNumber
""")
    List<CricketMatch> findLeagueResults(
            @Param("editionId") Long editionId
    );

    @Query("""
        SELECT m
        FROM CricketMatch m
        LEFT JOIN FETCH m.teamASide asd
        LEFT JOIN FETCH asd.tournamentTeam astt
        LEFT JOIN FETCH astt.team
        LEFT JOIN FETCH m.teamBSide bsd
        LEFT JOIN FETCH bsd.tournamentTeam bstt
        LEFT JOIN FETCH bstt.team
        LEFT JOIN FETCH m.venue
        WHERE m.matchType = com.eidcricketfest.match.entity.MatchType.FRIENDLY
        ORDER BY m.scheduledAt NULLS LAST, m.createdAt DESC
    """)
    List<CricketMatch> findDetailedFriendlyMatches();

    @Query("""
        SELECT DISTINCT m
        FROM CricketMatch m
        LEFT JOIN FETCH m.tournamentEdition
        LEFT JOIN FETCH m.teamA a
        LEFT JOIN FETCH a.team
        LEFT JOIN FETCH m.teamB b
        LEFT JOIN FETCH b.team
        LEFT JOIN FETCH m.teamASide asd
        LEFT JOIN FETCH asd.tournamentTeam astt
        LEFT JOIN FETCH astt.team
        LEFT JOIN FETCH m.teamBSide bsd
        LEFT JOIN FETCH bsd.tournamentTeam bstt
        LEFT JOIN FETCH bstt.team
        LEFT JOIN FETCH m.venue
        LEFT JOIN FETCH m.winnerSide ws
        LEFT JOIN FETCH ws.tournamentTeam wst
        LEFT JOIN FETCH wst.team
        LEFT JOIN FETCH m.rematchOfMatch
        LEFT JOIN FETCH m.supersededByMatch
        WHERE m.status IN (
            com.eidcricketfest.match.entity.MatchStatus.TOSS_COMPLETED,
            com.eidcricketfest.match.entity.MatchStatus.LIVE,
            com.eidcricketfest.match.entity.MatchStatus.INNINGS_BREAK,
            com.eidcricketfest.match.entity.MatchStatus.SUSPENDED
        )
        OR (
            m.status = com.eidcricketfest.match.entity.MatchStatus.COMPLETED
            AND m.actualEndedAt >= :completedSince
        )
        ORDER BY m.scheduledAt DESC NULLS LAST, m.matchNumber DESC
    """)
    List<CricketMatch> findLiveCentreMatches(
            @Param("completedSince") Instant completedSince
    );
}
