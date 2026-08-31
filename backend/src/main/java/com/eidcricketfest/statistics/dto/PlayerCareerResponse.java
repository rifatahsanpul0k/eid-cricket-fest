package com.eidcricketfest.statistics.dto;

import java.math.BigDecimal;

public record PlayerCareerResponse(

        Long playerId,
        String playerName,

        int editionsPlayed,
        int matchesPlayed,

        BattingCareer batting,
        BowlingCareer bowling

) {

    public record BattingCareer(

            int innings,
            int runs,
            int balls,

            int highestScore,

            int fours,
            int sixes,

            int dismissals,

            BigDecimal average,
            BigDecimal strikeRate
    ) {}

    public record BowlingCareer(

            String overs,

            int legalBalls,
            int runsConceded,
            int wickets,

            String bestBowling,

            BigDecimal average,
            BigDecimal economy
    ) {}
}
