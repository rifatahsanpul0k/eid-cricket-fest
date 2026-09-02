package com.eidcricketfest.match.dto;

import jakarta.validation.constraints.*;

import java.time.Instant;

public record OrderRematchRequest(
        @NotBlank
        String reason,

        Instant scheduledAt,

        Long venueId,

        @Min(1)
        Integer oversPerInnings
) {}
