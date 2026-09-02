package com.eidcricketfest.scoring.service;

import com.eidcricketfest.match.entity.PlayingXiEntry;
import com.eidcricketfest.match.entity.CricketMatch;
import com.eidcricketfest.match.repository.CricketMatchRepository;
import com.eidcricketfest.common.exception.ResourceNotFoundException;
import com.eidcricketfest.scoring.dto.ScorecardResponse;
import com.eidcricketfest.scoring.entity.*;
import com.eidcricketfest.scoring.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class ScorecardService {

    private final InningsRepository inningsRepository;
    private final DeliveryRepository deliveryRepository;
    private final WicketRepository wicketRepository;
    private final CricketMatchRepository matchRepository;

    public ScorecardService(
            InningsRepository inningsRepository,
            DeliveryRepository deliveryRepository,
            WicketRepository wicketRepository,
            CricketMatchRepository matchRepository
    ) {
        this.inningsRepository = inningsRepository;
        this.deliveryRepository = deliveryRepository;
        this.wicketRepository = wicketRepository;
        this.matchRepository = matchRepository;
    }

    public ScorecardResponse getScorecard(Long matchId) {

        CricketMatch match =
                matchRepository
                        .findDetailedById(matchId)
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

        return new ScorecardResponse(
                matchId,
                match.getMatchType(),
                match.getMatchNumber(),
                match.getStage(),
                match.getStatus(),
                match.getResultStatus(),
                match.getResultSummary(),
                match.getRematchOfMatch() != null
                        ? match.getRematchOfMatch().getId()
                        : null,
                match.getSupersededByMatch() != null
                        ? match.getSupersededByMatch().getId()
                        : null,
                inningsList.stream()
                        .map(this::buildInnings)
                        .toList()
        );
    }

    private ScorecardResponse.InningsScorecard buildInnings(
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

        Map<Long, BattingAccumulator> batting =
                new LinkedHashMap<>();

        Map<Long, BowlingAccumulator> bowling =
                new LinkedHashMap<>();

        for (Delivery delivery : deliveries) {

            PlayingXiEntry striker =
                    delivery.getStriker();

            Long batterId =
                    striker.getId();

            BattingAccumulator batter =
                    batting.computeIfAbsent(
                            batterId,
                            ignored ->
                                    new BattingAccumulator(
                                            striker
                                    )
                    );

            batter.runs +=
                    delivery.getRunsOffBat();

            /*
             * Wides do not count as balls faced.
             * No-balls do count as a delivery faced
             * for the batter's scorecard.
             */
            if (delivery.getWideRuns() == 0) {
                batter.balls++;
            }

            if (delivery.getRunsOffBat() == 4) {
                batter.fours++;
            }

            if (delivery.getRunsOffBat() == 6) {
                batter.sixes++;
            }

            PlayingXiEntry bowlerXi =
                    delivery.getBowler();

            BowlingAccumulator bowler =
                    bowling.computeIfAbsent(
                            bowlerXi.getId(),
                            ignored ->
                                    new BowlingAccumulator(
                                            bowlerXi
                                    )
                    );

            if (delivery.isLegal()) {
                bowler.legalBalls++;
            }

            /*
             * Byes and leg-byes are not charged
             * against the bowler.
             */
            bowler.runs +=
                    delivery.getRunsOffBat()
                    + delivery.getWideRuns()
                    + delivery.getNoBallRuns();
        }

        for (Wicket wicket : wickets) {

            BattingAccumulator batter =
                    batting.computeIfAbsent(
                            wicket.getDismissedPlayer()
                                    .getId(),
                            ignored ->
                                    new BattingAccumulator(
                                            wicket.getDismissedPlayer()
                                    )
                    );

            batter.dismissal =
                    dismissalText(wicket);

            if (wicket.isCreditedToBowler()) {

                Delivery delivery =
                        wicket.getDelivery();

                if (delivery != null) {

                    BowlingAccumulator bowler =
                            bowling.get(
                                    delivery.getBowler()
                                            .getId()
                            );

                    if (bowler != null) {
                        bowler.wickets++;
                    }
                }
            }
        }

        int legalBalls =
                innings.getLegalBalls();

        String overs =
                overs(legalBalls);

        return new ScorecardResponse.InningsScorecard(
                innings.getInningsNumber(),

                innings.getBattingSide()
                        .getDisplayName(),

                innings.getTotalRuns(),
                innings.getWickets(),
                overs,

                batting.values()
                        .stream()
                        .map(this::battingRow)
                        .toList(),

                bowling.values()
                        .stream()
                        .map(this::bowlingRow)
                        .toList()
        );
    }

    private ScorecardResponse.BattingRow battingRow(
            BattingAccumulator a
    ) {

        BigDecimal strikeRate =
                a.balls == 0
                        ? BigDecimal.ZERO
                        : BigDecimal.valueOf(
                                a.runs * 100.0
                                / a.balls
                        ).setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        return new ScorecardResponse.BattingRow(
                a.player.getPlayer()
                        .getId(),

                a.player.getPlayer()
                        .getFullName(),

                a.runs,
                a.balls,
                a.fours,
                a.sixes,
                strikeRate,
                a.dismissal
        );
    }

    private ScorecardResponse.BowlingRow bowlingRow(
            BowlingAccumulator a
    ) {

        BigDecimal economy =
                a.legalBalls == 0
                        ? BigDecimal.ZERO
                        : BigDecimal.valueOf(
                                a.runs * 6.0
                                / a.legalBalls
                        ).setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        return new ScorecardResponse.BowlingRow(
                a.player.getPlayer()
                        .getId(),

                a.player.getPlayer()
                        .getFullName(),

                overs(a.legalBalls),

                a.runs,
                a.wickets,
                economy
        );
    }

    private String dismissalText(Wicket wicket) {

        return switch (wicket.getDismissalType()) {

            case BOWLED ->
                    "bowled";

            case LBW ->
                    "lbw";

            case CAUGHT ->
                    wicket.getFielder() == null
                            ? "caught"
                            : "caught by "
                            + wicket.getFielder()
                                    .getPlayer()
                                    .getFullName();

            case RUN_OUT ->
                    "run out";

            case STUMPED ->
                    "stumped";

            case HIT_WICKET ->
                    "hit wicket";

            case HIT_BALL_TWICE ->
                    "hit the ball twice";

            case OBSTRUCTING_FIELD ->
                    "obstructing the field";
        };
    }

    private String overs(int legalBalls) {
        return (legalBalls / 6)
                + "."
                + (legalBalls % 6);
    }

    private static class BattingAccumulator {

        private final PlayingXiEntry player;

        private int runs;
        private int balls;
        private int fours;
        private int sixes;

        private String dismissal = "not out";

        private BattingAccumulator(
                PlayingXiEntry player
        ) {
            this.player = player;
        }
    }

    private static class BowlingAccumulator {

        private final PlayingXiEntry player;

        private int legalBalls;
        private int runs;
        private int wickets;

        private BowlingAccumulator(
                PlayingXiEntry player
        ) {
            this.player = player;
        }
    }
}
