package com.eidcricketfest.player.dto;

import java.math.BigDecimal;

public record MyEditionStatisticsResponse(
        Long editionId,
        Long playerId,
        String playerName,
        Integer matchesPlayed,
        Batting batting,
        Bowling bowling,
        Fielding fielding
) {

    public record Batting(
            Integer innings,
            Integer runs,
            Integer balls,
            Integer highestScore,
            Integer fours,
            Integer sixes,
            Integer dismissals,
            BigDecimal average,
            BigDecimal strikeRate
    ) {}

    public record Bowling(
            String overs,
            Integer legalBalls,
            Integer runsConceded,
            Integer wickets,
            String bestBowling,
            BigDecimal average,
            BigDecimal economy
    ) {}

    public record Fielding(
            Integer catches,
            Integer stumpings,
            Integer runOuts
    ) {}
}
