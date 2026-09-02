package com.eidcricketfest.match.dto;

import com.eidcricketfest.match.entity.TossDecision;
import jakarta.validation.constraints.NotNull;

public record RecordTossRequest(

        Long winnerTournamentTeamId,

        Long winnerMatchSideId,

        @NotNull
        TossDecision decision
) {}
