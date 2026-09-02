package com.eidcricketfest.match.dto;

import com.eidcricketfest.match.entity.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

public record MatchResponse(

        Long id,

        MatchType matchType,

        Integer matchNumber,
        Integer roundNumber,

        MatchStage stage,
        MatchStatus status,
        MatchResultStatus resultStatus,

        Long rematchOfMatchId,
        Long supersededByMatchId,

        TeamInfo teamA,
        TeamInfo teamB,

        Integer oversPerInnings,

        VenueInfo venue,

        Instant scheduledAt,

        boolean scorerAssigned,
        boolean teamAPlayingXiSubmitted,
        boolean teamBPlayingXiSubmitted,
        boolean tossCompleted,

        List<MatchOperationType> availableOperations,
        List<MatchOperationHistoryResponse> operationHistory
) {

    @Schema(name = "MatchTeamInfo")
    public record TeamInfo(
            Long matchSideId,
            Long tournamentTeamId,
            String name
    ) {}

    public record VenueInfo(
            Long id,
            String name
    ) {}
}
