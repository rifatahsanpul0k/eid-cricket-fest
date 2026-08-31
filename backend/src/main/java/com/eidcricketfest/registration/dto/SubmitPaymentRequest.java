package com.eidcricketfest.registration.dto;

import com.eidcricketfest.registration.entity.PaymentMethod;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;

public record SubmitPaymentRequest(

        @NotNull
        @DecimalMin(value = "0.01")
        BigDecimal amount,

        @NotNull
        PaymentMethod paymentMethod,

        @Size(max = 150)
        String transactionReference,

        Instant paidAt
) {

    @AssertTrue(
            message = "Transaction reference is required for bKash, Nagad and bank payments"
    )
    public boolean isTransactionReferenceValid() {

        if (paymentMethod == null) {
            return true;
        }

        boolean digital =
                paymentMethod == PaymentMethod.BKASH
                        || paymentMethod == PaymentMethod.NAGAD
                        || paymentMethod == PaymentMethod.BANK;

        if (!digital) {
            return true;
        }

        return transactionReference != null
                && !transactionReference.isBlank();
    }
}