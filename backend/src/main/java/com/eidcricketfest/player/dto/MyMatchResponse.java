package com.eidcricketfest.player.dto;

import com.eidcricketfest.match.entity.MatchResultType;
import com.eidcricketfest.match.entity.MatchStage;
import com.eidcricketfest.match.entity.MatchStatus;

import java.time.Instant;

public record MyMatchResponse(
        Long matchId,
        Integer matchNumber,
        Integer roundNumber,
        MatchStage stage,
        MatchStatus status,
        Instant scheduledAt,
        Integer oversPerInnings,
        Venue venue,
        Team teamA,
        Team teamB,
        Long myTournamentTeamId,
        Team opponent,
        boolean inPlayingXi,
        boolean myTeamPlayingXiSubmitted,
        Long winnerTeamId,
        MatchResultType resultType,
        String resultSummary
) {

    public record Venue(
            Long id,
            String name
    ) {}

    public record Team(
            Long tournamentTeamId,
            String name
    ) {}
}
