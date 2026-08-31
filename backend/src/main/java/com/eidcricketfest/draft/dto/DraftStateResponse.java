package com.eidcricketfest.draft.dto;

import com.eidcricketfest.draft.entity.*;

import java.util.List;

public record DraftStateResponse(

        Long id,
        Long tournamentEditionId,

        DraftStatus status,
        DraftPickMode pickMode,

        Integer squadSize,

        long completedPicks,
        long totalRequiredPicks,

        CurrentTurn currentTurn,

        List<OrderItem> order
) {

    public record CurrentTurn(
            Integer pickNumber,
            Integer roundNumber,
            Long tournamentTeamId,
            String teamName
    ) {}

    public record OrderItem(
            Integer position,
            Long tournamentTeamId,
            String teamName
    ) {}
}
