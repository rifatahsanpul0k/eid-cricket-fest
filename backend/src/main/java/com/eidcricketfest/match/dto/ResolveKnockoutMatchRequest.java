package com.eidcricketfest.match.dto;

import jakarta.validation.constraints.*;

public record ResolveKnockoutMatchRequest(

        @NotNull
        Long winnerTournamentTeamId,

        @NotNull
        ResolutionType resolutionType,

        @NotBlank
        @Size(max = 1000)
        String reason

) {

    public enum ResolutionType {
        TIEBREAKER,
        FORFEIT
    }
}
