package com.eidcricketfest.draft.dto;

import java.time.Instant;

public record DraftPickResponse(

        Long id,
        Integer pickNumber,
        Integer roundNumber,

        Long tournamentTeamId,
        String teamName,

        Long registrationId,
        Long playerId,
        String playerName,

        Instant selectedAt
) {}
