package com.eidcricketfest.scoring.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RecordDeliveryRequest(

        @NotNull
        UUID clientEventId,

        @Min(0) int runsOffBat,

        @Min(0) int wideRuns,
        @Min(0) int noBallRuns,
        @Min(0) int byeRuns,
        @Min(0) int legByeRuns,
        @Min(0) int penaltyRuns,

        /*
         * null = calculate automatically
         * true/false = scorer override
         */
        Boolean swapEnds,

        WicketRequest wicket
) {}
