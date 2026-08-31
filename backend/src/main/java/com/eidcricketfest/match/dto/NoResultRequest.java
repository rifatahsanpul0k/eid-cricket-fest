package com.eidcricketfest.match.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NoResultRequest(

        @NotBlank
        @Size(max = 1000)
        String reason

) {}
