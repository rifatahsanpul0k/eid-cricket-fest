package com.eidcricketfest.match.dto;

import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.List;

public record CreateFriendlyMatchRequest(

        @NotBlank
        @Size(max = 150)
        String teamAName,

        @NotBlank
        @Size(max = 150)
        String teamBName,

        @NotEmpty
        List<Long> teamAPlayerIds,

        @NotEmpty
        List<Long> teamBPlayerIds,

        @NotNull
        @Positive
        Integer oversPerInnings,

        @NotNull
        Instant scheduledAt,

        @NotNull
        Long venueId
) {}
