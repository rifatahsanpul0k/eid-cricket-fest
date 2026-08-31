package com.eidcricketfest.draft.dto;

public record DraftPoolPlayerResponse(

        Long registrationId,
        Long playerId,
        String playerName,

        Short categoryId,
        String categoryCode,
        String categoryName
) {}
