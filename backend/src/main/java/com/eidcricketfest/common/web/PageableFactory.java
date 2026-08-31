package com.eidcricketfest.common.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

@Component
public class PageableFactory {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public Pageable create(
            Integer page,
            Integer size,
            String sortBy,
            String direction,
            Map<String, String> allowedSorts,
            String defaultSort
    ) {

        int resolvedPage =
                page == null
                        ? DEFAULT_PAGE
                        : page;

        int resolvedSize =
                size == null
                        ? DEFAULT_SIZE
                        : size;

        if (resolvedPage < 0) {
            throw new IllegalArgumentException(
                    "Page must be zero or greater"
            );
        }

        if (resolvedSize < 1
                || resolvedSize > MAX_SIZE) {

            throw new IllegalArgumentException(
                    "Page size must be between 1 and "
                            + MAX_SIZE
            );
        }

        String requestedSort =
                sortBy == null
                        || sortBy.isBlank()
                        ? defaultSort
                        : sortBy;

        String entitySort =
                allowedSorts.get(requestedSort);

        if (entitySort == null) {
            throw new IllegalArgumentException(
                    "Unsupported sort field: "
                            + requestedSort
            );
        }

        Sort.Direction resolvedDirection;

        try {

            resolvedDirection =
                    direction == null
                            || direction.isBlank()
                            ? Sort.Direction.ASC
                            : Sort.Direction.valueOf(
                                    direction
                                            .trim()
                                            .toUpperCase(
                                                    Locale.ROOT
                                            )
                            );

        } catch (IllegalArgumentException ex) {

            throw new IllegalArgumentException(
                    "Sort direction must be asc or desc"
            );
        }

        Sort sort =
                Sort.by(
                        resolvedDirection,
                        entitySort
                );

        if (!"id".equals(entitySort)) {

            sort = sort.and(
                    Sort.by(
                            Sort.Direction.ASC,
                            "id"
                    )
            );
        }

        return PageRequest.of(
                resolvedPage,
                resolvedSize,
                sort
        );
    }
}
