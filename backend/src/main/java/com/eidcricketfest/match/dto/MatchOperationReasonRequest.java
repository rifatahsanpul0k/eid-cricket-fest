package com.eidcricketfest.match.dto;

import jakarta.validation.constraints.NotBlank;

public record MatchOperationReasonRequest(
        @NotBlank
        String reason
) {}
