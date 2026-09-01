package com.eidcricketfest.tournament.dto;

import com.eidcricketfest.tournament.entity.TournamentEditionStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTournamentEditionStatusRequest(

        @NotNull
        TournamentEditionStatus status
) {
}
