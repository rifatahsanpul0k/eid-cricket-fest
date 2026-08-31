package com.eidcricketfest.tournament.dto;

import java.time.Instant;

public record TournamentResponse(
        Long id,
        String name,
        String description,
        String logoUrl,
        Instant createdAt,
        Instant updatedAt
) {
}