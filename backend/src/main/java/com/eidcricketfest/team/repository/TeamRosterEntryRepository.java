package com.eidcricketfest.team.repository;

import com.eidcricketfest.team.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeamRosterEntryRepository
        extends JpaRepository<TeamRosterEntry, Long> {

    Optional<TeamRosterEntry>
    findByTournamentEdition_IdAndPlayerRegistration_IdAndStatus(
            Long editionId,
            Long registrationId,
            RosterEntryStatus status
    );

    @Query("""
        SELECT tre
        FROM TeamRosterEntry tre
        JOIN FETCH tre.tournamentTeam tt
        JOIN FETCH tt.team
        JOIN FETCH tt.tournamentEdition
        LEFT JOIN FETCH tt.captainRegistration captain
        LEFT JOIN FETCH captain.player
        JOIN FETCH tre.playerRegistration pr
        JOIN FETCH pr.player p
        LEFT JOIN FETCH p.primaryCategory
        WHERE tt.tournamentEdition.id = :editionId
          AND p.user.id = :userId
          AND tre.status = :status
    """)
    Optional<TeamRosterEntry> findMineDetailedByEditionIdAndUserId(
            @Param("editionId") Long editionId,
            @Param("userId") Long userId,
            @Param("status") RosterEntryStatus status
    );

    @Query("""
        SELECT tre
        FROM TeamRosterEntry tre
        JOIN FETCH tre.tournamentTeam tt
        JOIN FETCH tt.team
        JOIN FETCH tt.tournamentEdition
        LEFT JOIN FETCH tt.captainRegistration captain
        LEFT JOIN FETCH captain.player
        JOIN FETCH tre.playerRegistration pr
        JOIN FETCH pr.player p
        LEFT JOIN FETCH p.primaryCategory
        WHERE tt.id = :tournamentTeamId
          AND tre.status = :status
        ORDER BY
          CASE
            WHEN captain.id IS NOT NULL
             AND captain.id = pr.id THEN 0
            ELSE 1
          END,
          p.fullName
    """)
    List<TeamRosterEntry> findActiveDetailedByTournamentTeamId(
            @Param("tournamentTeamId") Long tournamentTeamId,
            @Param("status") RosterEntryStatus status
    );

    @Query("""
        SELECT tre
        FROM TeamRosterEntry tre
        JOIN FETCH tre.tournamentTeam tt
        JOIN FETCH tt.team
        JOIN FETCH tre.playerRegistration pr
        JOIN FETCH pr.player p
        WHERE tre.tournamentEdition.id = :editionId
          AND tre.status = :status
        ORDER BY tt.team.name, p.fullName
    """)
    List<TeamRosterEntry> findActiveDetailedByEditionId(
            @Param("editionId") Long editionId,
            @Param("status") RosterEntryStatus status
    );

    long countByTournamentTeam_IdAndStatus(
            Long tournamentTeamId,
            RosterEntryStatus status
    );

    boolean existsByTournamentTeam_IdAndPlayerRegistration_IdAndStatus(
            Long tournamentTeamId,
            Long registrationId,
            RosterEntryStatus status
    );
}
