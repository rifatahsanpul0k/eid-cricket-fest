package com.eidcricketfest.registration.dto;

import com.eidcricketfest.registration.entity.*;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(

        Long id,
        Long registrationId,
        BigDecimal amount,
        PaymentMethod paymentMethod,
        String transactionReference,
        PaymentStatus status,
        Instant paidAt,
        Instant verifiedAt,
        String rejectionReason,
        Instant createdAt
) {
}