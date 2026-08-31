package com.eidcricketfest.team.service;

import com.eidcricketfest.common.exception.*;
import com.eidcricketfest.registration.entity.*;
import com.eidcricketfest.registration.repository.PlayerRegistrationRepository;
import com.eidcricketfest.team.dto.*;
import com.eidcricketfest.team.entity.*;
import com.eidcricketfest.team.repository.*;
import com.eidcricketfest.tournament.entity.TournamentEdition;
import com.eidcricketfest.tournament.repository.TournamentEditionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TeamService {

    private final TeamRepository teamRepository;
    private final TournamentTeamRepository tournamentTeamRepository;
    private final TeamRosterEntryRepository rosterRepository;
    private final TournamentEditionRepository editionRepository;
    private final PlayerRegistrationRepository registrationRepository;

    public TeamService(
            TeamRepository teamRepository,
            TournamentTeamRepository tournamentTeamRepository,
            TeamRosterEntryRepository rosterRepository,
            TournamentEditionRepository editionRepository,
            PlayerRegistrationRepository registrationRepository
    ) {
        this.teamRepository = teamRepository;
        this.tournamentTeamRepository = tournamentTeamRepository;
        this.rosterRepository = rosterRepository;
        this.editionRepository = editionRepository;
        this.registrationRepository = registrationRepository;
    }

    public TeamResponse createTeam(CreateTeamRequest request) {

        String name = request.name().trim();

        if (teamRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException(
                    "Team already exists: " + name
            );
        }

        Team team = new Team(
                name,
                request.shortName(),
                request.logoUrl()
        );

        return toTeamResponse(teamRepository.save(team));
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> getTeams() {

        return teamRepository.findAll()
                .stream()
                .map(this::toTeamResponse)
                .toList();
    }

    public TournamentTeamResponse addTeamToEdition(
            Long editionId,
            Long teamId
    ) {

        TournamentEdition edition =
                editionRepository.findById(editionId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Tournament edition not found"
                                )
                        );

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Team not found"
                        )
                );

        if (tournamentTeamRepository
                .existsByTournamentEdition_IdAndTeam_Id(
                        editionId,
                        teamId
                )) {

            throw new ConflictException(
                    "Team already participates in this edition"
            );
        }

        TournamentTeam tournamentTeam =
                new TournamentTeam(edition, team);

        return toTournamentTeamResponse(
                tournamentTeamRepository.save(tournamentTeam)
        );
    }

    @Transactional(readOnly = true)
    public List<TournamentTeamResponse> getEditionTeams(
            Long editionId
    ) {

        if (!editionRepository.existsById(editionId)) {
            throw new ResourceNotFoundException(
                    "Tournament edition not found"
            );
        }

        return tournamentTeamRepository
                .findDetailedByEditionId(editionId)
                .stream()
                .map(this::toTournamentTeamResponse)
                .toList();
    }

    public TournamentTeamResponse assignCaptain(
            Long tournamentTeamId,
            AssignCaptainRequest request
    ) {

        TournamentTeam team =
                tournamentTeamRepository
                        .findDetailedById(tournamentTeamId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Tournament team not found"
                                )
                        );

        PlayerRegistration registration =
                registrationRepository
                        .findDetailedById(request.registrationId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Player registration not found"
                                )
                        );

        if (!registration.getTournamentEdition()
                .getId()
                .equals(team.getTournamentEdition().getId())) {

            throw new ConflictException(
                    "Captain must belong to the same tournament edition"
            );
        }

        if (registration.getStatus()
                != RegistrationStatus.APPROVED) {

            throw new ConflictException(
                    "Captain must have an approved registration"
            );
        }

        var existingRoster =
                rosterRepository
                        .findByTournamentEdition_IdAndPlayerRegistration_IdAndStatus(
                                team.getTournamentEdition().getId(),
                                registration.getId(),
                                RosterEntryStatus.ACTIVE
                        );

        if (existingRoster.isPresent()
                && !existingRoster.get()
                .getTournamentTeam()
                .getId()
                .equals(team.getId())) {

            throw new ConflictException(
                    "Player already belongs to another team"
            );
        }

        team.assignCaptain(registration);

        if (existingRoster.isEmpty()) {

            TeamRosterEntry rosterEntry =
                    new TeamRosterEntry(
                            team.getTournamentEdition(),
                            team,
                            registration,
                            AcquisitionType.CAPTAIN
                    );

            rosterRepository.save(rosterEntry);
        }

        return toTournamentTeamResponse(team);
    }

    private TeamResponse toTeamResponse(Team team) {

        return new TeamResponse(
                team.getId(),
                team.getName(),
                team.getShortName(),
                team.getLogoUrl()
        );
    }

    private TournamentTeamResponse toTournamentTeamResponse(
            TournamentTeam tournamentTeam
    ) {

        PlayerRegistration captain =
                tournamentTeam.getCaptainRegistration();

        TournamentTeamResponse.CaptainInfo captainInfo = null;

        if (captain != null) {
            captainInfo =
                    new TournamentTeamResponse.CaptainInfo(
                            captain.getId(),
                            captain.getPlayer().getId(),
                            captain.getPlayer().getFullName()
                    );
        }

        return new TournamentTeamResponse(
                tournamentTeam.getId(),
                tournamentTeam.getTournamentEdition().getId(),

                tournamentTeam.getTeam().getId(),
                tournamentTeam.getTeam().getName(),
                tournamentTeam.getTeam().getShortName(),

                captainInfo,

                tournamentTeam.getRosterStatus()
        );
    }
}
