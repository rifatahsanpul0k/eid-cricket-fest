package com.eidcricketfest.draft.dto;

import jakarta.validation.constraints.NotNull;

public record DraftPickRequest(

        @NotNull
        Long registrationId
) {}
