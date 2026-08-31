package com.eidcricketfest.history.service;

import com.eidcricketfest.award.dto.PlayerAwardResponse;
import com.eidcricketfest.award.service.AwardService;
import com.eidcricketfest.common.exception.ResourceNotFoundException;
import com.eidcricketfest.history.dto.TournamentHistoryResponse;
import com.eidcricketfest.match.entity.CricketMatch;
import com.eidcricketfest.match.repository.CricketMatchRepository;
import com.eidcricketfest.statistics.dto.TournamentStatisticsResponse;
import com.eidcricketfest.statistics.service.StatisticsService;
import com.eidcricketfest.team.entity.TournamentTeam;
import com.eidcricketfest.tournament.entity.*;
import com.eidcricketfest.tournament.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional(readOnly = true)
public class TournamentHistoryService {

    private final TournamentRepository tournamentRepository;
    private final TournamentEditionRepository editionRepository;
    private final CricketMatchRepository matchRepository;
    private final StatisticsService statisticsService;
    private final AwardService awardService;

    public TournamentHistoryService(
            TournamentRepository tournamentRepository,
            TournamentEditionRepository editionRepository,
            CricketMatchRepository matchRepository,
            StatisticsService statisticsService,
            AwardService awardService
    ) {
        this.tournamentRepository = tournamentRepository;
        this.editionRepository = editionRepository;
        this.matchRepository = matchRepository;
        this.statisticsService = statisticsService;
        this.awardService = awardService;
    }

    public TournamentHistoryResponse history(
            Long tournamentId
    ) {

        Tournament tournament =
                tournamentRepository.findById(tournamentId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Tournament not found"
                                )
                        );

        List<TournamentEdition> editions =
                editionRepository
                        .findByTournament_IdAndStatusOrderByEndDateDesc(
                                tournamentId,
                                TournamentEditionStatus.COMPLETED
                        );

        List<TournamentHistoryResponse.Edition> history =
                editions.stream()
                        .map(this::editionHistory)
                        .toList();

        return new TournamentHistoryResponse(
                tournament.getId(),
                tournament.getName(),
                history
        );
    }

    private TournamentHistoryResponse.Edition editionHistory(
            TournamentEdition edition
    ) {

        CricketMatch finalMatch =
                matchRepository
                        .findFinalByEditionId(
                                edition.getId()
                        )
                        .orElse(null);

        TournamentHistoryResponse.Team champion = null;
        TournamentHistoryResponse.Team runnerUp = null;

        if (finalMatch != null
                && finalMatch.getWinnerTeam() != null) {

            TournamentTeam winner =
                    finalMatch.getWinnerTeam();

            TournamentTeam loser =
                    finalMatch.getTeamA()
                            .getId()
                            .equals(winner.getId())
                            ? finalMatch.getTeamB()
                            : finalMatch.getTeamA();

            champion =
                    new TournamentHistoryResponse.Team(
                            winner.getId(),
                            winner.getTeam().getName()
                    );

            runnerUp =
                    new TournamentHistoryResponse.Team(
                            loser.getId(),
                            loser.getTeam().getName()
                    );
        }

        TournamentStatisticsResponse statistics =
                statisticsService.statistics(
                        edition.getId()
                );

        TournamentHistoryResponse.PlayerStat topBatter =
                statistics.batting().isEmpty()
                        ? null
                        : new TournamentHistoryResponse.PlayerStat(
                                statistics.batting()
                                        .get(0)
                                        .playerId(),

                                statistics.batting()
                                        .get(0)
                                        .playerName(),

                                statistics.batting()
                                        .get(0)
                                        .runs()
                        );

        TournamentHistoryResponse.PlayerStat topBowler =
                statistics.bowling().isEmpty()
                        ? null
                        : new TournamentHistoryResponse.PlayerStat(
                                statistics.bowling()
                                        .get(0)
                                        .playerId(),

                                statistics.bowling()
                                        .get(0)
                                        .playerName(),

                                statistics.bowling()
                                        .get(0)
                                        .wickets()
                        );

        List<PlayerAwardResponse> awards =
                awardService.getAwards(
                        edition.getId()
                );

        return new TournamentHistoryResponse.Edition(
                edition.getId(),
                edition.getName(),

                edition.getStartDate(),
                edition.getEndDate(),

                champion,
                runnerUp,

                finalMatch == null
                        ? null
                        : finalMatch.getResultSummary(),

                topBatter,
                topBowler,
                awards
        );
    }
}
