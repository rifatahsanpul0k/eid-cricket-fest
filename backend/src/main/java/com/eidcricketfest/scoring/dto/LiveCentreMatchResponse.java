package com.eidcricketfest.scoring.dto;

import com.eidcricketfest.match.entity.*;

import java.math.BigDecimal;
import java.time.Instant;

public record LiveCentreMatchResponse(

        Long matchId,
        MatchType matchType,
        MatchStatus status,
        Integer matchNumber,
        MatchStage stage,
        Instant scheduledAt,
        Integer oversPerInnings,
        MatchResultStatus resultStatus,
        Long rematchOfMatchId,
        Long supersededByMatchId,
        SideInfo teamA,
        SideInfo teamB,
        VenueInfo venue,
        TossInfo toss,
        InningsSummary innings,
        String resultText,
        SideInfo winner
) {

    public record SideInfo(
            Long matchSideId,
            Long tournamentTeamId,
            String name
    ) {}

    public record VenueInfo(
            Long id,
            String name
    ) {}

    public record TossInfo(
            Long winnerMatchSideId,
            String winnerName,
            TossDecision decision
    ) {}

    public record InningsSummary(
            Long inningsId,
            short inningsNumber,
            String battingTeam,
            String bowlingTeam,
            int runs,
            int wickets,
            String overs,
            Integer target,
            Integer runsRequired,
            Integer ballsRemaining,
            BigDecimal currentRunRate,
            BigDecimal requiredRunRate,
            PlayerInfo striker,
            PlayerInfo nonStriker,
            PlayerInfo bowler
    ) {}

    public record PlayerInfo(
            Long playerId,
            String name
    ) {}
}
