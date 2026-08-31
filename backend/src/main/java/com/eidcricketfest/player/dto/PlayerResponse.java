package com.eidcricketfest.player.dto;

import java.time.Instant;
import java.time.LocalDate;

public record PlayerResponse(

        Long id,

        Long userId,

        String fullName,

        String photoUrl,

        LocalDate dateOfBirth,

        CategoryInfo primaryCategory,

        String battingStyle,

        String bowlingStyle,

        Instant createdAt
) {

    public record CategoryInfo(
            Short id,
            String code,
            String name
    ) {
    }
}