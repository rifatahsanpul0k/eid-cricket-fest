package com.eidcricketfest.match.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateVenueRequest(

        @NotBlank
        @Size(max = 150)
        String name,

        @Size(max = 1000)
        String address
) {}
