package com.eidcricketfest.scoring.service;

import com.eidcricketfest.match.entity.*;
import com.eidcricketfest.match.repository.*;
import com.eidcricketfest.scoring.dto.LiveCentreMatchResponse;
import com.eidcricketfest.scoring.entity.Innings;
import com.eidcricketfest.scoring.repository.InningsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class LiveCentreService {

    private static final Duration RECENT_RESULT_WINDOW =
            Duration.ofHours(48);

    private final CricketMatchRepository matchRepository;
    private final InningsRepository inningsRepository;
    private final MatchTossRepository tossRepository;

    public LiveCentreService(
            CricketMatchRepository matchRepository,
            InningsRepository inningsRepository,
            MatchTossRepository tossRepository
    ) {
        this.matchRepository = matchRepository;
        this.inningsRepository = inningsRepository;
        this.tossRepository = tossRepository;
    }

    public List<LiveCentreMatchResponse> getLiveCentreMatches() {

        List<CricketMatch> matches =
                matchRepository.findLiveCentreMatches(
                        Instant.now().minus(RECENT_RESULT_WINDOW)
                );

        if (matches.isEmpty()) {
            return List.of();
        }

        Set<Long> matchIds =
                matches.stream()
                        .map(CricketMatch::getId)
                        .collect(Collectors.toCollection(
                                LinkedHashSet::new
                        ));

        Map<Long, Innings> latestInnings =
                latestInningsByMatch(matchIds);

        Map<Long, MatchToss> tosses =
                tossRepository.findDetailedByMatchIds(matchIds)
                        .stream()
                        .collect(Collectors.toMap(
                                toss -> toss.getMatch().getId(),
                                Function.identity()
                        ));

        return matches.stream()
                .map(match ->
                        toResponse(
                                match,
                                latestInnings.get(match.getId()),
                                tosses.get(match.getId())
                        )
                )
                .toList();
    }

    private Map<Long, Innings> latestInningsByMatch(
            Set<Long> matchIds
    ) {

        Map<Long, Innings> latest =
                new HashMap<>();

        for (Innings innings :
                inningsRepository.findDetailedByMatchIds(matchIds)) {
            latest.put(
                    innings.getMatch().getId(),
                    innings
            );
        }

        return latest;
    }

    private LiveCentreMatchResponse toResponse(
            CricketMatch match,
            Innings innings,
            MatchToss toss
    ) {

        return new LiveCentreMatchResponse(
                match.getId(),
                match.getMatchType(),
                match.getStatus(),
                match.getMatchNumber(),
                match.getStage(),
                match.getScheduledAt(),
                match.getOversPerInnings(),
                match.getResultStatus(),
                match.getRematchOfMatch() != null
                        ? match.getRematchOfMatch().getId()
                        : null,
                match.getSupersededByMatch() != null
                        ? match.getSupersededByMatch().getId()
                        : null,
                side(match.getTeamASide()),
                side(match.getTeamBSide()),
                venue(match.getVenue()),
                toss(toss),
                innings == null
                        ? null
                        : innings(match, innings),
                resultText(match),
                side(match.getWinnerSide())
        );
    }

    private LiveCentreMatchResponse.SideInfo side(
            MatchSide side
    ) {

        if (side == null) {
            return null;
        }

        return new LiveCentreMatchResponse.SideInfo(
                side.getId(),
                side.getTournamentTeam() != null
                        ? side.getTournamentTeam().getId()
                        : null,
                side.getDisplayName()
        );
    }

    private LiveCentreMatchResponse.VenueInfo venue(
            Venue venue
    ) {

        if (venue == null) {
            return null;
        }

        return new LiveCentreMatchResponse.VenueInfo(
                venue.getId(),
                venue.getName()
        );
    }

    private LiveCentreMatchResponse.TossInfo toss(
            MatchToss toss
    ) {

        if (toss == null) {
            return null;
        }

        MatchSide winner =
                toss.getWinnerSide();

        return new LiveCentreMatchResponse.TossInfo(
                winner.getId(),
                winner.getDisplayName(),
                toss.getDecision()
        );
    }

    private LiveCentreMatchResponse.InningsSummary innings(
            CricketMatch match,
            Innings innings
    ) {

        int legalBalls =
                innings.getLegalBalls();

        BigDecimal currentRunRate =
                legalBalls == 0
                        ? BigDecimal.ZERO
                        : BigDecimal.valueOf(
                                innings.getTotalRuns() * 6.0
                                / legalBalls
                        ).setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        Integer target =
                innings.getTargetRuns();

        if (target == null
                && match.getStatus() == MatchStatus.INNINGS_BREAK) {
            target = innings.getTotalRuns() + 1;
        }

        Integer runsRequired = null;
        Integer ballsRemaining = null;
        BigDecimal requiredRate = null;

        if (target != null) {
            runsRequired =
                    Math.max(
                            target - innings.getTotalRuns(),
                            0
                    );

            ballsRemaining =
                    Math.max(
                            match.getOversPerInnings() * 6
                                    - legalBalls,
                            0
                    );

            requiredRate =
                    ballsRemaining == 0
                            ? BigDecimal.ZERO
                            : BigDecimal.valueOf(
                                    runsRequired * 6.0
                                            / ballsRemaining
                            ).setScale(
                                    2,
                                    RoundingMode.HALF_UP
                            );
        }

        return new LiveCentreMatchResponse.InningsSummary(
                innings.getId(),
                innings.getInningsNumber(),
                innings.getBattingSide().getDisplayName(),
                innings.getBowlingSide().getDisplayName(),
                innings.getTotalRuns(),
                innings.getWickets(),
                overs(legalBalls),
                target,
                runsRequired,
                ballsRemaining,
                currentRunRate,
                requiredRate,
                player(innings.getCurrentStriker()),
                player(innings.getCurrentNonStriker()),
                player(innings.getCurrentBowler())
        );
    }

    private LiveCentreMatchResponse.PlayerInfo player(
            PlayingXiEntry xi
    ) {

        if (xi == null) {
            return null;
        }

        return new LiveCentreMatchResponse.PlayerInfo(
                xi.getPlayer().getId(),
                xi.getPlayer().getFullName()
        );
    }

    private String overs(int legalBalls) {
        return (legalBalls / 6)
                + "."
                + (legalBalls % 6);
    }

    private String resultText(CricketMatch match) {
        if (match.getResultStatus() == MatchResultStatus.UNDER_REVIEW) {
            return "Result under review";
        }

        if (match.getResultStatus() == MatchResultStatus.VOID) {
            return "Result voided";
        }

        if (match.getResultStatus() == MatchResultStatus.SUPERSEDED) {
            return "Result superseded";
        }

        return match.getResultSummary();
    }
}
