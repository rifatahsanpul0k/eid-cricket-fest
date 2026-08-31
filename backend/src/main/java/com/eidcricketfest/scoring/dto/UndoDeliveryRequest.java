package com.eidcricketfest.scoring.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UndoDeliveryRequest(

        @NotNull
        UUID clientEventId,

        @NotBlank
        @Size(max = 500)
        String reason
) {}
