package com.eidcricketfest.registration.controller;

import com.eidcricketfest.common.dto.PageResponse;
import com.eidcricketfest.registration.dto.*;
import com.eidcricketfest.registration.entity.RegistrationStatus;
import com.eidcricketfest.registration.service.RegistrationService;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Player Registration")
@RestController
@RequestMapping("/api/v1/tournament-editions")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(
            RegistrationService registrationService
    ) {
        this.registrationService = registrationService;
    }

    @Operation(
            summary = "Register myself for a tournament edition"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{editionId}/registrations/me")
    @ResponseStatus(HttpStatus.CREATED)
    public RegistrationResponse registerMyself(
            @PathVariable Long editionId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateRegistrationRequest request
    ) {

        return registrationService.registerMyself(
                Long.valueOf(jwt.getSubject()),
                editionId,
                request
        );
    }

    @Operation(summary = "Search player registrations")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{editionId}/registrations")
    public PageResponse<RegistrationResponse> getRegistrations(
            @PathVariable Long editionId,
            @RequestParam(required = false)
            RegistrationStatus status,
            @RequestParam(required = false)
            String category,
            @RequestParam(required = false)
            String q,
            @RequestParam(defaultValue = "0")
            Integer page,
            @RequestParam(defaultValue = "20")
            Integer size,
            @RequestParam(defaultValue = "registeredAt")
            String sortBy,
            @RequestParam(defaultValue = "desc")
            String direction
    ) {
        return registrationService.searchRegistrations(
                editionId,
                status,
                category,
                q,
                page,
                size,
                sortBy,
                direction
        );
    }

    @Operation(summary = "Approve player registration")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/registrations/{registrationId}/approve")
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
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/registrations/{registrationId}/reject")
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
