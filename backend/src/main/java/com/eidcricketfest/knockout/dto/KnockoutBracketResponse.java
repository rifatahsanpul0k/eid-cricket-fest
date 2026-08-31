package com.eidcricketfest.knockout.dto;

import com.eidcricketfest.match.entity.*;

import java.util.List;

public record KnockoutBracketResponse(

        Long tournamentEditionId,

        List<MatchInfo> semiFinals,

        MatchInfo finalMatch

) {

    public record MatchInfo(

            Long matchId,
            Integer matchNumber,

            MatchStage stage,
            MatchStatus status,

            TeamInfo teamA,
            TeamInfo teamB,

            TeamInfo winner,

            Long sourceMatchAId,
            Long sourceMatchBId
    ) {}

    public record TeamInfo(

            Long tournamentTeamId,
            String teamName,
            Integer seed

    ) {}
}
