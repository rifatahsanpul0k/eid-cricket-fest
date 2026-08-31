package com.eidcricketfest.registration.repository;

import com.eidcricketfest.registration.entity.PlayerRegistration;
import com.eidcricketfest.registration.entity.RegistrationStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

public final class PlayerRegistrationSpecifications {

    private PlayerRegistrationSpecifications() {
    }

    public static Specification<PlayerRegistration> edition(
            Long editionId
    ) {

        return (root, query, cb) ->
                cb.equal(
                        root.get("tournamentEdition")
                                .get("id"),
                        editionId
                );
    }

    public static Specification<PlayerRegistration> status(
            RegistrationStatus status
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

    public static Specification<PlayerRegistration> categoryCode(
            String category
    ) {

        if (category == null
                || category.isBlank()) {

            return Specification.unrestricted();
        }

        String code =
                category
                        .trim()
                        .toUpperCase(Locale.ROOT);

        return (root, query, cb) ->
                cb.equal(
                        root.get("category")
                                .get("code"),
                        code
                );
    }

    public static Specification<PlayerRegistration> playerNameContains(
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
                                root.get("player")
                                        .get("fullName")
                        ),
                        value
                );
    }
}
