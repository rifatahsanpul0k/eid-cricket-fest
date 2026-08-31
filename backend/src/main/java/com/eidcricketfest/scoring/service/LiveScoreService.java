package com.eidcricketfest.scoring.service;

import com.eidcricketfest.common.exception.ResourceNotFoundException;
import com.eidcricketfest.match.entity.CricketMatch;
import com.eidcricketfest.match.entity.PlayingXiEntry;
import com.eidcricketfest.match.repository.CricketMatchRepository;
import com.eidcricketfest.scoring.dto.LiveMatchResponse;
import com.eidcricketfest.scoring.entity.Delivery;
import com.eidcricketfest.scoring.entity.Innings;
import com.eidcricketfest.scoring.repository.DeliveryRepository;
import com.eidcricketfest.scoring.repository.InningsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class LiveScoreService {

    private final CricketMatchRepository matchRepository;
    private final InningsRepository inningsRepository;
    private final DeliveryRepository deliveryRepository;

    public LiveScoreService(
            CricketMatchRepository matchRepository,
            InningsRepository inningsRepository,
            DeliveryRepository deliveryRepository
    ) {
        this.matchRepository = matchRepository;
        this.inningsRepository = inningsRepository;
        this.deliveryRepository = deliveryRepository;
    }

    public LiveMatchResponse getLiveMatch(Long matchId) {

        CricketMatch match =
                matchRepository.findDetailedById(matchId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Match not found"
                                )
                        );

        List<Innings> inningsList =
                inningsRepository
                        .findByMatch_IdOrderByInningsNumber(
                                matchId
                        );

        Innings innings =
                inningsList.isEmpty()
                        ? null
                        : inningsList.get(
                                inningsList.size() - 1
                        );

        LiveMatchResponse.InningsInfo inningsInfo =
                innings == null
                        ? null
                        : inningsInfo(innings);

        List<LiveMatchResponse.BallInfo> recent =
                innings == null
                        ? List.of()
                        : recentBalls(innings);

        return new LiveMatchResponse(
                match.getId(),
                match.getMatchNumber(),
                match.getStatus(),

                match.getTeamA()
                        .getTeam()
                        .getName(),

                match.getTeamB()
                        .getTeam()
                        .getName(),

                inningsInfo,
                recent
        );
    }

    private LiveMatchResponse.InningsInfo inningsInfo(
            Innings innings
    ) {

        int legalBalls = innings.getLegalBalls();

        String overs =
                (legalBalls / 6)
                + "."
                + (legalBalls % 6);

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

        Integer required = null;
        Integer ballsRemaining = null;
        BigDecimal requiredRate = null;

        if (innings.getTargetRuns() != null) {

            required =
                    Math.max(
                            innings.getTargetRuns()
                                    - innings.getTotalRuns(),
                            0
                    );

            int maximumBalls =
                    innings.getMatch()
                            .getOversPerInnings()
                            * 6;

            ballsRemaining =
                    Math.max(
                            maximumBalls - legalBalls,
                            0
                    );

            requiredRate =
                    ballsRemaining == 0
                            ? BigDecimal.ZERO
                            : BigDecimal.valueOf(
                                    required * 6.0
                                    / ballsRemaining
                            ).setScale(
                                    2,
                                    RoundingMode.HALF_UP
                            );
        }

        return new LiveMatchResponse.InningsInfo(
                innings.getId(),
                innings.getInningsNumber(),
                innings.getScoreRevision(),

                innings.getBattingTeam()
                        .getTeam()
                        .getName(),

                innings.getBowlingTeam()
                        .getTeam()
                        .getName(),

                innings.getTotalRuns(),
                innings.getWickets(),
                overs,

                innings.getTargetRuns(),
                required,
                ballsRemaining,

                currentRunRate,
                requiredRate,

                player(innings.getCurrentStriker()),
                player(innings.getCurrentNonStriker()),
                player(innings.getCurrentBowler())
        );
    }

    private LiveMatchResponse.PlayerInfo player(
            PlayingXiEntry xi
    ) {

        if (xi == null) {
            return null;
        }

        return new LiveMatchResponse.PlayerInfo(
                xi.getRegistration()
                        .getPlayer()
                        .getId(),

                xi.getRegistration()
                        .getPlayer()
                        .getFullName()
        );
    }

    private List<LiveMatchResponse.BallInfo> recentBalls(
            Innings innings
    ) {

        List<Delivery> all =
                deliveryRepository
                        .findActiveDeliveries(
                                innings.getId()
                        );

        int start =
                Math.max(0, all.size() - 6);

        List<LiveMatchResponse.BallInfo> result =
                new ArrayList<>();

        for (int i = start; i < all.size(); i++) {

            Delivery d = all.get(i);

            result.add(
                    new LiveMatchResponse.BallInfo(
                            d.getId(),
                            d.getSequenceNo(),
                            d.calculateTotalRuns(),
                            d.isLegal(),
                            d.getCommentary()
                    )
            );
        }

        return result;
    }
}
