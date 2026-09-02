package com.eidcricketfest.match.dto;

public record FriendlyPlayerOptionResponse(
        Long playerId,
        String fullName,
        String photoUrl,
        String primaryCategory,
        String battingStyle,
        String bowlingStyle
) {}
