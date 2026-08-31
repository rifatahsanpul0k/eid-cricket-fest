package com.eidcricketfest.match.dto;

import jakarta.validation.constraints.NotNull;

public record AssignScorerRequest(

        @NotNull
        Long scorerUserId,

        boolean primary
) {}
