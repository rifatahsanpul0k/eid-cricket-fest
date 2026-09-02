package com.eidcricketfest.player.service;

import com.eidcricketfest.common.exception.ResourceNotFoundException;
import com.eidcricketfest.match.entity.CricketMatch;
import com.eidcricketfest.match.repository.CricketMatchRepository;
import com.eidcricketfest.match.repository.PlayingXiEntryRepository;
import com.eidcricketfest.player.dto.MyEditionStatisticsResponse;
import com.eidcricketfest.player.dto.MyMatchResponse;
import com.eidcricketfest.player.dto.MyTeamResponse;
import com.eidcricketfest.player.entity.Player;
import com.eidcricketfest.player.repository.PlayerRepository;
import com.eidcricketfest.registration.entity.PlayerRegistration;
import com.eidcricketfest.statistics.service.StatisticsService;
import com.eidcricketfest.team.entity.RosterEntryStatus;
import com.eidcricketfest.team.entity.TeamRosterEntry;
import com.eidcricketfest.team.entity.TournamentTeam;
import com.eidcricketfest.team.repository.TeamRosterEntryRepository;
import com.eidcricketfest.tournament.repository.TournamentEditionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class MyCricketService {

    private final PlayerRepository playerRepository;
    private final TournamentEditionRepository editionRepository;
    private final TeamRosterEntryRepository rosterRepository;
    private final CricketMatchRepository matchRepository;
    private final PlayingXiEntryRepository playingXiRepository;
    private final StatisticsService statisticsService;

    public MyCricketService(
            PlayerRepository playerRepository,
            TournamentEditionRepository editionRepository,
            TeamRosterEntryRepository rosterRepository,
            CricketMatchRepository matchRepository,
            PlayingXiEntryRepository playingXiRepository,
            StatisticsService statisticsService
    ) {
        this.playerRepository = playerRepository;
        this.editionRepository = editionRepository;
        this.rosterRepository = rosterRepository;
        this.matchRepository = matchRepository;
        this.playingXiRepository = playingXiRepository;
        this.statisticsService = statisticsService;
    }

    public MyTeamResponse getMyTeam(
            Long userId,
            Long editionId
    ) {

        verifyEdition(editionId);

        TeamRosterEntry mine =
                rosterRepository
                        .findMineDetailedByEditionIdAndUserId(
                                editionId,
                                userId,
                                RosterEntryStatus.ACTIVE
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Player is not assigned to a team"
                                )
                        );

        TournamentTeam tournamentTeam = mine.getTournamentTeam();

        List<TeamRosterEntry> squad =
                rosterRepository.findActiveDetailedByTournamentTeamId(
                        tournamentTeam.getId(),
                        RosterEntryStatus.ACTIVE
                );

        PlayerRegistration captainRegistration =
                tournamentTeam.getCaptainRegistration();

        Long captainRegistrationId =
                captainRegistration != null
                        ? captainRegistration.getId()
                        : null;

        return new MyTeamResponse(
                editionId,
                tournamentTeam.getId(),
                tournamentTeam.getTeam().getId(),
                tournamentTeam.getTeam().getName(),
                tournamentTeam.getTeam().getShortName(),
                tournamentTeam.getTeam().getLogoUrl(),
                captain(captainRegistration),
                me(mine, captainRegistrationId),
                squad.stream()
                        .map(entry -> squadMember(
                                entry,
                                captainRegistrationId
                        ))
                        .toList()
        );
    }

    public List<MyMatchResponse> getMyMatches(
            Long userId,
            Long editionId
    ) {

        verifyEdition(editionId);
        Player player = findPlayer(userId);

        var mine =
                rosterRepository
                        .findMineDetailedByEditionIdAndUserId(
                                editionId,
                                userId,
                                RosterEntryStatus.ACTIVE
                        );

        if (mine.isEmpty()) {
            return List.of();
        }

        Long tournamentTeamId =
                mine.get()
                        .getTournamentTeam()
                        .getId();

        List<CricketMatch> matches =
                matchRepository.findDetailedByEditionAndTeamId(
                        editionId,
                        tournamentTeamId
                );

        Set<Long> playingXiMatchIds =
                playingXiRepository
                        .findMatchIdsByEditionIdAndPlayerId(
                                editionId,
                                player.getId()
                        )
                        .stream()
                        .collect(Collectors.toSet());

        Map<Long, Map<Long, Long>> submittedByMatchAndTeam =
                submittedByMatchAndTeam(
                        matches.stream()
                                .map(CricketMatch::getId)
                                .toList()
                );

        return matches.stream()
                .map(match -> toMyMatch(
                        match,
                        tournamentTeamId,
                        playingXiMatchIds,
                        submittedByMatchAndTeam
                ))
                .toList();
    }

    public MyEditionStatisticsResponse getMyStatistics(
            Long userId,
            Long editionId
    ) {

        verifyEdition(editionId);
        Player player = findPlayer(userId);

        return statisticsService.playerEditionStatistics(
                editionId,
                player.getId()
        );
    }

    private Player findPlayer(Long userId) {
        return playerRepository.findByUser_Id(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Player profile not found"
                        )
                );
    }

    private void verifyEdition(Long editionId) {
        if (!editionRepository.existsById(editionId)) {
            throw new ResourceNotFoundException(
                    "Tournament edition not found"
            );
        }
    }

    private Map<Long, Map<Long, Long>> submittedByMatchAndTeam(
            List<Long> matchIds
    ) {

        if (matchIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Map<Long, Long>> counts = new HashMap<>();

        for (Object[] row :
                playingXiRepository.countSubmittedByMatchAndTeam(matchIds)) {

            Long matchId = (Long) row[0];
            Long matchSideId = (Long) row[1];
            Long count = (Long) row[2];

            counts.computeIfAbsent(
                            matchId,
                            ignored -> new HashMap<>()
                    )
                    .put(matchSideId, count);
        }

        return counts;
    }

    private MyMatchResponse toMyMatch(
            CricketMatch match,
            Long myTournamentTeamId,
            Set<Long> playingXiMatchIds,
            Map<Long, Map<Long, Long>> submittedByMatchAndTeam
    ) {

        TournamentTeam opponent =
                match.getTeamA().getId().equals(myTournamentTeamId)
                        ? match.getTeamB()
                        : match.getTeamA();

        Long myMatchSideId =
                match.sideForTournamentTeam(
                                match.getTeamA().getId().equals(myTournamentTeamId)
                                        ? match.getTeamA()
                                        : match.getTeamB()
                        )
                        .getId();

        long submitted =
                submittedByMatchAndTeam
                        .getOrDefault(match.getId(), Map.of())
                        .getOrDefault(myMatchSideId, 0L);

        boolean myTeamPlayingXiSubmitted =
                submitted >= match.getTournamentEdition().getPlayingXiSize();

        return new MyMatchResponse(
                match.getId(),
                match.getMatchNumber(),
                match.getRoundNumber(),
                match.getStage(),
                match.getStatus(),
                match.getScheduledAt(),
                match.getOversPerInnings(),
                venue(match),
                team(match.getTeamA()),
                team(match.getTeamB()),
                myTournamentTeamId,
                team(opponent),
                playingXiMatchIds.contains(match.getId()),
                myTeamPlayingXiSubmitted,
                match.getWinnerTeam() != null
                        ? match.getWinnerTeam().getId()
                        : null,
                match.getResultType(),
                match.getResultSummary()
        );
    }

    private MyTeamResponse.Captain captain(
            PlayerRegistration captainRegistration
    ) {

        if (captainRegistration == null) {
            return null;
        }

        return new MyTeamResponse.Captain(
                captainRegistration.getId(),
                captainRegistration.getPlayer().getId(),
                captainRegistration.getPlayer().getFullName()
        );
    }

    private MyTeamResponse.Me me(
            TeamRosterEntry mine,
            Long captainRegistrationId
    ) {

        PlayerRegistration registration =
                mine.getPlayerRegistration();

        return new MyTeamResponse.Me(
                registration.getId(),
                registration.getPlayer().getId(),
                registration.getPlayer().getFullName(),
                mine.getAcquisitionType(),
                mine.getJerseyNumber(),
                registration.getId().equals(captainRegistrationId)
        );
    }

    private MyTeamResponse.SquadMember squadMember(
            TeamRosterEntry entry,
            Long captainRegistrationId
    ) {

        PlayerRegistration registration =
                entry.getPlayerRegistration();

        return new MyTeamResponse.SquadMember(
                registration.getId(),
                registration.getPlayer().getId(),
                registration.getPlayer().getFullName(),
                registration.getPlayer().getPhotoUrl(),
                registration.getPlayer().getPrimaryCategory() != null
                        ? registration.getPlayer()
                        .getPrimaryCategory()
                        .getName()
                        : registration.getCategory().getName(),
                entry.getAcquisitionType(),
                entry.getJerseyNumber(),
                registration.getId().equals(captainRegistrationId)
        );
    }

    private MyMatchResponse.Venue venue(CricketMatch match) {
        return match.getVenue() != null
                ? new MyMatchResponse.Venue(
                        match.getVenue().getId(),
                        match.getVenue().getName()
                )
                : null;
    }

    private MyMatchResponse.Team team(TournamentTeam tournamentTeam) {
        return new MyMatchResponse.Team(
                tournamentTeam.getId(),
                tournamentTeam.getTeam().getName()
        );
    }
}
