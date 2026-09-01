package com.eidcricketfest.tournament.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record UpdateTournamentEditionRequest(

        @NotBlank
        @Size(max = 150)
        String name,

        LocalDate startDate,

        LocalDate endDate,

        Instant registrationStartAt,

        Instant registrationEndAt,

        @NotNull
        @Positive
        Integer oversPerInnings,

        @NotNull
        @Min(2)
        Integer squadSize,

        @NotNull
        @Min(2)
        Integer playingXiSize,

        @DecimalMin(value = "0.00")
        BigDecimal registrationFee,

        @Pattern(regexp = "^[A-Z]{3}$")
        String registrationCurrency,

        @DecimalMin("0.00")
        BigDecimal winPoints,

        @DecimalMin("0.00")
        BigDecimal tiePoints,

        @DecimalMin("0.00")
        BigDecimal noResultPoints,

        @DecimalMin("0.00")
        BigDecimal lossPoints
) {

    @AssertTrue(message = "End date must be on or after start date")
    public boolean isTournamentDateRangeValid() {
        return startDate == null
                || endDate == null
                || !endDate.isBefore(startDate);
    }

    @AssertTrue(message = "Registration end time must be after registration start time")
    public boolean isRegistrationRangeValid() {
        return registrationStartAt == null
                || registrationEndAt == null
                || registrationEndAt.isAfter(registrationStartAt);
    }

    @AssertTrue(message = "Playing XI size must be less than or equal to squad size")
    public boolean isPlayingXiSizeValid() {
        return squadSize == null
                || playingXiSize == null
                || playingXiSize <= squadSize;
    }
}
