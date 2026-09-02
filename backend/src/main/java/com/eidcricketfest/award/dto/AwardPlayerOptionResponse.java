package com.eidcricketfest.award.dto;

public record AwardPlayerOptionResponse(

        Long registrationId,
        Long playerId,
        String playerName,
        Long tournamentTeamId,
        String teamName

) {}
