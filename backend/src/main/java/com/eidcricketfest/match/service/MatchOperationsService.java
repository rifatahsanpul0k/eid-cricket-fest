package com.eidcricketfest.match.service;

import com.eidcricketfest.auth.entity.User;
import com.eidcricketfest.auth.repository.UserRepository;
import com.eidcricketfest.common.exception.*;
import com.eidcricketfest.match.dto.*;
import com.eidcricketfest.match.entity.*;
import com.eidcricketfest.match.repository.*;
import com.eidcricketfest.scoring.repository.InningsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class MatchOperationsService {

    private final CricketMatchRepository matchRepository;
    private final VenueRepository venueRepository;
    private final MatchSideRepository matchSideRepository;
    private final MatchTossRepository tossRepository;
    private final InningsRepository inningsRepository;
    private final MatchOperationAuditRepository auditRepository;
    private final UserRepository userRepository;

    public MatchOperationsService(
            CricketMatchRepository matchRepository,
            VenueRepository venueRepository,
            MatchSideRepository matchSideRepository,
            MatchTossRepository tossRepository,
            InningsRepository inningsRepository,
            MatchOperationAuditRepository auditRepository,
            UserRepository userRepository
    ) {
        this.matchRepository = matchRepository;
        this.venueRepository = venueRepository;
        this.matchSideRepository = matchSideRepository;
        this.tossRepository = tossRepository;
        this.inningsRepository = inningsRepository;
        this.auditRepository = auditRepository;
        this.userRepository = userRepository;
    }

    public Long reschedule(
            Long matchId,
            Long actorUserId,
            RescheduleMatchOperationRequest request
    ) {
        CricketMatch match = findMatch(matchId);
        Venue venue = findVenue(request.venueId());

        return mutate(
                match,
                actorUserId,
                MatchOperationType.RESCHEDULE,
                request.reason(),
                "scheduledAt=" + request.scheduledAt()
                        + ",venueId=" + venue.getId()
                        + ",oversPerInnings=" + request.oversPerInnings(),
                () -> match.reschedule(
                        request.scheduledAt(),
                        venue,
                        request.oversPerInnings()
                )
        );
    }

    public Long postpone(
            Long matchId,
            Long actorUserId,
            MatchOperationReasonRequest request
    ) {
        CricketMatch match = findMatch(matchId);

        return mutate(
                match,
                actorUserId,
                MatchOperationType.POSTPONE,
                request.reason(),
                null,
                match::postpone
        );
    }

    public Long suspend(
            Long matchId,
            Long actorUserId,
            MatchOperationReasonRequest request
    ) {
        CricketMatch match = findMatch(matchId);

        return mutate(
                match,
                actorUserId,
                MatchOperationType.SUSPEND,
                request.reason(),
                null,
                match::suspend
        );
    }

    public Long resume(
            Long matchId,
            Long actorUserId,
            MatchOperationReasonRequest request
    ) {
        CricketMatch match = findMatch(matchId);

        return mutate(
                match,
                actorUserId,
                MatchOperationType.RESUME,
                request.reason(),
                null,
                match::resume
        );
    }

    public Long abandon(
            Long matchId,
            Long actorUserId,
            MatchOperationReasonRequest request
    ) {
        CricketMatch match = findMatch(matchId);

        return mutate(
                match,
                actorUserId,
                MatchOperationType.ABANDON,
                request.reason(),
                null,
                () -> match.abandon(request.reason().trim())
        );
    }

    public Long cancel(
            Long matchId,
            Long actorUserId,
            MatchOperationReasonRequest request
    ) {
        CricketMatch match = findMatch(matchId);
        assertNoInnings(match);

        return mutate(
                match,
                actorUserId,
                MatchOperationType.CANCEL,
                request.reason(),
                null,
                match::cancel
        );
    }

    public Long resetToss(
            Long matchId,
            Long actorUserId,
            MatchOperationReasonRequest request
    ) {
        CricketMatch match = findMatch(matchId);
        assertNoInnings(match);

        return mutate(
                match,
                actorUserId,
                MatchOperationType.RESET_TOSS,
                request.reason(),
                null,
                () -> {
                    match.resetToss();
                    tossRepository
                            .findByMatch_Id(matchId)
                            .ifPresent(tossRepository::delete);
                }
        );
    }

    public Long markUnderReview(
            Long matchId,
            Long actorUserId,
            MatchOperationReasonRequest request
    ) {
        CricketMatch match = findMatch(matchId);

        return mutate(
                match,
                actorUserId,
                MatchOperationType.MARK_UNDER_REVIEW,
                request.reason(),
                null,
                match::markResultUnderReview
        );
    }

    public Long restoreOfficial(
            Long matchId,
            Long actorUserId,
            MatchOperationReasonRequest request
    ) {
        CricketMatch match = findMatch(matchId);

        return mutate(
                match,
                actorUserId,
                MatchOperationType.RESTORE_OFFICIAL,
                request.reason(),
                null,
                match::restoreOfficialResult
        );
    }

    public Long voidResult(
            Long matchId,
            Long actorUserId,
            MatchOperationReasonRequest request
    ) {
        CricketMatch match = findMatch(matchId);
        assertNoStartedDependents(match);

        return mutate(
                match,
                actorUserId,
                MatchOperationType.VOID_RESULT,
                request.reason(),
                null,
                () -> match.voidResult(request.reason().trim())
        );
    }

    public Long orderRematch(
            Long matchId,
            Long actorUserId,
            OrderRematchRequest request
    ) {
        CricketMatch original = findMatch(matchId);
        assertNoStartedDependents(original);

        if (original.getStatus() != MatchStatus.COMPLETED) {
            throw new ConflictException(
                    "Only completed matches can have a rematch ordered"
            );
        }

        Venue venue = request.venueId() != null
                ? findVenue(request.venueId())
                : original.getVenue();

        CricketMatch rematch =
                new CricketMatch(
                        original.requireTournamentEdition(),
                        original.getTeamA(),
                        original.getTeamB(),
                        original.getStage(),
                        original.getRoundNumber(),
                        matchRepository.findMaxMatchNumber(
                                original.requireTournamentEdition()
                                        .getId()
                        ) + 1,
                        request.oversPerInnings() != null
                                ? request.oversPerInnings()
                                : original.getOversPerInnings(),
                        venue
                );

        if (request.scheduledAt() != null) {
            rematch.schedule(
                    request.scheduledAt(),
                    venue
            );
        }

        rematch.markRematchOf(original);
        CricketMatch savedRematch = matchRepository.save(rematch);
        attachTournamentSides(savedRematch);

        mutate(
                original,
                actorUserId,
                MatchOperationType.ORDER_REMATCH,
                request.reason(),
                "rematchId=" + savedRematch.getId(),
                () -> original.markSupersededBy(savedRematch),
                savedRematch
        );

        return savedRematch.getId();
    }

    private Long mutate(
            CricketMatch match,
            Long actorUserId,
            MatchOperationType type,
            String reason,
            String metadata,
            Runnable action
    ) {
        return mutate(
                match,
                actorUserId,
                type,
                reason,
                metadata,
                action,
                null
        );
    }

    private Long mutate(
            CricketMatch match,
            Long actorUserId,
            MatchOperationType type,
            String reason,
            String metadata,
            Runnable action,
            CricketMatch relatedMatch
    ) {
        User actor = findUser(actorUserId);
        String trimmedReason = trimReason(reason);

        MatchStatus oldStatus = match.getStatus();
        MatchResultStatus oldResultStatus = match.getResultStatus();

        try {
            action.run();
        } catch (IllegalStateException ex) {
            throw new ConflictException(ex.getMessage());
        }

        auditRepository.save(
                new MatchOperationAudit(
                        match,
                        type,
                        actor,
                        trimmedReason,
                        oldStatus,
                        match.getStatus(),
                        oldResultStatus,
                        match.getResultStatus(),
                        metadata,
                        relatedMatch
                )
        );

        return match.getId();
    }

    private CricketMatch findMatch(Long matchId) {
        return matchRepository
                .findDetailedById(matchId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Match not found"
                        )
                );
    }

    private Venue findVenue(Long venueId) {
        return venueRepository
                .findById(venueId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Venue not found"
                        )
                );
    }

    private User findUser(Long userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );
    }

    private String trimReason(String reason) {
        String trimmed = reason == null ? "" : reason.trim();

        if (trimmed.isEmpty()) {
            throw new ConflictException("Reason is required");
        }

        return trimmed;
    }

    private void assertNoInnings(CricketMatch match) {
        if (inningsRepository.existsByMatch_Id(match.getId())) {
            throw new ConflictException(
                    "Operation is not allowed after innings have started"
            );
        }
    }

    private void assertNoStartedDependents(CricketMatch match) {
        List<CricketMatch> dependents =
                matchRepository.findDetailedDependents(match.getId());

        boolean started =
                dependents.stream()
                        .anyMatch(dependent ->
                                dependent.getStatus() == MatchStatus.TOSS_COMPLETED
                                || dependent.getStatus() == MatchStatus.LIVE
                                || dependent.getStatus() == MatchStatus.INNINGS_BREAK
                                || dependent.getStatus() == MatchStatus.SUSPENDED
                                || dependent.getStatus() == MatchStatus.COMPLETED
                        );

        if (started) {
            throw new ConflictException(
                    "Operation is blocked because a downstream knockout match has started"
            );
        }
    }

    private void attachTournamentSides(CricketMatch match) {
        MatchSide sideA =
                matchSideRepository.save(
                        new MatchSide(
                                match,
                                MatchSideKey.A,
                                match.getTeamA().getTeam().getName(),
                                match.getTeamA()
                        )
                );

        MatchSide sideB =
                matchSideRepository.save(
                        new MatchSide(
                                match,
                                MatchSideKey.B,
                                match.getTeamB().getTeam().getName(),
                                match.getTeamB()
                        )
                );

        match.attachSides(sideA, sideB);
    }
}
