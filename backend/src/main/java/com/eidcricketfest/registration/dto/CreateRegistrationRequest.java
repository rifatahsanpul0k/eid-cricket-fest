package com.eidcricketfest.registration.dto;

import jakarta.validation.constraints.NotNull;

public record CreateRegistrationRequest(

        @NotNull
        Short categoryId
) {
}