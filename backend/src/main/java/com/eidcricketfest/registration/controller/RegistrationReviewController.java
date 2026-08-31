package com.eidcricketfest.registration.controller;

import com.eidcricketfest.registration.dto.RegistrationReviewResponse;
import com.eidcricketfest.registration.dto.RejectRequest;
import com.eidcricketfest.registration.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Registration Review")
@RestController
@RequestMapping("/api/v1/registrations")
@SecurityRequirement(name = "bearerAuth")
public class RegistrationReviewController {

    private final RegistrationService registrationService;

    public RegistrationReviewController(
            RegistrationService registrationService
    ) {
        this.registrationService = registrationService;
    }

    @Operation(summary = "Approve player registration")
    @PatchMapping("/{registrationId}/approve")
    public RegistrationReviewResponse approve(
            @PathVariable Long registrationId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return registrationService.approve(
                Long.valueOf(jwt.getSubject()),
                registrationId
        );
    }

    @Operation(summary = "Reject player registration")
    @PatchMapping("/{registrationId}/reject")
    public RegistrationReviewResponse reject(
            @PathVariable Long registrationId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody RejectRequest request
    ) {
        return registrationService.reject(
                Long.valueOf(jwt.getSubject()),
                registrationId,
                request
        );
    }
}