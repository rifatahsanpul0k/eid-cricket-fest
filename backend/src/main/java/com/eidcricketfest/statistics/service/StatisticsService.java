package com.eidcricketfest.statistics.service;

import com.eidcricketfest.common.exception.ResourceNotFoundException;
import com.eidcricketfest.match.entity.PlayingXiEntry;
import com.eidcricketfest.match.repository.PlayingXiEntryRepository;
import com.eidcricketfest.player.entity.Player;
import com.eidcricketfest.player.repository.PlayerRepository;
import com.eidcricketfest.scoring.entity.*;
import com.eidcricketfest.scoring.repository.*;
import com.eidcricketfest.statistics.dto.PlayerCareerResponse;
import com.eidcricketfest.statistics.dto.TournamentStatisticsResponse;
import com.eidcricketfest.tournament.repository.TournamentEditionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class StatisticsService {

    private final TournamentEditionRepository editionRepository;
    private final DeliveryRepository deliveryRepository;
    private final WicketRepository wicketRepository;
    private final PlayerRepository playerRepository;
    private final PlayingXiEntryRepository playingXiRepository;

    public StatisticsService(
            TournamentEditionRepository editionRepository,
            DeliveryRepository deliveryRepository,
            WicketRepository wicketRepository,
            PlayerRepository playerRepository,
            PlayingXiEntryRepository playingXiRepository
    ) {
        this.editionRepository = editionRepository;
        this.deliveryRepository = deliveryRepository;
        this.wicketRepository = wicketRepository;
        this.playerRepository = playerRepository;
        this.playingXiRepository = playingXiRepository;
    }

    public TournamentStatisticsResponse statistics(
            Long editionId
    ) {

        if (!editionRepository.existsById(editionId)) {
            throw new ResourceNotFoundException(
                    "Tournament edition not found"
            );
        }

        List<Delivery> deliveries =
                deliveryRepository
                        .findActiveByEditionId(
                                editionId
                        );

        List<Wicket> wickets =
                wicketRepository
                        .findActiveByEditionId(
                                editionId
                        );

        Map<Long, BatterStats> batting =
                buildBatting(deliveries, wickets);

        Map<Long, BowlerStats> bowling =
                buildBowling(deliveries, wickets);

        return new TournamentStatisticsResponse(
                editionId,
                battingLeaderboard(batting),
                bowlingLeaderboard(bowling)
        );
    }

    public PlayerCareerResponse playerCareer(
            Long playerId
    ) {

        Player player =
                playerRepository.findById(playerId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Player not found"
                                )
                        );

        List<Delivery> battingDeliveries =
                deliveryRepository
                        .findActiveBattingDeliveriesByPlayerId(
                                playerId
                        );

        List<Delivery> bowlingDeliveries =
                deliveryRepository
                        .findActiveBowlingDeliveriesByPlayerId(
                                playerId
                        );

        List<Wicket> dismissals =
                wicketRepository
                        .findDismissalsForPlayer(
                                playerId
                        );

        List<Wicket> bowlingWickets =
                wicketRepository
                        .findBowlerWicketsForPlayer(
                                playerId
                        );

        return new PlayerCareerResponse(
                playerId,
                player.getFullName(),

                Math.toIntExact(
                        playingXiRepository
                                .countEditionsPlayed(playerId)
                ),

                Math.toIntExact(
                        playingXiRepository
                                .countMatchesPlayed(playerId)
                ),

                buildCareerBatting(
                        battingDeliveries,
                        dismissals.size()
                ),

                buildCareerBowling(
                        bowlingDeliveries,
                        bowlingWickets
                )
        );
    }

    private Map<Long, BatterStats> buildBatting(
            List<Delivery> deliveries,
            List<Wicket> wickets
    ) {

        Map<Long, BatterStats> map =
                new HashMap<>();

        for (Delivery delivery : deliveries) {

            PlayingXiEntry striker =
                    delivery.getStriker();

            Long playerId =
                    striker.getRegistration()
                            .getPlayer()
                            .getId();

            BatterStats stats =
                    map.computeIfAbsent(
                            playerId,
                            ignored ->
                                    new BatterStats(
                                            playerId,
                                            striker.getRegistration()
                                                    .getPlayer()
                                                    .getFullName()
                                    )
                    );

            Long inningsId =
                    delivery.getInnings().getId();

            stats.inningsPlayed.add(
                    inningsId
            );

            int runs =
                    delivery.getRunsOffBat();

            stats.runs += runs;

            /*
             * Wide is not a ball faced.
             */
            if (delivery.getWideRuns() == 0) {
                stats.balls++;
            }

            if (runs == 4) {
                stats.fours++;
            }

            if (runs == 6) {
                stats.sixes++;
            }

            stats.runsPerInnings.merge(
                    inningsId,
                    runs,
                    Integer::sum
            );
        }

        for (Wicket wicket : wickets) {

            Long playerId =
                    wicket.getDismissedPlayer()
                            .getRegistration()
                            .getPlayer()
                            .getId();

            BatterStats stats =
                    map.computeIfAbsent(
                            playerId,
                            ignored ->
                                    new BatterStats(
                                            playerId,
                                            wicket.getDismissedPlayer()
                                                    .getRegistration()
                                                    .getPlayer()
                                                    .getFullName()
                                    )
                    );

            stats.dismissals++;
        }

        for (BatterStats stats : map.values()) {

            stats.highestScore =
                    stats.runsPerInnings
                            .values()
                            .stream()
                            .max(Integer::compareTo)
                            .orElse(0);
        }

        return map;
    }

    private Map<Long, BowlerStats> buildBowling(
            List<Delivery> deliveries,
            List<Wicket> wickets
    ) {

        Map<Long, BowlerStats> map =
                new HashMap<>();

        for (Delivery delivery : deliveries) {

            PlayingXiEntry bowler =
                    delivery.getBowler();

            Long playerId =
                    bowler.getRegistration()
                            .getPlayer()
                            .getId();

            BowlerStats stats =
                    map.computeIfAbsent(
                            playerId,
                            ignored ->
                                    new BowlerStats(
                                            playerId,
                                            bowler.getRegistration()
                                                    .getPlayer()
                                                    .getFullName()
                                    )
                    );

            Long inningsId =
                    delivery.getInnings().getId();

            if (delivery.isLegal()) {
                stats.legalBalls++;
            }

            /*
             * Bowler conceded:
             *
             * bat runs
             * + wides
             * + no-balls
             *
             * NOT byes / leg-byes.
             */
            int conceded =
                    delivery.getRunsOffBat()
                    + delivery.getWideRuns()
                    + delivery.getNoBallRuns();

            stats.runsConceded += conceded;

            BowlingFigures figures =
                    stats.inningsFigures
                            .computeIfAbsent(
                                    inningsId,
                                    ignored ->
                                            new BowlingFigures()
                            );

            figures.runs += conceded;
        }

        for (Wicket wicket : wickets) {

            if (!wicket.isCreditedToBowler()) {
                continue;
            }

            PlayingXiEntry bowler =
                    wicket.getDelivery()
                            .getBowler();

            Long playerId =
                    bowler.getRegistration()
                            .getPlayer()
                            .getId();

            BowlerStats stats =
                    map.get(playerId);

            if (stats == null) {
                continue;
            }

            stats.wickets++;

            Long inningsId =
                    wicket.getDelivery()
                            .getInnings()
                            .getId();

            BowlingFigures figures =
                    stats.inningsFigures
                            .computeIfAbsent(
                                    inningsId,
                                    ignored ->
                                            new BowlingFigures()
                            );

            figures.wickets++;
        }

        return map;
    }

    private List<TournamentStatisticsResponse.BattingLeader>
    battingLeaderboard(
            Map<Long, BatterStats> map
    ) {

        List<BatterStats> ordered =
                new ArrayList<>(
                        map.values()
                );

        ordered.sort(
                Comparator
                        .comparingInt(
                                (BatterStats a) -> a.runs
                        )
                        .reversed()
                        .thenComparingInt(
                                a -> a.balls
                        )
        );

        List<TournamentStatisticsResponse.BattingLeader>
                result = new ArrayList<>();

        int rank = 1;

        for (BatterStats a : ordered) {

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

            BigDecimal average =
                    a.dismissals == 0
                            ? null
                            : BigDecimal.valueOf(
                                    (double) a.runs
                                    / a.dismissals
                            ).setScale(
                                    2,
                                    RoundingMode.HALF_UP
                            );

            result.add(
                    new TournamentStatisticsResponse.BattingLeader(
                            rank++,

                            a.playerId,
                            a.playerName,

                            a.inningsPlayed.size(),

                            a.runs,
                            a.balls,

                            a.highestScore,

                            a.fours,
                            a.sixes,

                            a.dismissals,

                            average,
                            strikeRate
                    )
            );
        }

        return result;
    }

    private List<TournamentStatisticsResponse.BowlingLeader>
    bowlingLeaderboard(
            Map<Long, BowlerStats> map
    ) {

        List<BowlerStats> ordered =
                new ArrayList<>(
                        map.values()
                );

        ordered.sort(
                Comparator
                        .comparingInt(
                                (BowlerStats a) -> a.wickets
                        )
                        .reversed()

                        .thenComparing(
                                a -> economy(a)
                        )
        );

        List<TournamentStatisticsResponse.BowlingLeader>
                result = new ArrayList<>();

        int rank = 1;

        for (BowlerStats a : ordered) {

            BigDecimal economy =
                    economy(a);

            BigDecimal average =
                    a.wickets == 0
                            ? null
                            : BigDecimal.valueOf(
                                    (double) a.runsConceded
                                    / a.wickets
                            ).setScale(
                                    2,
                                    RoundingMode.HALF_UP
                            );

            BowlingFigures best =
                    a.inningsFigures.values()
                            .stream()
                            .max(
                                    Comparator
                                            .comparingInt(
                                                    (BowlingFigures f) ->
                                                            f.wickets
                                            )
                                            .thenComparing(
                                                    f -> -f.runs
                                            )
                            )
                            .orElse(
                                    new BowlingFigures()
                            );

            result.add(
                    new TournamentStatisticsResponse.BowlingLeader(
                            rank++,

                            a.playerId,
                            a.playerName,

                            a.wickets,

                            overs(a.legalBalls),

                            a.runsConceded,

                            best.wickets
                                    + "/"
                                    + best.runs,

                            average,
                            economy
                    )
            );
        }

        return result;
    }

    private BigDecimal economy(
            BowlerStats a
    ) {

        if (a.legalBalls == 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(
                a.runsConceded * 6.0
                / a.legalBalls
        ).setScale(
                2,
                RoundingMode.HALF_UP
        );
    }

    private PlayerCareerResponse.BattingCareer
    buildCareerBatting(
            List<Delivery> deliveries,
            int dismissals
    ) {

        int runs = 0;
        int balls = 0;

        int fours = 0;
        int sixes = 0;

        Map<Long, Integer> inningsRuns =
                new HashMap<>();

        for (Delivery delivery : deliveries) {

            int scored =
                    delivery.getRunsOffBat();

            runs += scored;

            if (delivery.getWideRuns() == 0) {
                balls++;
            }

            if (scored == 4) {
                fours++;
            }

            if (scored == 6) {
                sixes++;
            }

            inningsRuns.merge(
                    delivery.getInnings().getId(),
                    scored,
                    Integer::sum
            );
        }

        int highest =
                inningsRuns.values()
                        .stream()
                        .max(Integer::compareTo)
                        .orElse(0);

        BigDecimal average =
                dismissals == 0
                        ? null
                        : BigDecimal.valueOf(
                                (double) runs
                                / dismissals
                        ).setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        BigDecimal strikeRate =
                balls == 0
                        ? BigDecimal.ZERO
                        : BigDecimal.valueOf(
                                runs * 100.0 / balls
                        ).setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        return new PlayerCareerResponse.BattingCareer(
                inningsRuns.size(),
                runs,
                balls,
                highest,
                fours,
                sixes,
                dismissals,
                average,
                strikeRate
        );
    }

    private PlayerCareerResponse.BowlingCareer
    buildCareerBowling(
            List<Delivery> deliveries,
            List<Wicket> wickets
    ) {

        int legalBalls = 0;
        int runsConceded = 0;

        Map<Long, BowlingFigures> figures =
                new HashMap<>();

        for (Delivery delivery : deliveries) {

            if (delivery.isLegal()) {
                legalBalls++;
            }

            int conceded =
                    delivery.getRunsOffBat()
                    + delivery.getWideRuns()
                    + delivery.getNoBallRuns();

            runsConceded += conceded;

            BowlingFigures innings =
                    figures.computeIfAbsent(
                            delivery.getInnings().getId(),
                            ignored ->
                                    new BowlingFigures()
                    );

            innings.runs += conceded;
        }

        for (Wicket wicket : wickets) {

            Long inningsId =
                    wicket.getDelivery()
                            .getInnings()
                            .getId();

            BowlingFigures innings =
                    figures.computeIfAbsent(
                            inningsId,
                            ignored ->
                                    new BowlingFigures()
                    );

            innings.wickets++;
        }

        BowlingFigures best =
                figures.values()
                        .stream()
                        .max(
                                Comparator
                                        .comparingInt(
                                                (BowlingFigures f) ->
                                                        f.wickets
                                        )
                                        .thenComparingInt(
                                                f -> -f.runs
                                        )
                        )
                        .orElse(
                                new BowlingFigures()
                        );

        int totalWickets =
                wickets.size();

        BigDecimal average =
                totalWickets == 0
                        ? null
                        : BigDecimal.valueOf(
                                (double) runsConceded
                                / totalWickets
                        ).setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        BigDecimal economy =
                legalBalls == 0
                        ? BigDecimal.ZERO
                        : BigDecimal.valueOf(
                                runsConceded * 6.0
                                / legalBalls
                        ).setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        return new PlayerCareerResponse.BowlingCareer(
                overs(legalBalls),
                legalBalls,
                runsConceded,
                totalWickets,

                best.wickets
                        + "/"
                        + best.runs,

                average,
                economy
        );
    }

    private String overs(int legalBalls) {

        return (legalBalls / 6)
                + "."
                + (legalBalls % 6);
    }

    private static class BatterStats {

        private final Long playerId;
        private final String playerName;

        private final Set<Long> inningsPlayed =
                new HashSet<>();

        private final Map<Long, Integer>
                runsPerInnings =
                new HashMap<>();

        private int runs;
        private int balls;

        private int fours;
        private int sixes;

        private int dismissals;

        private int highestScore;

        private BatterStats(
                Long playerId,
                String playerName
        ) {
            this.playerId = playerId;
            this.playerName = playerName;
        }
    }

    private static class BowlerStats {

        private final Long playerId;
        private final String playerName;

        private int legalBalls;
        private int runsConceded;
        private int wickets;

        private final Map<Long, BowlingFigures>
                inningsFigures =
                new HashMap<>();

        private BowlerStats(
                Long playerId,
                String playerName
        ) {
            this.playerId = playerId;
            this.playerName = playerName;
        }
    }

    private static class BowlingFigures {

        private int runs;
        private int wickets;
    }
}
