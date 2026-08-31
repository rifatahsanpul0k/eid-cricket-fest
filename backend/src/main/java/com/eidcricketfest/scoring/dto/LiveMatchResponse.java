package com.eidcricketfest.scoring.dto;

import com.eidcricketfest.match.entity.MatchStatus;

import java.math.BigDecimal;
import java.util.List;

public record LiveMatchResponse(

        Long matchId,
        Integer matchNumber,
        MatchStatus status,

        String teamA,
        String teamB,

        InningsInfo innings,

        List<BallInfo> recentBalls

) {

    public record InningsInfo(

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

    public record BallInfo(

            Long deliveryId,

            Integer sequence,

            int runs,

            boolean legal,

            String commentary
    ) {}
}
