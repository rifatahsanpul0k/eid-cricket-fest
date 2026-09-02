package com.eidcricketfest.history.service;

import com.eidcricketfest.award.dto.PlayerAwardResponse;
import com.eidcricketfest.award.service.AwardService;
import com.eidcricketfest.common.exception.ResourceNotFoundException;
import com.eidcricketfest.history.dto.TournamentHistoryResponse;
import com.eidcricketfest.match.entity.CricketMatch;
import com.eidcricketfest.statistics.dto.TournamentStatisticsResponse;
import com.eidcricketfest.statistics.service.StatisticsService;
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
    private final StatisticsService statisticsService;
    private final AwardService awardService;

    public TournamentHistoryService(
            TournamentRepository tournamentRepository,
            TournamentEditionRepository editionRepository,
            StatisticsService statisticsService,
            AwardService awardService
    ) {
        this.tournamentRepository = tournamentRepository;
        this.editionRepository = editionRepository;
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
                        .findDetailedByTournamentIdAndStatusOrderByEndDateDesc(
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

        TournamentHistoryResponse.Team champion = null;
        TournamentHistoryResponse.Team runnerUp = null;

        CricketMatch finalMatch =
                edition.getFinalMatch();

        if (edition.getChampionTeam() != null) {
            champion =
                    new TournamentHistoryResponse.Team(
                            edition.getChampionTeam().getId(),
                            edition.getChampionTeam()
                                    .getTeam()
                                    .getName()
                    );
        }

        if (edition.getRunnerUpTeam() != null) {
            runnerUp =
                    new TournamentHistoryResponse.Team(
                            edition.getRunnerUpTeam().getId(),
                            edition.getRunnerUpTeam()
                                    .getTeam()
                                    .getName()
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
