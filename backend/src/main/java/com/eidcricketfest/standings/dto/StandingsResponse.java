package com.eidcricketfest.standings.dto;

import java.math.BigDecimal;
import java.util.List;

public record StandingsResponse(

        Long tournamentEditionId,

        List<Row> standings

) {

    public record Row(

            int rank,

            Long tournamentTeamId,
            Long teamId,

            String teamName,
            String shortName,

            int played,
            int won,
            int lost,
            int tied,
            int noResult,

            BigDecimal points,

            BigDecimal netRunRate,

            BigDecimal runRateFor,
            BigDecimal runRateAgainst
    ) {}
}
