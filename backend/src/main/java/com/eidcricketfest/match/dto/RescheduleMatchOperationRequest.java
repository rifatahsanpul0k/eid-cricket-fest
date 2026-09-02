package com.eidcricketfest.match.dto;

import jakarta.validation.constraints.*;

import java.time.Instant;

public record RescheduleMatchOperationRequest(
        @NotNull
        Instant scheduledAt,

        @NotNull
        Long venueId,

        @Min(1)
        Integer oversPerInnings,

        @NotBlank
        String reason
) {}
