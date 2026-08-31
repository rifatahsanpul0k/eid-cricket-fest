package com.eidcricketfest.registration.repository;

import com.eidcricketfest.registration.entity.PaymentMethod;
import com.eidcricketfest.registration.entity.PaymentStatus;
import com.eidcricketfest.registration.entity.RegistrationPayment;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

public final class RegistrationPaymentSpecifications {

    private RegistrationPaymentSpecifications() {
    }

    public static Specification<RegistrationPayment> edition(
            Long editionId
    ) {

        return (root, query, cb) ->
                cb.equal(
                        root.get("registration")
                                .get("tournamentEdition")
                                .get("id"),
                        editionId
                );
    }

    public static Specification<RegistrationPayment> status(
            PaymentStatus status
    ) {

        if (status == null) {
            return Specification.unrestricted();
        }

        return (root, query, cb) ->
                cb.equal(
                        root.get("status"),
                        status
                );
    }

    public static Specification<RegistrationPayment> method(
            PaymentMethod method
    ) {

        if (method == null) {
            return Specification.unrestricted();
        }

        return (root, query, cb) ->
                cb.equal(
                        root.get("paymentMethod"),
                        method
                );
    }

    public static Specification<RegistrationPayment> playerNameContains(
            String search
    ) {

        if (search == null
                || search.isBlank()) {

            return Specification.unrestricted();
        }

        String value =
                "%"
                + search
                .trim()
                .toLowerCase(Locale.ROOT)
                + "%";

        return (root, query, cb) ->
                cb.like(
                        cb.lower(
                                root.get("registration")
                                        .get("player")
                                        .get("fullName")
                        ),
                        value
                );
    }
}
