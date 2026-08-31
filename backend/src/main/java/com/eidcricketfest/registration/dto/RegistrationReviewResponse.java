package com.eidcricketfest.registration.dto;

import com.eidcricketfest.registration.entity.RegistrationStatus;

import java.math.BigDecimal;

public record RegistrationReviewResponse(

        Long registrationId,
        RegistrationStatus status,

        BigDecimal requiredFee,
        BigDecimal verifiedAmount,
        BigDecimal remainingAmount,

        String rejectionReason
) {
}