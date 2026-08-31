package com.eidcricketfest.player.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record CreatePlayerRequest(

        @NotBlank
        @Size(max = 150)
        String fullName,

        @Size(max = 2000)
        String photoUrl,

        @Past
        LocalDate dateOfBirth,

        Short primaryCategoryId,

        @Size(max = 50)
        String battingStyle,

        @Size(max = 80)
        String bowlingStyle
) {
}