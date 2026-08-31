package com.eidcricketfest.player.repository;

import com.eidcricketfest.player.entity.Player;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

public final class PlayerSpecifications {

    private PlayerSpecifications() {
    }

    public static Specification<Player> nameContains(
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
                                root.get("fullName")
                        ),
                        value
                );
    }

    public static Specification<Player> categoryCode(
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
                        root.get("primaryCategory")
                                .get("code"),
                        code
                );
    }
}
