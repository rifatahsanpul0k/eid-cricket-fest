package com.eidcricketfest.scoring.dto;

import com.eidcricketfest.scoring.entity.InningsStatus;

public record InningsResponse(

        Long id,
        short inningsNumber,

        String battingTeam,
        String bowlingTeam,

        int runs,
        int wickets,

        int legalBalls,

        String overs,

        Integer target,

        int wides,
        int noBalls,
        int byes,
        int legByes,
        int penaltyRuns,

        InningsStatus status
) {}
