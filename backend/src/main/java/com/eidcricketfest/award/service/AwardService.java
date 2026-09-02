package com.eidcricketfest.award.service;

import com.eidcricketfest.auth.entity.User;
import com.eidcricketfest.auth.repository.UserRepository;
import com.eidcricketfest.award.dto.*;
import com.eidcricketfest.award.entity.*;
import com.eidcricketfest.award.repository.TournamentPlayerAwardRepository;
import com.eidcricketfest.common.exception.*;
import com.eidcricketfest.registration.entity.*;
import com.eidcricketfest.registration.repository.PlayerRegistrationRepository;
import com.eidcricketfest.team.entity.*;
import com.eidcricketfest.team.repository.TeamRosterEntryRepository;
import com.eidcricketfest.tournament.entity.*;
import com.eidcricketfest.tournament.repository.TournamentEditionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class AwardService {

    private final TournamentPlayerAwardRepository awardRepository;
    private final TournamentEditionRepository editionRepository;
    private final PlayerRegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final TeamRosterEntryRepository rosterEntryRepository;

    public AwardService(
            TournamentPlayerAwardRepository awardRepository,
            TournamentEditionRepository editionRepository,
            PlayerRegistrationRepository registrationRepository,
            UserRepository userRepository,
            TeamRosterEntryRepository rosterEntryRepository
    ) {
        this.awardRepository = awardRepository;
        this.editionRepository = editionRepository;
        this.registrationRepository = registrationRepository;
        this.userRepository = userRepository;
        this.rosterEntryRepository = rosterEntryRepository;
    }

    public PlayerAwardResponse assignAward(
            Long editionId,
            Long actorUserId,
            AssignPlayerAwardRequest request
    ) {

        TournamentEdition edition =
                editionRepository.findById(editionId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Tournament edition not found"
                                )
                        );

        if (edition.getStatus()
                != TournamentEditionStatus.COMPLETED) {

            throw new ConflictException(
                    "Awards can be assigned after the tournament is completed"
            );
        }

        PlayerRegistration registration =
                registrationRepository
                        .findDetailedById(
                                request.registrationId()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Player registration not found"
                                )
                        );

        if (!registration.getTournamentEdition()
                .getId()
                .equals(editionId)) {

            throw new ConflictException(
                    "Player belongs to another tournament edition"
            );
        }

        if (registration.getStatus()
                != RegistrationStatus.APPROVED) {

            throw new ConflictException(
                    "Award recipient must have an approved registration"
            );
        }

        if (awardRepository
                .existsByTournamentEdition_IdAndAwardTypeAndPlayerRegistration_Id(
                        editionId,
                        request.awardType(),
                        registration.getId()
                )) {

            throw new ConflictException(
                    "This award is already assigned to this player"
            );
        }

        User actor =
                userRepository.findById(actorUserId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"
                                )
                        );

        TournamentPlayerAward award =
                new TournamentPlayerAward(
                        edition,
                        registration,
                        request.awardType(),

                        request.title() == null
                                ? null
                                : request.title().trim(),

                        request.notes(),
                        actor
                );

        TeamRosterEntry rosterEntry =
                rosterEntryRepository
                        .findByTournamentEdition_IdAndPlayerRegistration_IdAndStatus(
                                editionId,
                                registration.getId(),
                                RosterEntryStatus.ACTIVE
                        )
                        .orElse(null);

        return toResponse(
                awardRepository.save(award),
                rosterEntry
        );
    }

    @Transactional(readOnly = true)
    public List<PlayerAwardResponse> getAwards(
            Long editionId
    ) {

        if (!editionRepository.existsById(editionId)) {
            throw new ResourceNotFoundException(
                    "Tournament edition not found"
            );
        }

        Map<Long, TeamRosterEntry> rosterByRegistration =
                rosterEntryRepository
                        .findActiveDetailedByEditionId(
                                editionId,
                                RosterEntryStatus.ACTIVE
                        )
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        entry -> entry
                                                .getPlayerRegistration()
                                                .getId(),
                                        entry -> entry,
                                        (first, ignored) -> first
                                )
                        );

        return awardRepository
                .findDetailedByEditionId(editionId)
                .stream()
                .map(award ->
                        toResponse(
                                award,
                                rosterByRegistration.get(
                                        award.getPlayerRegistration()
                                                .getId()
                                )
                        )
                )
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AwardPlayerOptionResponse> getPlayerOptions(
            Long editionId
    ) {

        if (!editionRepository.existsById(editionId)) {
            throw new ResourceNotFoundException(
                    "Tournament edition not found"
            );
        }

        return rosterEntryRepository
                .findActiveDetailedByEditionId(
                        editionId,
                        RosterEntryStatus.ACTIVE
                )
                .stream()
                .map(entry -> {
                    PlayerRegistration registration =
                            entry.getPlayerRegistration();

                    return new AwardPlayerOptionResponse(
                            registration.getId(),
                            registration.getPlayer().getId(),
                            registration.getPlayer().getFullName(),
                            entry.getTournamentTeam().getId(),
                            entry.getTournamentTeam()
                                    .getTeam()
                                    .getName()
                    );
                })
                .toList();
    }

    private PlayerAwardResponse toResponse(
            TournamentPlayerAward award
    ) {

        return toResponse(
                award,
                null
        );
    }

    private PlayerAwardResponse toResponse(
            TournamentPlayerAward award,
            TeamRosterEntry rosterEntry
    ) {

        PlayerRegistration registration =
                award.getPlayerRegistration();

        return new PlayerAwardResponse(
                award.getId(),
                award.getAwardType(),
                award.getTitle(),

                registration.getId(),
                registration.getPlayer().getId(),
                registration.getPlayer().getFullName(),
                rosterEntry == null
                        ? null
                        : rosterEntry.getTournamentTeam()
                                .getId(),
                rosterEntry == null
                        ? null
                        : rosterEntry.getTournamentTeam()
                                .getTeam()
                                .getName(),

                award.getNotes()
        );
    }
}
