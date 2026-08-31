package com.eidcricketfest.knockout.service;

import com.eidcricketfest.common.exception.*;
import com.eidcricketfest.knockout.dto.KnockoutBracketResponse;
import com.eidcricketfest.match.entity.*;
import com.eidcricketfest.match.repository.CricketMatchRepository;
import com.eidcricketfest.standings.dto.StandingsResponse;
import com.eidcricketfest.standings.service.StandingsService;
import com.eidcricketfest.team.entity.TournamentTeam;
import com.eidcricketfest.team.repository.TournamentTeamRepository;
import com.eidcricketfest.tournament.entity.TournamentEdition;
import com.eidcricketfest.tournament.repository.TournamentEditionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class KnockoutService {

    private final TournamentEditionRepository editionRepository;
    private final TournamentTeamRepository tournamentTeamRepository;
    private final CricketMatchRepository matchRepository;
    private final StandingsService standingsService;

    public KnockoutService(
            TournamentEditionRepository editionRepository,
            TournamentTeamRepository tournamentTeamRepository,
            CricketMatchRepository matchRepository,
            StandingsService standingsService
    ) {
        this.editionRepository = editionRepository;
        this.tournamentTeamRepository = tournamentTeamRepository;
        this.matchRepository = matchRepository;
        this.standingsService = standingsService;
    }

    public KnockoutBracketResponse generateSemiFinals(
            Long editionId
    ) {

        TournamentEdition edition =
                editionRepository
                        .findByIdForUpdate(editionId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Tournament edition not found"
                                )
                        );

        if (matchRepository
                .existsByTournamentEdition_IdAndStage(
                        editionId,
                        MatchStage.SEMI_FINAL
                )) {

            throw new ConflictException(
                    "Semi-finals already exist"
            );
        }

        long leagueMatches =
                matchRepository
                        .countByTournamentEdition_IdAndStage(
                                editionId,
                                MatchStage.LEAGUE
                        );

        if (leagueMatches == 0) {
            throw new ConflictException(
                    "No league fixtures exist"
            );
        }

        long finishedLeagueMatches =
                matchRepository
                        .countByTournamentEdition_IdAndStageAndResultTypeIsNotNull(
                                editionId,
                                MatchStage.LEAGUE
                        );

        if (finishedLeagueMatches != leagueMatches) {

            throw new ConflictException(
                    "All league matches must finish before semi-finals are generated"
            );
        }

        StandingsResponse standings =
                standingsService.getStandings(
                        editionId
                );

        if (standings.standings().size() < 4) {
            throw new ConflictException(
                    "At least four teams are required for semi-finals"
            );
        }

        List<StandingsResponse.Row> topFour =
                standings.standings()
                        .stream()
                        .limit(4)
                        .toList();

        Map<Long, TournamentTeam> teams =
                tournamentTeamRepository
                        .findDetailedByEditionId(editionId)
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        TournamentTeam::getId,
                                        Function.identity()
                                )
                        );

        TournamentTeam first =
                requireTeam(
                        teams,
                        topFour.get(0)
                                .tournamentTeamId()
                );

        TournamentTeam second =
                requireTeam(
                        teams,
                        topFour.get(1)
                                .tournamentTeamId()
                );

        TournamentTeam third =
                requireTeam(
                        teams,
                        topFour.get(2)
                                .tournamentTeamId()
                );

        TournamentTeam fourth =
                requireTeam(
                        teams,
                        topFour.get(3)
                                .tournamentTeamId()
                );

        int matchNumber =
                matchRepository
                        .findMaxMatchNumber(editionId)
                        + 1;

        CricketMatch semiFinalOne =
                new CricketMatch(
                        edition,
                        first,
                        fourth,
                        MatchStage.SEMI_FINAL,
                        1,
                        matchNumber++,
                        edition.getOversPerInnings(),
                        null
                );

        semiFinalOne.setQualificationSeeds(
                1,
                4
        );

        CricketMatch semiFinalTwo =
                new CricketMatch(
                        edition,
                        second,
                        third,
                        MatchStage.SEMI_FINAL,
                        1,
                        matchNumber,
                        edition.getOversPerInnings(),
                        null
                );

        semiFinalTwo.setQualificationSeeds(
                2,
                3
        );

        matchRepository.saveAll(
                List.of(
                        semiFinalOne,
                        semiFinalTwo
                )
        );

        return getBracket(editionId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean generateFinalIfReady(
            Long editionId
    ) {

        TournamentEdition edition =
                editionRepository
                        .findByIdForUpdate(editionId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Tournament edition not found"
                                )
                        );

        List<CricketMatch> existingFinals =
                matchRepository
                        .findDetailedByEditionAndStage(
                                editionId,
                                MatchStage.FINAL
                        );

        if (!existingFinals.isEmpty()) {
            return false;
        }

        List<CricketMatch> semiFinals =
                matchRepository
                        .findDetailedByEditionAndStage(
                                editionId,
                                MatchStage.SEMI_FINAL
                        );

        if (semiFinals.size() != 2) {
            return false;
        }

        CricketMatch semiOne =
                semiFinals.get(0);

        CricketMatch semiTwo =
                semiFinals.get(1);

        if (semiOne.getWinnerTeam() == null
                || semiTwo.getWinnerTeam() == null) {

            return false;
        }

        int matchNumber =
                matchRepository
                        .findMaxMatchNumber(editionId)
                        + 1;

        CricketMatch finalMatch =
                new CricketMatch(
                        edition,

                        semiOne.getWinnerTeam(),
                        semiTwo.getWinnerTeam(),

                        MatchStage.FINAL,

                        1,

                        matchNumber,

                        edition.getOversPerInnings(),

                        null
                );

        finalMatch.setSourceMatches(
                semiOne,
                semiTwo
        );

        matchRepository.save(finalMatch);

        return true;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeEditionFromFinal(
            Long matchId
    ) {

        CricketMatch match =
                matchRepository
                        .findDetailedById(matchId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Final match not found"
                                )
                        );

        if (match.getStage()
                != MatchStage.FINAL) {
            return;
        }

        if (match.getWinnerTeam() == null) {
            return;
        }

        TournamentEdition edition =
                editionRepository
                        .findByIdForUpdate(
                                match.getTournamentEdition()
                                        .getId()
                        )
                        .orElseThrow();

        edition.markCompleted();
    }

    @Transactional(readOnly = true)
    public KnockoutBracketResponse getBracket(
            Long editionId
    ) {

        if (!editionRepository.existsById(editionId)) {
            throw new ResourceNotFoundException(
                    "Tournament edition not found"
            );
        }

        List<CricketMatch> semiFinals =
                matchRepository
                        .findDetailedByEditionAndStage(
                                editionId,
                                MatchStage.SEMI_FINAL
                        );

        List<CricketMatch> finals =
                matchRepository
                        .findDetailedByEditionAndStage(
                                editionId,
                                MatchStage.FINAL
                        );

        CricketMatch finalMatch =
                finals.isEmpty()
                        ? null
                        : finals.get(0);

        return new KnockoutBracketResponse(
                editionId,

                semiFinals.stream()
                        .map(this::toMatchInfo)
                        .toList(),

                finalMatch == null
                        ? null
                        : toMatchInfo(finalMatch)
        );
    }

    private TournamentTeam requireTeam(
            Map<Long, TournamentTeam> teams,
            Long id
    ) {

        TournamentTeam team =
                teams.get(id);

        if (team == null) {
            throw new IllegalStateException(
                    "Qualified tournament team does not exist"
            );
        }

        return team;
    }

    private KnockoutBracketResponse.MatchInfo toMatchInfo(
            CricketMatch match
    ) {

        return new KnockoutBracketResponse.MatchInfo(
                match.getId(),
                match.getMatchNumber(),

                match.getStage(),
                match.getStatus(),

                new KnockoutBracketResponse.TeamInfo(
                        match.getTeamA().getId(),
                        match.getTeamA()
                                .getTeam()
                                .getName(),
                        match.getTeamASeed()
                ),

                new KnockoutBracketResponse.TeamInfo(
                        match.getTeamB().getId(),
                        match.getTeamB()
                                .getTeam()
                                .getName(),
                        match.getTeamBSeed()
                ),

                match.getWinnerTeam() == null
                        ? null
                        : new KnockoutBracketResponse.TeamInfo(
                                match.getWinnerTeam().getId(),
                                match.getWinnerTeam()
                                        .getTeam()
                                        .getName(),
                                null
                        ),

                match.getSourceMatchA() == null
                        ? null
                        : match.getSourceMatchA().getId(),

                match.getSourceMatchB() == null
                        ? null
                        : match.getSourceMatchB().getId()
        );
    }
}
