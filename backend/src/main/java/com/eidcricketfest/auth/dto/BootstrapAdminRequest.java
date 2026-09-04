package com.eidcricketfest.auth.dto;

import jakarta.validation.constraints.*;

public record BootstrapAdminRequest(

        @NotBlank
        String bootstrapToken,

        @NotBlank
        @Size(max = 120)
        String displayName,

        @Email
        @NotBlank
        @Size(max = 255)
        String email,

        @NotBlank
        @Size(min = 8, max = 72)
        String password

) {}
