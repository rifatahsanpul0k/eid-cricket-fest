package com.eidcricketfest.team.repository;

import com.eidcricketfest.team.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeamRosterEntryRepository
        extends JpaRepository<TeamRosterEntry, Long> {

    Optional<TeamRosterEntry>
    findByTournamentEdition_IdAndPlayerRegistration_IdAndStatus(
            Long editionId,
            Long registrationId,
            RosterEntryStatus status
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
