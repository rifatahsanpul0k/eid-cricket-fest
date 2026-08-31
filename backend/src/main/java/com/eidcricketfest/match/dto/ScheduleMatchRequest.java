package com.eidcricketfest.match.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record ScheduleMatchRequest(

        @NotNull
        Instant scheduledAt,

        @NotNull
        Long venueId
) {}
