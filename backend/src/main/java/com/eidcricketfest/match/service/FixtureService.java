package com.eidcricketfest.match.service;

import com.eidcricketfest.common.dto.PageResponse;
import com.eidcricketfest.common.exception.*;
import com.eidcricketfest.common.web.PageableFactory;
import com.eidcricketfest.match.dto.*;
import com.eidcricketfest.match.entity.*;
import com.eidcricketfest.match.repository.*;
import com.eidcricketfest.team.entity.TournamentTeam;
import com.eidcricketfest.team.repository.TournamentTeamRepository;
import com.eidcricketfest.tournament.entity.TournamentEdition;
import com.eidcricketfest.tournament.repository.TournamentEditionRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
public class FixtureService {

    private static final Map<String, String> MATCH_SORTS =
            Map.of(
                    "matchNumber",
                    "matchNumber",

                    "scheduledAt",
                    "scheduledAt",

                    "stage",
                    "stage",

                    "createdAt",
                    "createdAt"
            );

    private final CricketMatchRepository matchRepository;
    private final VenueRepository venueRepository;
    private final TournamentEditionRepository editionRepository;
    private final TournamentTeamRepository tournamentTeamRepository;
    private final PageableFactory pageableFactory;

    public FixtureService(
            CricketMatchRepository matchRepository,
            VenueRepository venueRepository,
            TournamentEditionRepository editionRepository,
            TournamentTeamRepository tournamentTeamRepository,
            PageableFactory pageableFactory
    ) {
        this.matchRepository = matchRepository;
        this.venueRepository = venueRepository;
        this.editionRepository = editionRepository;
        this.tournamentTeamRepository = tournamentTeamRepository;
        this.pageableFactory = pageableFactory;
    }

    public List<MatchResponse> generateRoundRobin(
            Long editionId,
            GenerateRoundRobinRequest request
    ) {

        TournamentEdition edition =
                editionRepository.findById(editionId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Tournament edition not found"
                                )
                        );

        if (matchRepository
                .existsByTournamentEdition_IdAndStage(
                        editionId,
                        MatchStage.LEAGUE
                )) {

            throw new ConflictException(
                    "League fixtures already exist"
            );
        }

        List<TournamentTeam> teams =
                new ArrayList<>(
                        tournamentTeamRepository
                                .findDetailedByEditionId(editionId)
                );

        if (teams.size() < 2) {
            throw new ConflictException(
                    "At least two teams are required"
            );
        }

        Venue venue = null;

        if (request.venueId() != null) {
            venue = venueRepository
                    .findById(request.venueId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Venue not found"
                            )
                    );
        }

        /*
         * Odd number of teams:
         * add null as a BYE slot.
         */
        if (teams.size() % 2 != 0) {
            teams.add(null);
        }

        int size = teams.size();
        int rounds = size - 1;

        int matchNumber =
                matchRepository.findMaxMatchNumber(editionId) + 1;

        List<CricketMatch> generated =
                new ArrayList<>();

        for (int round = 1; round <= rounds; round++) {

            for (int i = 0; i < size / 2; i++) {

                TournamentTeam teamA = teams.get(i);
                TournamentTeam teamB =
                        teams.get(size - 1 - i);

                /*
                 * BYE
                 */
                if (teamA == null || teamB == null) {
                    continue;
                }

                CricketMatch match =
                        new CricketMatch(
                                edition,
                                teamA,
                                teamB,
                                MatchStage.LEAGUE,
                                round,
                                matchNumber++,
                                edition.getOversPerInnings(),
                                venue
                        );

                generated.add(match);
            }

            /*
             * Circle method:
             * first team fixed,
             * rotate the remaining teams.
             */
            TournamentTeam last =
                    teams.remove(size - 1);

            teams.add(1, last);
        }

        return matchRepository.saveAll(generated)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public MatchResponse scheduleMatch(
            Long matchId,
            ScheduleMatchRequest request
    ) {

        CricketMatch match =
                findMatch(matchId);

        Venue venue =
                venueRepository.findById(
                                request.venueId()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Venue not found"
                                )
                        );

        match.schedule(
                request.scheduledAt(),
                venue
        );

        return toResponse(match);
    }

    @Transactional(readOnly = true)
    public List<MatchResponse> getEditionMatches(
            Long editionId
    ) {

        return matchRepository
                .findDetailedByEditionId(editionId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<MatchResponse> searchMatches(
            Long editionId,
            MatchStatus status,
            MatchStage stage,
            Long teamId,
            Integer page,
            Integer size,
            String sortBy,
            String direction
    ) {

        Pageable pageable =
                pageableFactory.create(
                        page,
                        size,
                        sortBy,
                        direction,
                        MATCH_SORTS,
                        "matchNumber"
                );

        Specification<CricketMatch> spec =
                Specification.allOf(
                        CricketMatchSpecifications
                                .edition(editionId),

                        CricketMatchSpecifications
                                .status(status),

                        CricketMatchSpecifications
                                .stage(stage),

                        CricketMatchSpecifications
                                .involvesTeam(teamId)
                );

        var matches =
                matchRepository
                        .findAll(
                                spec,
                                pageable
                        )
                        .map(this::toResponse);

        return PageResponse.from(matches);
    }

    private CricketMatch findMatch(Long id) {

        return matchRepository
                .findDetailedById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Match not found"
                        )
                );
    }

    private MatchResponse toResponse(
            CricketMatch match
    ) {

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
                match.getScheduledAt()
        );
    }
}
