package com.eidcricketfest.auth.dto;

import jakarta.validation.constraints.*;

public record RegisterRequest(

        @NotBlank
        @Size(max = 120)
        String displayName,

        @Email
        @Size(max = 255)
        String email,

        @Size(max = 30)
        String phone,

        @NotBlank
        @Size(min = 8, max = 72)
        String password
) {

    @AssertTrue(
            message = "Either email or phone must be provided"
    )
    public boolean isContactProvided() {

        return (email != null && !email.isBlank())
                ||
                (phone != null && !phone.isBlank());
    }
}