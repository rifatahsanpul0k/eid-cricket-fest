package com.eidcricketfest.scoring.dto;

import com.eidcricketfest.match.entity.*;

import java.math.BigDecimal;
import java.util.List;

public record ScorecardResponse(

        Long matchId,
        MatchType matchType,
        Integer matchNumber,
        MatchStage stage,
        MatchStatus status,
        MatchResultStatus resultStatus,
        String resultText,
        Long rematchOfMatchId,
        Long supersededByMatchId,
        List<InningsScorecard> innings

) {

    public record InningsScorecard(

            short inningsNumber,

            String battingTeam,

            int runs,
            int wickets,
            String overs,

            List<BattingRow> batting,

            List<BowlingRow> bowling
    ) {}

    public record BattingRow(

            Long playerId,
            String playerName,

            int runs,
            int balls,

            int fours,
            int sixes,

            BigDecimal strikeRate,

            String dismissal
    ) {}

    public record BowlingRow(

            Long playerId,
            String playerName,

            String overs,

            int runs,
            int wickets,

            BigDecimal economy
    ) {}
}
