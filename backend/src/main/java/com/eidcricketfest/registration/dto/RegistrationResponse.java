package com.eidcricketfest.registration.dto;

import com.eidcricketfest.registration.entity.RegistrationStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record RegistrationResponse(

        Long id,
        Long tournamentEditionId,
        Long playerId,

        Short categoryId,
        String category,

        BigDecimal feeAmount,
        String currency,

        RegistrationStatus status,

        Instant registeredAt
) {
}