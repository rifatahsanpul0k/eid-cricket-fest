package com.eidcricketfest.team.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTeamRequest(

        @NotBlank
        @Size(max = 120)
        String name,

        @Size(max = 20)
        String shortName,

        @Size(max = 2000)
        String logoUrl
) {}
