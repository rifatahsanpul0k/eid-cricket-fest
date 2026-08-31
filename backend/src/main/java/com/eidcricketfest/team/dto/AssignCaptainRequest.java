package com.eidcricketfest.team.dto;

import jakarta.validation.constraints.NotNull;

public record AssignCaptainRequest(

        @NotNull
        Long registrationId
) {}
