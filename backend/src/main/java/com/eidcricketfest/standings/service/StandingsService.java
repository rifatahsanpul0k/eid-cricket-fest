package com.eidcricketfest.standings.service;

import com.eidcricketfest.common.exception.ResourceNotFoundException;
import com.eidcricketfest.match.entity.*;
import com.eidcricketfest.match.repository.CricketMatchRepository;
import com.eidcricketfest.scoring.entity.Innings;
import com.eidcricketfest.scoring.repository.InningsRepository;
import com.eidcricketfest.standings.dto.StandingsResponse;
import com.eidcricketfest.team.entity.TournamentTeam;
import com.eidcricketfest.team.repository.TournamentTeamRepository;
import com.eidcricketfest.tournament.entity.TournamentEdition;
import com.eidcricketfest.tournament.repository.TournamentEditionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class StandingsService {

    private final TournamentEditionRepository editionRepository;
    private final TournamentTeamRepository tournamentTeamRepository;
    private final CricketMatchRepository matchRepository;
    private final InningsRepository inningsRepository;

    public StandingsService(
            TournamentEditionRepository editionRepository,
            TournamentTeamRepository tournamentTeamRepository,
            CricketMatchRepository matchRepository,
            InningsRepository inningsRepository
    ) {
        this.editionRepository = editionRepository;
        this.tournamentTeamRepository = tournamentTeamRepository;
        this.matchRepository = matchRepository;
        this.inningsRepository = inningsRepository;
    }

    public StandingsResponse getStandings(
            Long editionId
    ) {

        TournamentEdition edition =
                editionRepository.findById(editionId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Tournament edition not found"
                                )
                        );

        List<TournamentTeam> teams =
                tournamentTeamRepository
                        .findDetailedByEditionId(editionId);

        Map<Long, StandingAccumulator> table =
                new LinkedHashMap<>();

        for (TournamentTeam team : teams) {

            table.put(
                    team.getId(),
                    new StandingAccumulator(team)
            );
        }

        List<CricketMatch> matches =
                matchRepository.findLeagueResults(
                        editionId
                );

        for (CricketMatch match : matches) {

            StandingAccumulator teamA =
                    table.get(
                            match.getTeamA().getId()
                    );

            StandingAccumulator teamB =
                    table.get(
                            match.getTeamB().getId()
                    );

            if (teamA == null || teamB == null) {
                continue;
            }

            applyResult(
                    match,
                    edition,
                    teamA,
                    teamB
            );

            /*
             * No-result matches do not contribute
             * to Net Run Rate.
             */
            if (match.getResultType()
                    != MatchResultType.NO_RESULT
                    && match.getResultType()
                    != MatchResultType.FORFEIT) {

                applyNrrData(
                        match,
                        teamA,
                        teamB
                );
            }
        }

        List<StandingAccumulator> ordered =
                new ArrayList<>(table.values());

        ordered.forEach(this::calculateNrr);

        /*
         * Default ordering:
         *
         * 1. Points
         * 2. Wins
         * 3. NRR
         * 4. Team name
         */
        ordered.sort(
                Comparator
                        .comparing(
                                StandingAccumulator::points
                        )
                        .reversed()

                        .thenComparing(
                                StandingAccumulator::wins,
                                Comparator.reverseOrder()
                        )

                        .thenComparing(
                                StandingAccumulator::nrr,
                                Comparator.reverseOrder()
                        )

                        .thenComparing(
                                a -> a.team
                                        .getTeam()
                                        .getName()
                        )
        );

        List<StandingsResponse.Row> rows =
                new ArrayList<>();

        int rank = 1;

        for (StandingAccumulator a : ordered) {

            rows.add(
                    new StandingsResponse.Row(
                            rank++,

                            a.team.getId(),

                            a.team.getTeam().getId(),

                            a.team.getTeam().getName(),
                            a.team.getTeam().getShortName(),

                            a.played,
                            a.wins,
                            a.losses,
                            a.ties,
                            a.noResults,

                            a.points,

                            a.nrr,
                            a.runRateFor,
                            a.runRateAgainst
                    )
            );
        }

        return new StandingsResponse(
                editionId,
                rows
        );
    }

    private void applyResult(
            CricketMatch match,
            TournamentEdition edition,
            StandingAccumulator a,
            StandingAccumulator b
    ) {

        a.played++;
        b.played++;

        switch (match.getResultType()) {

            case RUNS, WICKETS, TIEBREAKER, FORFEIT -> {

                TournamentTeam winner =
                        match.getWinnerTeam();

                if (winner == null) {
                    throw new IllegalStateException(
                            "Completed match has no winner"
                    );
                }

                StandingAccumulator winning =
                        winner.getId()
                                .equals(a.team.getId())
                                ? a
                                : b;

                StandingAccumulator losing =
                        winning == a
                                ? b
                                : a;

                winning.wins++;
                losing.losses++;

                winning.points =
                        winning.points.add(
                                edition.getWinPoints()
                        );

                losing.points =
                        losing.points.add(
                                edition.getLossPoints()
                        );
            }

            case TIE -> {

                a.ties++;
                b.ties++;

                a.points =
                        a.points.add(
                                edition.getTiePoints()
                        );

                b.points =
                        b.points.add(
                                edition.getTiePoints()
                        );
            }

            case NO_RESULT -> {

                a.noResults++;
                b.noResults++;

                a.points =
                        a.points.add(
                                edition.getNoResultPoints()
                        );

                b.points =
                        b.points.add(
                                edition.getNoResultPoints()
                        );
            }
        }
    }

    private void applyNrrData(
            CricketMatch match,
            StandingAccumulator teamA,
            StandingAccumulator teamB
    ) {

        List<Innings> inningsList =
                inningsRepository
                        .findByMatch_IdOrderByInningsNumber(
                                match.getId()
                        );

        /*
         * A normal result requires both innings.
         */
        if (inningsList.size() < 2) {
            return;
        }

        for (Innings innings : inningsList) {

            StandingAccumulator batting =
                    innings.getBattingTeam()
                            .getId()
                            .equals(teamA.team.getId())
                            ? teamA
                            : teamB;

            StandingAccumulator bowling =
                    batting == teamA
                            ? teamB
                            : teamA;

            int effectiveBalls =
                    effectiveBallsForNrr(
                            innings
                    );

            batting.runsFor +=
                    innings.getTotalRuns();

            batting.ballsFaced +=
                    effectiveBalls;

            bowling.runsAgainst +=
                    innings.getTotalRuns();

            bowling.ballsBowled +=
                    effectiveBalls;
        }
    }

    private int effectiveBallsForNrr(
            Innings innings
    ) {

        int maximumBalls =
                innings.getMatch()
                        .getOversPerInnings()
                        * 6;

        int maximumWickets =
                innings.getMatch()
                        .getTournamentEdition()
                        .getPlayingXiSize()
                        - 1;

        boolean allOut =
                innings.getWickets()
                        >= maximumWickets;

        /*
         * ICC-style NRR:
         * all out early -> treat as full quota.
         */
        if (allOut
                && innings.getLegalBalls()
                < maximumBalls) {

            return maximumBalls;
        }

        return innings.getLegalBalls();
    }

    private void calculateNrr(
            StandingAccumulator a
    ) {

        a.runRateFor =
                rate(
                        a.runsFor,
                        a.ballsFaced
                );

        a.runRateAgainst =
                rate(
                        a.runsAgainst,
                        a.ballsBowled
                );

        a.nrr =
                a.runRateFor
                        .subtract(
                                a.runRateAgainst
                        )
                        .setScale(
                                3,
                                RoundingMode.HALF_UP
                        );
    }

    private BigDecimal rate(
            int runs,
            int balls
    ) {

        if (balls == 0) {
            return BigDecimal.ZERO
                    .setScale(3);
        }

        return BigDecimal.valueOf(runs)
                .multiply(
                        BigDecimal.valueOf(6)
                )
                .divide(
                        BigDecimal.valueOf(balls),
                        3,
                        RoundingMode.HALF_UP
                );
    }

    private static class StandingAccumulator {

        private final TournamentTeam team;

        private int played;
        private int wins;
        private int losses;
        private int ties;
        private int noResults;

        private BigDecimal points =
                BigDecimal.ZERO;

        private int runsFor;
        private int ballsFaced;

        private int runsAgainst;
        private int ballsBowled;

        private BigDecimal runRateFor =
                BigDecimal.ZERO;

        private BigDecimal runRateAgainst =
                BigDecimal.ZERO;

        private BigDecimal nrr =
                BigDecimal.ZERO;

        private StandingAccumulator(
                TournamentTeam team
        ) {
            this.team = team;
        }

        private BigDecimal points() {
            return points;
        }

        private Integer wins() {
            return wins;
        }

        private BigDecimal nrr() {
            return nrr;
        }
    }
}
