package com.eidcricketfest.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(

        @NotBlank
        String refreshToken
) {
}