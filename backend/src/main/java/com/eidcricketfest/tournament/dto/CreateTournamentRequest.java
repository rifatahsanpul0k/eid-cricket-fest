package com.eidcricketfest.tournament.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTournamentRequest(

        @NotBlank
        @Size(max = 150)
        String name,

        @Size(max = 5000)
        String description,

        @Size(max = 2000)
        String logoUrl
) {
}