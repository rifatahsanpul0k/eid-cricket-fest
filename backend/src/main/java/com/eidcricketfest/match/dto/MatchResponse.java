package com.eidcricketfest.match.dto;

import com.eidcricketfest.match.entity.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record MatchResponse(

        Long id,

        Integer matchNumber,
        Integer roundNumber,

        MatchStage stage,
        MatchStatus status,

        TeamInfo teamA,
        TeamInfo teamB,

        Integer oversPerInnings,

        VenueInfo venue,

        Instant scheduledAt
) {

    @Schema(name = "MatchTeamInfo")
    public record TeamInfo(
            Long tournamentTeamId,
            String name
    ) {}

    public record VenueInfo(
            Long id,
            String name
    ) {}
}
