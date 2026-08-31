package com.eidcricketfest.draft.service;

import com.eidcricketfest.auth.entity.User;
import com.eidcricketfest.auth.repository.UserRepository;
import com.eidcricketfest.common.exception.*;
import com.eidcricketfest.draft.dto.*;
import com.eidcricketfest.draft.entity.*;
import com.eidcricketfest.draft.repository.*;
import com.eidcricketfest.registration.entity.*;
import com.eidcricketfest.registration.repository.PlayerRegistrationRepository;
import com.eidcricketfest.team.entity.*;
import com.eidcricketfest.team.repository.*;
import com.eidcricketfest.tournament.entity.TournamentEdition;
import com.eidcricketfest.tournament.repository.TournamentEditionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.*;

@Service
@Transactional
public class DraftService {

    private static final SecureRandom RANDOM =
            new SecureRandom();

    private final DraftRepository draftRepository;
    private final DraftOrderRepository orderRepository;
    private final DraftPickRepository pickRepository;

    private final TournamentEditionRepository editionRepository;
    private final TournamentTeamRepository tournamentTeamRepository;

    private final PlayerRegistrationRepository registrationRepository;
    private final TeamRosterEntryRepository rosterRepository;

    private final UserRepository userRepository;

    public DraftService(
            DraftRepository draftRepository,
            DraftOrderRepository orderRepository,
            DraftPickRepository pickRepository,
            TournamentEditionRepository editionRepository,
            TournamentTeamRepository tournamentTeamRepository,
            PlayerRegistrationRepository registrationRepository,
            TeamRosterEntryRepository rosterRepository,
            UserRepository userRepository
    ) {
        this.draftRepository = draftRepository;
        this.orderRepository = orderRepository;
        this.pickRepository = pickRepository;
        this.editionRepository = editionRepository;
        this.tournamentTeamRepository = tournamentTeamRepository;
        this.registrationRepository = registrationRepository;
        this.rosterRepository = rosterRepository;
        this.userRepository = userRepository;
    }

    public DraftStateResponse createDraft(
            Long editionId,
            CreateDraftRequest request
    ) {

        if (draftRepository
                .existsByTournamentEdition_Id(editionId)) {

            throw new ConflictException(
                    "Draft already exists for this tournament edition"
            );
        }

        TournamentEdition edition =
                editionRepository.findById(editionId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Tournament edition not found"
                                )
                        );

        Draft draft = new Draft(
                edition,
                request.pickMode()
        );

        draftRepository.save(draft);

        return toState(draft);
    }

    public DraftStateResponse generateLottery(Long draftId) {

        Draft draft = lockDraft(draftId);

        if (draft.getStatus() != DraftStatus.PENDING) {
            throw new ConflictException(
                    "Lottery can only be generated for a pending draft"
            );
        }

        List<TournamentTeam> teams =
                tournamentTeamRepository
                        .findDetailedByEditionId(
                                draft.getTournamentEdition().getId()
                        );

        if (teams.size() < 2) {
            throw new ConflictException(
                    "At least two teams are required"
            );
        }

        for (TournamentTeam team : teams) {

            if (team.getCaptainRegistration() == null) {
                throw new ConflictException(
                        "Every team must have a captain before the lottery"
                );
            }
        }

        List<TournamentTeam> randomized =
                new ArrayList<>(teams);

        Collections.shuffle(
                randomized,
                RANDOM
        );

        int position = 1;

        for (TournamentTeam team : randomized) {

            orderRepository.save(
                    new DraftOrder(
                            draft,
                            draft.getTournamentEdition(),
                            team,
                            position++
                    )
            );
        }

        draft.markOrderGenerated();

        return toState(draft);
    }

    public DraftStateResponse startDraft(Long draftId) {

        Draft draft = lockDraft(draftId);

        if (draft.getStatus()
                != DraftStatus.ORDER_GENERATED) {

            throw new ConflictException(
                    "Draft lottery must be completed first"
            );
        }

        List<DraftOrder> orders =
                orderRepository
                        .findDetailedByDraftId(draftId);

        if (orders.isEmpty()) {
            throw new ConflictException(
                    "Draft order does not exist"
            );
        }

        /*
         * Verify every captain really exists in the roster.
         */
        for (DraftOrder order : orders) {

            TournamentTeam team =
                    order.getTournamentTeam();

            PlayerRegistration captain =
                    team.getCaptainRegistration();

            if (captain == null) {
                throw new ConflictException(
                        "Every team must have a captain"
                );
            }

            boolean captainInRoster =
                    rosterRepository
                            .findByTournamentEdition_IdAndPlayerRegistration_IdAndStatus(
                                    draft.getTournamentEdition().getId(),
                                    captain.getId(),
                                    RosterEntryStatus.ACTIVE
                            )
                            .isPresent();

            if (!captainInRoster) {
                throw new ConflictException(
                        "Captain is missing from team roster"
                );
            }
        }

        draft.start();

        return toState(draft);
    }

    public DraftPickResponse makePick(
            Long draftId,
            Long actorUserId,
            boolean privileged,
            DraftPickRequest request
    ) {

        /*
         * Locks the draft row.
         * All picks for this draft are serialized.
         */
        Draft draft = lockDraft(draftId);

        if (draft.getStatus()
                != DraftStatus.IN_PROGRESS) {

            throw new ConflictException(
                    "Draft is not currently in progress"
            );
        }

        List<DraftOrder> orders =
                orderRepository
                        .findDetailedByDraftId(draftId);

        long completed =
                pickRepository.countByDraft_Id(draftId);

        long required =
                requiredPickCount(
                        draft,
                        orders.size()
                );

        if (completed >= required) {
            throw new ConflictException(
                    "Draft is already complete"
            );
        }

        Turn turn =
                calculateTurn(
                        draft,
                        orders,
                        completed
                );

        assertCanPick(
                turn.team(),
                actorUserId,
                privileged
        );

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

        if (!registration
                .getTournamentEdition()
                .getId()
                .equals(
                        draft.getTournamentEdition().getId()
                )) {

            throw new ConflictException(
                    "Player belongs to another tournament edition"
            );
        }

        if (registration.getStatus()
                != RegistrationStatus.APPROVED) {

            throw new ConflictException(
                    "Only approved players can be drafted"
            );
        }

        if (pickRepository
                .existsByDraft_IdAndPlayerRegistration_Id(
                        draftId,
                        registration.getId()
                )) {

            throw new ConflictException(
                    "Player has already been drafted"
            );
        }

        boolean alreadyOnRoster =
                rosterRepository
                        .findByTournamentEdition_IdAndPlayerRegistration_IdAndStatus(
                                draft.getTournamentEdition().getId(),
                                registration.getId(),
                                RosterEntryStatus.ACTIVE
                        )
                        .isPresent();

        if (alreadyOnRoster) {
            throw new ConflictException(
                    "Player already belongs to a team"
            );
        }

        User actor =
                userRepository.findById(actorUserId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"
                                )
                        );

        DraftPick pick =
                new DraftPick(
                        draft,
                        draft.getTournamentEdition(),
                        turn.team(),
                        registration,
                        turn.roundNumber(),
                        turn.pickNumber(),
                        actor
                );

        pickRepository.save(pick);

        rosterRepository.save(
                new TeamRosterEntry(
                        draft.getTournamentEdition(),
                        turn.team(),
                        registration,
                        AcquisitionType.DRAFT
                )
        );

        if (completed + 1 >= required) {
            draft.complete();
        }

        return toPickResponse(pick);
    }

    @Transactional(readOnly = true)
    public DraftStateResponse getDraftByEdition(
            Long editionId
    ) {

        Draft draft =
                draftRepository
                        .findByTournamentEdition_Id(editionId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Draft not found"
                                )
                        );

        return toState(draft);
    }

    @Transactional(readOnly = true)
    public List<DraftPickResponse> getPicks(
            Long draftId
    ) {

        return pickRepository
                .findDetailedByDraftId(draftId)
                .stream()
                .map(this::toPickResponse)
                .toList();
    }

    private void assertCanPick(
            TournamentTeam currentTeam,
            Long actorUserId,
            boolean privileged
    ) {

        if (privileged) {
            return;
        }

        PlayerRegistration captain =
                currentTeam.getCaptainRegistration();

        if (captain == null
                || captain.getPlayer().getUser() == null
                || !captain.getPlayer()
                .getUser()
                .getId()
                .equals(actorUserId)) {

            throw new ForbiddenException(
                    "It is not your team's turn"
            );
        }
    }

    private Turn calculateTurn(
            Draft draft,
            List<DraftOrder> orders,
            long completedPicks
    ) {

        int teamCount = orders.size();

        int pickNumber =
                Math.toIntExact(completedPicks + 1);

        int round =
                ((pickNumber - 1) / teamCount) + 1;

        int slot =
                ((pickNumber - 1) % teamCount) + 1;

        int orderPosition;

        if (draft.getPickMode()
                == DraftPickMode.LINEAR
                || round % 2 == 1) {

            orderPosition = slot;

        } else {

            orderPosition =
                    teamCount - slot + 1;
        }

        DraftOrder order =
                orders.stream()
                        .filter(o ->
                                o.getPosition()
                                        == orderPosition
                        )
                        .findFirst()
                        .orElseThrow();

        return new Turn(
                pickNumber,
                round,
                order.getTournamentTeam()
        );
    }

    private long requiredPickCount(
            Draft draft,
            int teamCount
    ) {

        /*
         * Captain is already on each roster.
         *
         * squadSize 11
         * -> captain + 10 draft picks
         */
        return (long) teamCount *
                (draft.getTournamentEdition()
                        .getSquadSize() - 1);
    }

    private Draft lockDraft(Long draftId) {

        return draftRepository
                .findByIdForUpdate(draftId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Draft not found"
                        )
                );
    }

    private DraftStateResponse toState(Draft draft) {

        List<DraftOrder> orders =
                orderRepository
                        .findDetailedByDraftId(
                                draft.getId()
                        );

        long completed =
                pickRepository
                        .countByDraft_Id(
                                draft.getId()
                        );

        long required =
                orders.isEmpty()
                        ? 0
                        : requiredPickCount(
                                draft,
                                orders.size()
                        );

        DraftStateResponse.CurrentTurn current = null;

        if (draft.getStatus()
                == DraftStatus.IN_PROGRESS
                && completed < required) {

            Turn turn =
                    calculateTurn(
                            draft,
                            orders,
                            completed
                    );

            current =
                    new DraftStateResponse.CurrentTurn(
                            turn.pickNumber(),
                            turn.roundNumber(),
                            turn.team().getId(),
                            turn.team().getTeam().getName()
                    );
        }

        List<DraftStateResponse.OrderItem> orderItems =
                orders.stream()
                        .map(order ->
                                new DraftStateResponse.OrderItem(
                                        order.getPosition(),
                                        order.getTournamentTeam().getId(),
                                        order.getTournamentTeam()
                                                .getTeam()
                                                .getName()
                                )
                        )
                        .toList();

        return new DraftStateResponse(
                draft.getId(),
                draft.getTournamentEdition().getId(),
                draft.getStatus(),
                draft.getPickMode(),
                draft.getTournamentEdition().getSquadSize(),
                completed,
                required,
                current,
                orderItems
        );
    }

    private DraftPickResponse toPickResponse(
            DraftPick pick
    ) {

        PlayerRegistration registration =
                pick.getPlayerRegistration();

        return new DraftPickResponse(
                pick.getId(),
                pick.getPickNumber(),
                pick.getRoundNumber(),

                pick.getTournamentTeam().getId(),
                pick.getTournamentTeam()
                        .getTeam()
                        .getName(),

                registration.getId(),
                registration.getPlayer().getId(),
                registration.getPlayer().getFullName(),

                pick.getSelectedAt()
        );
    }

    private record Turn(
            Integer pickNumber,
            Integer roundNumber,
            TournamentTeam team
    ) {}
}
