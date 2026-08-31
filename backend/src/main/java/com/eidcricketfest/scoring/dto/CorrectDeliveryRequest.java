package com.eidcricketfest.scoring.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CorrectDeliveryRequest(

        @NotNull
        UUID clientEventId,

        @Min(0) int runsOffBat,

        @Min(0) int wideRuns,
        @Min(0) int noBallRuns,
        @Min(0) int byeRuns,
        @Min(0) int legByeRuns,
        @Min(0) int penaltyRuns,

        Boolean swapEnds,

        WicketRequest wicket,

        @Size(max = 1000)
        String commentary,

        @NotBlank
        @Size(max = 500)
        String reason

) {

    public RecordDeliveryRequest asDeliveryRequest() {

        return new RecordDeliveryRequest(
                clientEventId,
                runsOffBat,
                wideRuns,
                noBallRuns,
                byeRuns,
                legByeRuns,
                penaltyRuns,
                swapEnds,
                wicket
        );
    }
}
