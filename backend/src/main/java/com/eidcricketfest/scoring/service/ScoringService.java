package com.eidcricketfest.scoring.service;

import com.eidcricketfest.auth.entity.User;
import com.eidcricketfest.auth.repository.UserRepository;
import com.eidcricketfest.common.exception.*;
import com.eidcricketfest.match.entity.*;
import com.eidcricketfest.match.event.MatchCompletedEvent;
import com.eidcricketfest.match.repository.*;
import com.eidcricketfest.scoring.dto.*;
import com.eidcricketfest.scoring.entity.*;
import com.eidcricketfest.scoring.event.MatchScoreChangedEvent;
import com.eidcricketfest.scoring.repository.*;
import com.eidcricketfest.team.entity.TournamentTeam;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ScoringService {

    private final CricketMatchRepository matchRepository;
    private final MatchTossRepository tossRepository;
    private final MatchScorerRepository scorerRepository;

    private final PlayingXiEntryRepository playingXiRepository;

    private final InningsRepository inningsRepository;
    private final DeliveryRepository deliveryRepository;
    private final WicketRepository wicketRepository;

    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ScoringService(
            CricketMatchRepository matchRepository,
            MatchTossRepository tossRepository,
            MatchScorerRepository scorerRepository,
            PlayingXiEntryRepository playingXiRepository,
            InningsRepository inningsRepository,
            DeliveryRepository deliveryRepository,
            WicketRepository wicketRepository,
            UserRepository userRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.matchRepository = matchRepository;
        this.tossRepository = tossRepository;
        this.scorerRepository = scorerRepository;
        this.playingXiRepository = playingXiRepository;
        this.inningsRepository = inningsRepository;
        this.deliveryRepository = deliveryRepository;
        this.wicketRepository = wicketRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    public InningsResponse startInnings(
            Long matchId,
            Long actorUserId,
            boolean privileged,
            StartInningsRequest request
    ) {

        CricketMatch match =
                matchRepository.findDetailedById(matchId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Match not found"
                                )
                        );

        authorizeScorer(
                matchId,
                actorUserId,
                privileged
        );

        MatchToss toss =
                tossRepository.findByMatch_Id(matchId)
                        .orElseThrow(() ->
                                new ConflictException(
                                        "Toss must be recorded first"
                                )
                        );

        boolean firstExists =
                inningsRepository
                        .existsByMatch_IdAndInningsNumber(
                                matchId,
                                (short) 1
                        );

        short inningsNumber;

        TournamentTeam batting;
        TournamentTeam bowling;

        Integer target = null;

        if (!firstExists) {

            if (match.getStatus()
                    != MatchStatus.TOSS_COMPLETED) {

                throw new ConflictException(
                        "Match is not ready to start"
                );
            }

            inningsNumber = 1;

            TournamentTeam tossWinner =
                    toss.getWinnerTeam();

            TournamentTeam other =
                    tossWinner.getId()
                            .equals(match.getTeamA().getId())
                            ? match.getTeamB()
                            : match.getTeamA();

            if (toss.getDecision() == TossDecision.BAT) {
                batting = tossWinner;
                bowling = other;
            } else {
                batting = other;
                bowling = tossWinner;
            }

        } else {

            Innings first =
                    inningsRepository
                            .findByMatch_IdAndInningsNumber(
                                    matchId,
                                    (short) 1
                            )
                            .orElseThrow();

            if (first.getStatus()
                    != InningsStatus.COMPLETED) {

                throw new ConflictException(
                        "First innings has not completed"
                );
            }

            if (inningsRepository
                    .existsByMatch_IdAndInningsNumber(
                            matchId,
                            (short) 2
                    )) {

                throw new ConflictException(
                        "Second innings already exists"
                );
            }

            inningsNumber = 2;

            batting = first.getBowlingTeam();
            bowling = first.getBattingTeam();

            target = first.getTotalRuns() + 1;
        }

        PlayingXiEntry striker =
                findPlayingXi(
                        request.strikerPlayingXiId()
                );

        PlayingXiEntry nonStriker =
                findPlayingXi(
                        request.nonStrikerPlayingXiId()
                );

        PlayingXiEntry bowler =
                findPlayingXi(
                        request.bowlerPlayingXiId()
                );

        validateBatters(
                matchId,
                batting,
                striker,
                nonStriker
        );

        validateBowler(
                matchId,
                bowling,
                bowler
        );

        Innings innings =
                new Innings(
                        match,
                        inningsNumber,
                        batting,
                        bowling,
                        target
                );

        innings.setBatters(
                striker,
                nonStriker
        );

        innings.setBowler(bowler);

        inningsRepository.save(innings);

        match.startMatch();

        publishScoreChange(matchId);

        return toResponse(innings);
    }

    public InningsResponse setBatters(
            Long inningsId,
            Long actorUserId,
            boolean privileged,
            SetBattersRequest request
    ) {

        Innings innings = lockInnings(inningsId);

        authorizeScorer(
                innings.getMatch().getId(),
                actorUserId,
                privileged
        );

        PlayingXiEntry striker =
                findPlayingXi(
                        request.strikerPlayingXiId()
                );

        PlayingXiEntry nonStriker =
                findPlayingXi(
                        request.nonStrikerPlayingXiId()
                );

        validateBatters(
                innings.getMatch().getId(),
                innings.getBattingTeam(),
                striker,
                nonStriker
        );

        if (wicketRepository.isDismissed(
                inningsId,
                striker.getId()
        )) {
            throw new ConflictException(
                    "Striker is already dismissed"
            );
        }

        if (wicketRepository.isDismissed(
                inningsId,
                nonStriker.getId()
        )) {
            throw new ConflictException(
                    "Non-striker is already dismissed"
            );
        }

        innings.setBatters(
                striker,
                nonStriker
        );

        publishScoreChange(
                innings.getMatch().getId()
        );

        return toResponse(innings);
    }

    public InningsResponse setBowler(
            Long inningsId,
            Long actorUserId,
            boolean privileged,
            SetBowlerRequest request
    ) {

        Innings innings = lockInnings(inningsId);

        authorizeScorer(
                innings.getMatch().getId(),
                actorUserId,
                privileged
        );

        PlayingXiEntry bowler =
                findPlayingXi(
                        request.bowlerPlayingXiId()
                );

        validateBowler(
                innings.getMatch().getId(),
                innings.getBowlingTeam(),
                bowler
        );

        innings.setBowler(bowler);

        publishScoreChange(
                innings.getMatch().getId()
        );

        return toResponse(innings);
    }

    public InningsResponse recordDelivery(
            Long inningsId,
            Long actorUserId,
            boolean privileged,
            RecordDeliveryRequest request
    ) {

        Innings innings = lockInnings(inningsId);

        authorizeScorer(
                innings.getMatch().getId(),
                actorUserId,
                privileged
        );

        var existingEvent =
                deliveryRepository
                        .findByClientEventId(
                                request.clientEventId()
                        );

        if (existingEvent.isPresent()) {

            Delivery existing =
                    existingEvent.get();

            if (!existing.getInnings()
                    .getId()
                    .equals(inningsId)) {

                throw new ConflictException(
                        "Client event ID belongs to another innings"
                );
            }

            /*
             * HTTP retry / double click.
             *
             * Do NOT score again.
             */
            return toResponse(innings);
        }

        if (innings.getStatus()
                != InningsStatus.IN_PROGRESS) {

            throw new ConflictException(
                    "Innings has completed"
            );
        }

        if (innings.getCurrentStriker() == null
                || innings.getCurrentNonStriker() == null) {

            throw new ConflictException(
                    "Current batters must be selected"
            );
        }

        if (innings.getCurrentBowler() == null) {

            throw new ConflictException(
                    "Current bowler must be selected"
            );
        }

        validateDelivery(request);

        boolean swapEnds =
                request.swapEnds() != null
                        ? request.swapEnds()
                        : calculateAutomaticSwap(request);

        User scorer =
                userRepository.findById(actorUserId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"
                                )
                        );

        int sequence =
                deliveryRepository
                        .findMaxSequence(inningsId)
                        + 1;

        Delivery delivery =
                new Delivery(
                        innings,
                        sequence,

                        request.clientEventId(),

                        innings.getCurrentStriker(),
                        innings.getCurrentNonStriker(),
                        innings.getCurrentBowler(),
                        request.runsOffBat(),
                        request.wideRuns(),
                        request.noBallRuns(),
                        request.byeRuns(),
                        request.legByeRuns(),
                        request.penaltyRuns(),
                        swapEnds,
                        scorer
                );

        deliveryRepository.save(delivery);

        boolean wicketFell = false;

        if (request.wicket() != null) {

            createWicket(
                    innings,
                    delivery,
                    request
            );

            wicketFell = true;
        }

        innings.applyDelivery(
                delivery,
                wicketFell
        );

        if (shouldComplete(innings)) {

            innings.complete();

            handleInningsCompletion(innings);

            publishScoreChange(
                    innings.getMatch().getId()
            );

            return toResponse(innings);
        }

        if (wicketFell) {

            /*
             * Scorer explicitly chooses the new pair.
             * This avoids incorrect assumptions for run-outs,
             * crossed batters and unusual dismissal situations.
             */
            innings.clearBatters();

        } else {

            if (swapEnds) {
                innings.swapBatters();
            }

            /*
             * After six legal balls the batters change ends.
             */
            if (innings.getLegalBalls() % 6 == 0
                    && delivery.isLegal()) {

                innings.swapBatters();

                /*
                 * New over requires bowler selection.
                 */
                innings.clearBowler();
            }
        }

        publishScoreChange(
                innings.getMatch().getId()
        );

        return toResponse(innings);
    }

    public InningsResponse undoLastDelivery(
            Long inningsId,
            Long actorUserId,
            boolean privileged,
            UndoDeliveryRequest request
    ) {

        Innings innings = lockInnings(inningsId);

        if (innings.getStatus()
                != InningsStatus.IN_PROGRESS) {

            throw new ConflictException(
                    "Completed innings requires formal correction workflow"
            );
        }

        authorizeScorer(
                innings.getMatch().getId(),
                actorUserId,
                privileged
        );

        Delivery delivery =
                deliveryRepository
                        .findFirstByInnings_IdAndVoidedAtIsNullOrderBySequenceNoDesc(
                                inningsId
                        )
                        .orElseThrow(() ->
                                new ConflictException(
                                        "No delivery exists to undo"
                                )
                        );

        boolean wicket =
                wicketRepository
                        .findByDelivery_Id(
                                delivery.getId()
                        )
                        .isPresent();

        User actor =
                userRepository.findById(actorUserId)
                        .orElseThrow();

        innings.rollbackDelivery(
                delivery,
                wicket
        );

        delivery.voidDelivery(
                actor,
                request.reason().trim()
        );

        publishScoreChange(
                innings.getMatch().getId()
        );

        return toResponse(innings);
    }

    public InningsResponse correctDelivery(
            Long deliveryId,
            Long actorUserId,
            boolean privileged,
            CorrectDeliveryRequest request
    ) {

        var existingCorrection =
                deliveryRepository
                        .findByClientEventId(
                                request.clientEventId()
                        );

        if (existingCorrection.isPresent()) {

            Delivery existing =
                    existingCorrection.get();

            Delivery correctedFrom =
                    existing.getCorrectionOf();

            if (correctedFrom == null
                    || !correctedFrom.getId()
                            .equals(deliveryId)) {

                throw new ConflictException(
                        "Client event ID belongs to another operation"
                );
            }

            Innings innings =
                    lockInnings(
                            existing.getInnings().getId()
                    );

            authorizeScorer(
                    innings.getMatch().getId(),
                    actorUserId,
                    privileged
            );

            return toResponse(innings);
        }

        Delivery original =
                deliveryRepository
                        .findActiveDetailedById(deliveryId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Active delivery not found"
                                )
                        );

        Innings innings =
                lockInnings(
                        original.getInnings().getId()
                );

        if (innings.getStatus()
                != InningsStatus.IN_PROGRESS) {

            throw new ConflictException(
                    "Completed innings requires match correction workflow"
            );
        }

        authorizeScorer(
                innings.getMatch().getId(),
                actorUserId,
                privileged
        );

        RecordDeliveryRequest corrected =
                request.asDeliveryRequest();

        validateDelivery(corrected);

        User actor =
                userRepository.findById(actorUserId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"
                                )
                        );

        /*
         * Keep the old delivery as audit history.
         */
        original.voidDelivery(
                actor,
                request.reason().trim()
        );

        /*
         * Important because the partial unique index must see
         * the original as voided before inserting its replacement.
         */
        deliveryRepository.flush();

        boolean swap =
                corrected.swapEnds() != null
                        ? corrected.swapEnds()
                        : calculateAutomaticSwap(corrected);

        boolean laterDeliveriesExist =
                deliveryRepository
                        .existsByInnings_IdAndSequenceNoGreaterThanAndVoidedAtIsNull(
                                innings.getId(),
                                original.getSequenceNo()
                        );

        Wicket existingWicket =
                wicketRepository
                        .findByDelivery_Id(
                                original.getId()
                        )
                        .orElse(null);

        WicketRequest correctedWicket =
                request.wicket();

        boolean wicketStateChanged;

        if (existingWicket == null
                && correctedWicket == null) {

            wicketStateChanged = false;

        } else if (existingWicket == null
                || correctedWicket == null) {

            wicketStateChanged = true;

        } else {

            wicketStateChanged =
                    !existingWicket
                            .getDismissedPlayer()
                            .getId()
                            .equals(
                                    correctedWicket
                                            .dismissedPlayingXiId()
                            );
        }

        int correctedTotal =
                corrected.runsOffBat()
                + corrected.wideRuns()
                + corrected.noBallRuns()
                + corrected.byeRuns()
                + corrected.legByeRuns()
                + corrected.penaltyRuns();

        boolean correctedLegal =
                corrected.wideRuns() == 0
                && corrected.noBallRuns() == 0;

        boolean gameStateChanged =
                original.calculateTotalRuns()
                        != correctedTotal

                || original.isLegal()
                        != correctedLegal

                || original.isSwapEnds()
                        != swap

                || wicketStateChanged;

        if (laterDeliveriesExist
                && gameStateChanged) {

            throw new ConflictException(
                    "This correction changes match state. Undo later deliveries first."
            );
        }

        Delivery replacement =
                new Delivery(
                        innings,
                        original.getSequenceNo(),

                        request.clientEventId(),

                        original.getStriker(),
                        original.getNonStriker(),
                        original.getBowler(),

                        corrected.runsOffBat(),
                        corrected.wideRuns(),
                        corrected.noBallRuns(),
                        corrected.byeRuns(),
                        corrected.legByeRuns(),
                        corrected.penaltyRuns(),

                        swap,

                        actor,
                        original,
                        request.commentary()
                );

        deliveryRepository.save(replacement);

        if (corrected.wicket() != null) {

            createWicket(
                    innings,
                    replacement,
                    corrected
            );
        }

        /*
         * Recalculate from all ACTIVE deliveries.
         */
        recalculateInnings(innings);

        publishScoreChange(
                innings.getMatch().getId()
        );

        return toResponse(innings);
    }

    private void recalculateInnings(
            Innings innings
    ) {

        List<Delivery> deliveries =
                deliveryRepository
                        .findActiveDeliveries(
                                innings.getId()
                        );

        List<Wicket> wickets =
                wicketRepository
                        .findActiveByInningsId(
                                innings.getId()
                        );

        int runs = 0;
        int legalBalls = 0;

        int wides = 0;
        int noBalls = 0;
        int byes = 0;
        int legByes = 0;
        int penalties = 0;

        for (Delivery delivery : deliveries) {

            runs += delivery.calculateTotalRuns();

            wides += delivery.getWideRuns();
            noBalls += delivery.getNoBallRuns();
            byes += delivery.getByeRuns();
            legByes += delivery.getLegByeRuns();
            penalties += delivery.getPenaltyRuns();

            if (delivery.isLegal()) {
                legalBalls++;
            }
        }

        innings.replaceAggregates(
                runs,
                wickets.size(),
                legalBalls,
                wides,
                noBalls,
                byes,
                legByes,
                penalties
        );

        if (deliveries.isEmpty()) {

            innings.clearBatters();
            innings.clearBowler();

            return;
        }

        Delivery last =
                deliveries.get(deliveries.size() - 1);

        boolean lastWasWicket =
                wicketRepository
                        .findByDelivery_Id(last.getId())
                        .isPresent();

        if (lastWasWicket) {

            innings.clearBatters();

        } else {

            innings.setBatters(
                    last.getStriker(),
                    last.getNonStriker()
            );

            if (last.isSwapEnds()) {
                innings.swapBatters();
            }

            if (last.isLegal()
                    && legalBalls % 6 == 0) {

                innings.swapBatters();
                innings.clearBowler();

            } else {

                innings.setBowler(
                        last.getBowler()
                );
            }
        }
    }

    private void validateDelivery(
            RecordDeliveryRequest request
    ) {

        if (request.wideRuns() > 0
                && request.noBallRuns() > 0) {

            throw new ConflictException(
                    "A delivery cannot be both Wide and No-ball"
            );
        }

        if (request.byeRuns() > 0
                && request.legByeRuns() > 0) {

            throw new ConflictException(
                    "A delivery cannot contain both byes and leg-byes"
            );
        }

        if ((request.byeRuns() > 0
                || request.legByeRuns() > 0)
                && request.runsOffBat() > 0) {

            throw new ConflictException(
                    "Bat runs cannot coexist with byes or leg-byes"
            );
        }

        if (request.wideRuns() > 0
                && (
                    request.runsOffBat() > 0
                    || request.byeRuns() > 0
                    || request.legByeRuns() > 0
                )) {

            throw new ConflictException(
                    "All runs from a Wide must be recorded as wide runs"
            );
        }
    }

    private void createWicket(
            Innings innings,
            Delivery delivery,
            RecordDeliveryRequest request
    ) {

        WicketRequest wicketRequest =
                request.wicket();

        PlayingXiEntry dismissed =
                findPlayingXi(
                        wicketRequest.dismissedPlayingXiId()
                );

        boolean currentBatter =
                dismissed.getId().equals(
                        innings.getCurrentStriker().getId()
                )
                ||
                dismissed.getId().equals(
                        innings.getCurrentNonStriker().getId()
                );

        if (!currentBatter) {

            throw new ConflictException(
                    "Dismissed player must be one of the current batters"
            );
        }

        validateDismissalForDelivery(
                request,
                wicketRequest.dismissalType()
        );

        PlayingXiEntry fielder = null;

        if (wicketRequest.fielderPlayingXiId() != null) {

            fielder =
                    findPlayingXi(
                            wicketRequest
                                    .fielderPlayingXiId()
                    );

            validateBowler(
                    innings.getMatch().getId(),
                    innings.getBowlingTeam(),
                    fielder
            );
        }

        wicketRepository.save(
                new Wicket(
                        delivery,
                        dismissed,
                        wicketRequest.dismissalType(),
                        fielder
                )
        );
    }

    private void validateDismissalForDelivery(
            RecordDeliveryRequest request,
            DismissalType type
    ) {

        if (request.noBallRuns() > 0) {

            boolean allowed =
                    type == DismissalType.RUN_OUT
                    || type == DismissalType.OBSTRUCTING_FIELD
                    || type == DismissalType.HIT_BALL_TWICE;

            if (!allowed) {
                throw new ConflictException(
                        "This dismissal is not valid from a No-ball"
                );
            }
        }

        if (request.wideRuns() > 0) {

            boolean allowed =
                    type == DismissalType.RUN_OUT
                    || type == DismissalType.STUMPED
                    || type == DismissalType.HIT_WICKET
                    || type == DismissalType.OBSTRUCTING_FIELD;

            if (!allowed) {
                throw new ConflictException(
                        "This dismissal is not valid from a Wide"
                );
            }
        }
    }

    private boolean calculateAutomaticSwap(
            RecordDeliveryRequest request
    ) {

        int runningRuns = 0;

        /*
         * 4 and 6 are assumed boundaries.
         *
         * Scorer can explicitly override swapEnds
         * for overthrows or unusual situations.
         */
        if (request.runsOffBat() != 4
                && request.runsOffBat() != 6) {

            runningRuns += request.runsOffBat();
        }

        runningRuns += request.byeRuns();
        runningRuns += request.legByeRuns();

        if (request.wideRuns() > 1) {

            /*
             * First wide run is the penalty.
             * Additional runs imply completed running/boundary.
             */
            runningRuns += request.wideRuns() - 1;
        }

        return runningRuns % 2 == 1;
    }

    private boolean shouldComplete(Innings innings) {

        CricketMatch match =
                innings.getMatch();

        int maxBalls =
                match.getOversPerInnings() * 6;

        int maxWickets =
                match.getTournamentEdition()
                        .getPlayingXiSize()
                        - 1;

        if (innings.getLegalBalls() >= maxBalls) {
            return true;
        }

        if (innings.getWickets() >= maxWickets) {
            return true;
        }

        return innings.getTargetRuns() != null
                && innings.getTotalRuns()
                >= innings.getTargetRuns();
    }

    private void handleInningsCompletion(
            Innings innings
    ) {

        CricketMatch match = innings.getMatch();

        if (innings.getInningsNumber() == 1) {

            match.markInningsBreak();
            return;
        }

        Innings first =
                inningsRepository
                        .findByMatch_IdAndInningsNumber(
                                match.getId(),
                                (short) 1
                        )
                        .orElseThrow();

        Innings second = innings;

        if (second.getTotalRuns()
                > first.getTotalRuns()) {

            int maxWickets =
                    match.getTournamentEdition()
                            .getPlayingXiSize()
                            - 1;

            int remaining =
                    maxWickets
                            - second.getWickets();

            match.complete(
                    second.getBattingTeam(),
                    MatchResultType.WICKETS,
                    remaining,
                    second.getBattingTeam()
                            .getTeam()
                            .getName()
                            + " won by "
                            + remaining
                            + " wickets"
            );

        } else if (
                first.getTotalRuns()
                        > second.getTotalRuns()
        ) {

            int margin =
                    first.getTotalRuns()
                            - second.getTotalRuns();

            match.complete(
                    first.getBattingTeam(),
                    MatchResultType.RUNS,
                    margin,
                    first.getBattingTeam()
                            .getTeam()
                            .getName()
                            + " won by "
                            + margin
                            + " runs"
            );

        } else {

            match.complete(
                    null,
                    MatchResultType.TIE,
                    0,
                    "Match tied"
            );
        }

        eventPublisher.publishEvent(
                new MatchCompletedEvent(
                        match.getId(),
                        match.getTournamentEdition()
                                .getId(),
                        match.getStage()
                )
        );
    }

    private Innings lockInnings(Long id) {

        return inningsRepository
                .findByIdForUpdate(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Innings not found"
                        )
                );
    }

    private PlayingXiEntry findPlayingXi(Long id) {

        return playingXiRepository
                .findDetailedById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Playing XI player not found"
                        )
                );
    }

    private void validateBatters(
            Long matchId,
            TournamentTeam battingTeam,
            PlayingXiEntry striker,
            PlayingXiEntry nonStriker
    ) {

        if (striker.getId()
                .equals(nonStriker.getId())) {

            throw new ConflictException(
                    "Striker and non-striker must be different players"
            );
        }

        if (!striker.getMatch()
                .getId()
                .equals(matchId)
                ||
                !nonStriker.getMatch()
                        .getId()
                        .equals(matchId)) {

            throw new ConflictException(
                    "Batters must belong to this match"
            );
        }

        if (!striker.getTournamentTeam()
                .getId()
                .equals(battingTeam.getId())
                ||
                !nonStriker.getTournamentTeam()
                        .getId()
                        .equals(battingTeam.getId())) {

            throw new ConflictException(
                    "Batters must belong to the batting team"
            );
        }
    }

    private void validateBowler(
            Long matchId,
            TournamentTeam bowlingTeam,
            PlayingXiEntry player
    ) {

        if (!player.getMatch()
                .getId()
                .equals(matchId)
                ||
                !player.getTournamentTeam()
                        .getId()
                        .equals(bowlingTeam.getId())) {

            throw new ConflictException(
                    "Player must belong to the bowling team"
            );
        }
    }

    private void authorizeScorer(
            Long matchId,
            Long userId,
            boolean privileged
    ) {

        if (privileged) {
            return;
        }

        if (!scorerRepository
                .existsByMatch_IdAndUser_Id(
                        matchId,
                        userId
                )) {

            throw new ForbiddenException(
                    "You are not assigned as a scorer for this match"
            );
        }
    }

    private void publishScoreChange(Long matchId) {

        eventPublisher.publishEvent(
                new MatchScoreChangedEvent(matchId)
        );
    }

    private InningsResponse toResponse(
            Innings innings
    ) {

        int legalBalls =
                innings.getLegalBalls();

        String overs =
                (legalBalls / 6)
                + "."
                + (legalBalls % 6);

        return new InningsResponse(
                innings.getId(),
                innings.getInningsNumber(),

                innings.getBattingTeam()
                        .getTeam()
                        .getName(),

                innings.getBowlingTeam()
                        .getTeam()
                        .getName(),

                innings.getTotalRuns(),
                innings.getWickets(),

                legalBalls,
                overs,

                innings.getTargetRuns(),

                innings.getWideRuns(),
                innings.getNoBallRuns(),
                innings.getByeRuns(),
                innings.getLegByeRuns(),
                innings.getPenaltyRuns(),

                innings.getStatus()
        );
    }
}
