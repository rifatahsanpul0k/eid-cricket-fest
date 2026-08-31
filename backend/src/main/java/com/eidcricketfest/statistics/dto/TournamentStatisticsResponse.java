package com.eidcricketfest.statistics.dto;

import java.math.BigDecimal;
import java.util.List;

public record TournamentStatisticsResponse(

        Long tournamentEditionId,

        List<BattingLeader> batting,

        List<BowlingLeader> bowling

) {

    public record BattingLeader(

            int rank,

            Long playerId,
            String playerName,

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

    public record BowlingLeader(

            int rank,

            Long playerId,
            String playerName,

            int wickets,

            String overs,

            int runsConceded,

            String bestBowling,

            BigDecimal average,
            BigDecimal economy
    ) {}
}
