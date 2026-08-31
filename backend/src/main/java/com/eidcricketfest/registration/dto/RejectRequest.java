package com.eidcricketfest.registration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectRequest(

        @NotBlank
        @Size(max = 1000)
        String reason
) {
}