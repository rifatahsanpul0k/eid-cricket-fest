package com.eidcricketfest.team.dto;

public record TeamResponse(
        Long id,
        String name,
        String shortName,
        String logoUrl
) {}
