package com.eidcricketfest.scoring.service;

import com.eidcricketfest.common.exception.ForbiddenException;
import com.eidcricketfest.common.exception.ResourceNotFoundException;
import com.eidcricketfest.match.dto.MatchResponse;
import com.eidcricketfest.match.entity.CricketMatch;
import com.eidcricketfest.match.entity.MatchToss;
import com.eidcricketfest.match.entity.TossDecision;
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
import com.eidcricketfest.scoring.repository.InningsRepository;
import com.eidcricketfest.team.entity.TournamentTeam;
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
    private final LiveScoreService liveScoreService;

    public ScorerMatchQueryService(
            CricketMatchRepository matchRepository,
            MatchScorerRepository scorerRepository,
            MatchTossRepository tossRepository,
            PlayingXiEntryRepository playingXiRepository,
            InningsRepository inningsRepository,
            LiveScoreService liveScoreService
    ) {
        this.matchRepository = matchRepository;
        this.scorerRepository = scorerRepository;
        this.tossRepository = tossRepository;
        this.playingXiRepository = playingXiRepository;
        this.inningsRepository = inningsRepository;
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

        Long teamAId =
                match.getTeamA()
                        .getId();

        Long teamBId =
                match.getTeamB()
                        .getId();

        LiveMatchResponse live =
                liveScoreService
                        .getLiveMatch(matchId);

        NextInnings nextInnings =
                nextInnings(match);

        return new ScorerMatchStateResponse(
                toMatchResponse(match),
                live,
                playersForTeam(playingXi, teamAId),
                playersForTeam(playingXi, teamBId),
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
                latest.getBowlingTeam()
                        .getId(),
                latest.getBattingTeam()
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

        TournamentTeam tossWinner =
                toss.getWinnerTeam();

        TournamentTeam other =
                tossWinner.getId()
                        .equals(match.getTeamA().getId())
                        ? match.getTeamB()
                        : match.getTeamA();

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
            Long tournamentTeamId
    ) {

        return playingXi
                .stream()
                .filter(entry ->
                        entry.getTournamentTeam()
                                .getId()
                                .equals(tournamentTeamId)
                )
                .map(this::toPlayer)
                .toList();
    }

    private ScorerMatchStateResponse.PlayingXiPlayer toPlayer(
            PlayingXiEntry entry
    ) {

        return new ScorerMatchStateResponse.PlayingXiPlayer(
                entry.getId(),
                entry.getTournamentTeam()
                        .getId(),
                entry.getTournamentTeam()
                        .getTeam()
                        .getName(),
                entry.getRegistration()
                        .getPlayer()
                        .getId(),
                entry.getRegistration()
                        .getPlayer()
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
                match.getMatchNumber(),
                match.getRoundNumber(),
                match.getStage(),
                match.getStatus(),
                new MatchResponse.TeamInfo(
                        match.getTeamA().getId(),
                        match.getTeamA()
                                .getTeam()
                                .getName()
                ),
                new MatchResponse.TeamInfo(
                        match.getTeamB().getId(),
                        match.getTeamB()
                                .getTeam()
                                .getName()
                ),
                match.getOversPerInnings(),
                venue,
                match.getScheduledAt(),
                scorerRepository.existsByMatch_Id(match.getId()),
                playingXiSubmitted(
                        match,
                        match.getTeamA().getId()
                ),
                playingXiSubmitted(
                        match,
                        match.getTeamB().getId()
                ),
                tossRepository.existsByMatch_Id(match.getId())
        );
    }

    private boolean playingXiSubmitted(
            CricketMatch match,
            Long tournamentTeamId
    ) {

        Integer required =
                match.getTournamentEdition()
                        .getPlayingXiSize();

        return required != null
                && required > 0
                && playingXiRepository
                .countByMatch_IdAndTournamentTeam_Id(
                        match.getId(),
                        tournamentTeamId
                ) == required;
    }

    private record NextInnings(
            Long battingTeamId,
            Long bowlingTeamId
    ) {}
}
