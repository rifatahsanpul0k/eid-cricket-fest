package com.eidcricketfest.award.dto;

import com.eidcricketfest.award.entity.AwardType;

public record PlayerAwardResponse(

        Long id,

        AwardType awardType,
        String title,

        Long registrationId,
        Long playerId,
        String playerName,

        String notes

) {}
