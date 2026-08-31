package com.eidcricketfest.match.service;

import com.eidcricketfest.common.exception.*;
import com.eidcricketfest.match.dto.NoResultRequest;
import com.eidcricketfest.match.dto.ResolveKnockoutMatchRequest;
import com.eidcricketfest.match.entity.*;
import com.eidcricketfest.match.event.MatchCompletedEvent;
import com.eidcricketfest.match.repository.CricketMatchRepository;
import com.eidcricketfest.team.entity.TournamentTeam;
import com.eidcricketfest.team.repository.TournamentTeamRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MatchResultService {

    private final CricketMatchRepository matchRepository;
    private final TournamentTeamRepository tournamentTeamRepository;
    private final ApplicationEventPublisher eventPublisher;

    public MatchResultService(
            CricketMatchRepository matchRepository,
            TournamentTeamRepository tournamentTeamRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.matchRepository =
                matchRepository;
        this.tournamentTeamRepository =
                tournamentTeamRepository;
        this.eventPublisher =
                eventPublisher;
    }

    public void markNoResult(
            Long matchId,
            NoResultRequest request
    ) {

        CricketMatch match =
                matchRepository
                        .findDetailedById(matchId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Match not found"
                                )
                        );

        try {

            match.markNoResult(
                    request.reason().trim()
            );

        } catch (IllegalStateException ex) {

            throw new ConflictException(
                    ex.getMessage()
            );
        }
    }

    public void resolveKnockout(
            Long matchId,
            ResolveKnockoutMatchRequest request
    ) {

        CricketMatch match =
                matchRepository
                        .findDetailedById(matchId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Match not found"
                                )
                        );

        if (match.getStage()
                == MatchStage.LEAGUE) {

            throw new ConflictException(
                    "This endpoint is only for knockout matches"
            );
        }

        TournamentTeam winner =
                tournamentTeamRepository
                        .findDetailedById(
                                request.winnerTournamentTeamId()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Tournament team not found"
                                )
                        );

        MatchResultType resultType =
                switch (request.resolutionType()) {

                    case TIEBREAKER ->
                            MatchResultType.TIEBREAKER;

                    case FORFEIT ->
                            MatchResultType.FORFEIT;
                };

        try {

            match.resolveKnockoutWinner(
                    winner,
                    resultType,
                    request.reason().trim()
            );

        } catch (
                IllegalStateException
                | IllegalArgumentException ex
        ) {

            throw new ConflictException(
                    ex.getMessage()
            );
        }

        eventPublisher.publishEvent(
                new MatchCompletedEvent(
                        match.getId(),
                        match.getTournamentEdition()
                                .getId(),
                        match.getStage()
                )
        );
    }
}
