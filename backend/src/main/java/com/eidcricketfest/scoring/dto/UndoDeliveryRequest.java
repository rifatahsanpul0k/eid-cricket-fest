package com.eidcricketfest.scoring.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UndoDeliveryRequest(

        @NotBlank
        @Size(max = 500)
        String reason
) {}
