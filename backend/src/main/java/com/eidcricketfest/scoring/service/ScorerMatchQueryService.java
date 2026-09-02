package com.eidcricketfest.scoring.service;

import com.eidcricketfest.common.exception.ForbiddenException;
import com.eidcricketfest.common.exception.ResourceNotFoundException;
import com.eidcricketfest.match.dto.MatchResponse;
import com.eidcricketfest.match.entity.CricketMatch;
import com.eidcricketfest.match.entity.MatchToss;
import com.eidcricketfest.match.entity.TossDecision;
import com.eidcricketfest.match.entity.MatchSide;
import com.eidcricketfest.match.entity.PlayingXiEntry;
import com.eidcricketfest.match.repository.CricketMatchRepository;
import com.eidcricketfest.match.repository.MatchScorerRepository;
import com.eidcricketfest.match.repository.MatchTossRepository;
import com.eidcricketfest.match.repository.PlayingXiEntryRepository;
import com.eidcricketfest.scoring.dto.LiveMatchResponse;
import com.eidcricketfest.scoring.dto.ScorerMatchResponse;
import com.eidcricketfest.scoring.dto.ScorerMatchStateResponse;
import com.eidcricketfest.scoring.entity.Innings;
import com.eidcricketfest.scoring.entity.InningsStatus;
import com.eidcricketfest.scoring.entity.Delivery;
import com.eidcricketfest.scoring.repository.InningsRepository;
import com.eidcricketfest.scoring.repository.DeliveryRepository;
import com.eidcricketfest.scoring.repository.WicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ScorerMatchQueryService {

    private final CricketMatchRepository matchRepository;
    private final MatchScorerRepository scorerRepository;
    private final MatchTossRepository tossRepository;
    private final PlayingXiEntryRepository playingXiRepository;
    private final InningsRepository inningsRepository;
    private final DeliveryRepository deliveryRepository;
    private final WicketRepository wicketRepository;
    private final LiveScoreService liveScoreService;

    public ScorerMatchQueryService(
            CricketMatchRepository matchRepository,
            MatchScorerRepository scorerRepository,
            MatchTossRepository tossRepository,
            PlayingXiEntryRepository playingXiRepository,
            InningsRepository inningsRepository,
            DeliveryRepository deliveryRepository,
            WicketRepository wicketRepository,
            LiveScoreService liveScoreService
    ) {
        this.matchRepository = matchRepository;
        this.scorerRepository = scorerRepository;
        this.tossRepository = tossRepository;
        this.playingXiRepository = playingXiRepository;
        this.inningsRepository = inningsRepository;
        this.deliveryRepository = deliveryRepository;
        this.wicketRepository = wicketRepository;
        this.liveScoreService = liveScoreService;
    }

    public List<ScorerMatchResponse> assignedMatches(Long userId) {

        return matchRepository
                .findDetailedAssignedToScorer(userId)
                .stream()
                .map(match -> new ScorerMatchResponse(
                        toMatchResponse(match),
                        true
                ))
                .toList();
    }

    public ScorerMatchStateResponse matchState(
            Long matchId,
            Long userId,
            boolean privileged
    ) {

        CricketMatch match =
                matchRepository
                        .findDetailedById(matchId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Match not found"
                                )
                        );

        boolean assigned =
                scorerRepository
                        .existsByMatch_IdAndUser_Id(
                                matchId,
                                userId
                        );

        if (!privileged && !assigned) {
            throw new ForbiddenException(
                    "You are not assigned as a scorer for this match"
            );
        }

        List<PlayingXiEntry> playingXi =
                playingXiRepository
                        .findDetailedByMatchId(matchId);

        Long teamASideId =
                match.getTeamASide()
                        .getId();

        Long teamBSideId =
                match.getTeamBSide()
                        .getId();

        LiveMatchResponse live =
                liveScoreService
                        .getLiveMatch(matchId);

        NextInnings nextInnings =
                nextInnings(match);

        return new ScorerMatchStateResponse(
                toMatchResponse(match),
                live,
                playersForTeam(playingXi, teamASideId),
                playersForTeam(playingXi, teamBSideId),
                dismissedPlayingXiIds(live),
                previousOverBowlerPlayingXiId(live),
                nextInnings.battingTeamId(),
                nextInnings.bowlingTeamId(),
                assigned
        );
    }

    private NextInnings nextInnings(CricketMatch match) {

        List<Innings> innings =
                inningsRepository
                        .findByMatch_IdOrderByInningsNumber(
                                match.getId()
                        );

        if (innings.isEmpty()) {
            return firstInningsTeams(match);
        }

        Innings latest =
                innings.get(innings.size() - 1);

        if (latest.getStatus() != InningsStatus.COMPLETED
                || latest.getInningsNumber() != 1) {
            return new NextInnings(null, null);
        }

        if (innings.size() > 1) {
            return new NextInnings(null, null);
        }

        return new NextInnings(
                latest.getBowlingSide()
                        .getId(),
                latest.getBattingSide()
                        .getId()
        );
    }

    private NextInnings firstInningsTeams(CricketMatch match) {

        MatchToss toss =
                tossRepository
                        .findByMatch_Id(match.getId())
                        .orElse(null);

        if (toss == null) {
            return new NextInnings(null, null);
        }

        MatchSide tossWinner =
                toss.getWinnerSide();

        MatchSide other =
                tossWinner.getId()
                        .equals(match.getTeamASide().getId())
                        ? match.getTeamBSide()
                        : match.getTeamASide();

        if (toss.getDecision() == TossDecision.BAT) {
            return new NextInnings(
                    tossWinner.getId(),
                    other.getId()
            );
        }

        return new NextInnings(
                other.getId(),
                tossWinner.getId()
        );
    }

    private List<ScorerMatchStateResponse.PlayingXiPlayer> playersForTeam(
            List<PlayingXiEntry> playingXi,
            Long matchSideId
    ) {

        return playingXi
                .stream()
                .filter(entry ->
                        entry.getMatchSide()
                                .getId()
                                .equals(matchSideId)
                )
                .map(this::toPlayer)
                .toList();
    }

    private ScorerMatchStateResponse.PlayingXiPlayer toPlayer(
            PlayingXiEntry entry
    ) {

        return new ScorerMatchStateResponse.PlayingXiPlayer(
                entry.getId(),
                entry.getMatchSide()
                        .getId(),
                entry.getMatchSide()
                        .getDisplayName(),
                entry.getPlayer()
                        .getId(),
                entry.getPlayer()
                        .getFullName(),
                entry.isCaptain(),
                entry.isWicketkeeper()
        );
    }

    private MatchResponse toMatchResponse(CricketMatch match) {

        MatchResponse.VenueInfo venue = null;

        if (match.getVenue() != null) {
            venue = new MatchResponse.VenueInfo(
                    match.getVenue().getId(),
                    match.getVenue().getName()
            );
        }

        return new MatchResponse(
                match.getId(),
                match.getMatchType(),
                match.getMatchNumber(),
                match.getRoundNumber(),
                match.getStage(),
                match.getStatus(),
                match.getResultStatus(),
                match.getRematchOfMatch() != null
                        ? match.getRematchOfMatch().getId()
                        : null,
                match.getSupersededByMatch() != null
                        ? match.getSupersededByMatch().getId()
                        : null,
                new MatchResponse.TeamInfo(
                        match.getTeamASide().getId(),
                        match.getTeamA() != null
                                ? match.getTeamA().getId()
                                : null,
                        match.getTeamASide().getDisplayName()
                ),
                new MatchResponse.TeamInfo(
                        match.getTeamBSide().getId(),
                        match.getTeamB() != null
                                ? match.getTeamB().getId()
                                : null,
                        match.getTeamBSide().getDisplayName()
                ),
                match.getOversPerInnings(),
                venue,
                match.getScheduledAt(),
                scorerRepository.existsByMatch_Id(match.getId()),
                playingXiSubmitted(
                        match,
                        match.getTeamASide().getId()
                ),
                playingXiSubmitted(
                        match,
                        match.getTeamBSide().getId()
                ),
                tossRepository.existsByMatch_Id(match.getId()),
                List.of(),
                List.of()
        );
    }

    private List<Long> dismissedPlayingXiIds(
            LiveMatchResponse live
    ) {

        if (live.innings() == null) {
            return List.of();
        }

        Long inningsId =
                live.innings()
                        .inningsId();

        if (inningsId == null) {
            return List.of();
        }

        return wicketRepository
                .findActiveByInningsId(inningsId)
                .stream()
                .map(wicket ->
                        wicket.getDismissedPlayer()
                                .getId()
                )
                .distinct()
                .toList();
    }

    private Long previousOverBowlerPlayingXiId(
            LiveMatchResponse live
    ) {

        if (live.innings() == null
                || live.innings().bowler() != null) {
            return null;
        }

        Long inningsId =
                live.innings()
                        .inningsId();

        if (inningsId == null) {
            return null;
        }

        List<Delivery> deliveries =
                deliveryRepository
                        .findActiveDeliveries(inningsId);

        int legalBalls = 0;
        Delivery lastLegalDelivery = null;

        for (Delivery delivery : deliveries) {
            if (delivery.isLegal()) {
                legalBalls++;
                lastLegalDelivery = delivery;
            }
        }

        if (legalBalls == 0
                || legalBalls % 6 != 0
                || lastLegalDelivery == null) {
            return null;
        }

        return lastLegalDelivery
                .getBowler()
                .getId();
    }

    private boolean playingXiSubmitted(
            CricketMatch match,
            Long matchSideId
    ) {

        Integer required =
                match.isTournament()
                        ? match.requireTournamentEdition()
                                .getPlayingXiSize()
                        : Math.toIntExact(
                                playingXiRepository
                                        .countByMatch_IdAndMatchSide_Id(
                                                match.getId(),
                                                matchSideId
                                        )
                        );

        return required != null
                && required > 0
                && playingXiRepository
                .countByMatch_IdAndMatchSide_Id(
                        match.getId(),
                        matchSideId
                ) == required;
    }

    private record NextInnings(
            Long battingTeamId,
            Long bowlingTeamId
    ) {}
}
