package com.eidcricketfest.match.controller;

import com.eidcricketfest.match.dto.*;
import com.eidcricketfest.match.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Match Operations")
@RestController
@RequestMapping("/api/v1/matches/{matchId}/operations")
@SecurityRequirement(name = "bearerAuth")
public class MatchOperationsController {

    private final MatchOperationsService operationsService;
    private final FixtureService fixtureService;

    public MatchOperationsController(
            MatchOperationsService operationsService,
            FixtureService fixtureService
    ) {
        this.operationsService = operationsService;
        this.fixtureService = fixtureService;
    }

    @Operation(summary = "Reschedule match")
    @PatchMapping("/reschedule")
    public MatchResponse reschedule(
            @PathVariable Long matchId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody RescheduleMatchOperationRequest request
    ) {
        Long id = operationsService.reschedule(
                matchId,
                actorUserId(jwt),
                request
        );
        return fixtureService.getMatch(id);
    }

    @Operation(summary = "Postpone match")
    @PostMapping("/postpone")
    public MatchResponse postpone(
            @PathVariable Long matchId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody MatchOperationReasonRequest request
    ) {
        return operationResponse(
                operationsService.postpone(
                        matchId,
                        actorUserId(jwt),
                        request
                )
        );
    }

    @Operation(summary = "Suspend match")
    @PostMapping("/suspend")
    public MatchResponse suspend(
            @PathVariable Long matchId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody MatchOperationReasonRequest request
    ) {
        return operationResponse(
                operationsService.suspend(
                        matchId,
                        actorUserId(jwt),
                        request
                )
        );
    }

    @Operation(summary = "Resume match")
    @PostMapping("/resume")
    public MatchResponse resume(
            @PathVariable Long matchId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody MatchOperationReasonRequest request
    ) {
        return operationResponse(
                operationsService.resume(
                        matchId,
                        actorUserId(jwt),
                        request
                )
        );
    }

    @Operation(summary = "Abandon match")
    @PostMapping("/abandon")
    public MatchResponse abandon(
            @PathVariable Long matchId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody MatchOperationReasonRequest request
    ) {
        return operationResponse(
                operationsService.abandon(
                        matchId,
                        actorUserId(jwt),
                        request
                )
        );
    }

    @Operation(summary = "Cancel match")
    @PostMapping("/cancel")
    public MatchResponse cancel(
            @PathVariable Long matchId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody MatchOperationReasonRequest request
    ) {
        return operationResponse(
                operationsService.cancel(
                        matchId,
                        actorUserId(jwt),
                        request
                )
        );
    }

    @Operation(summary = "Reset match toss")
    @PostMapping("/reset-toss")
    public MatchResponse resetToss(
            @PathVariable Long matchId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody MatchOperationReasonRequest request
    ) {
        return operationResponse(
                operationsService.resetToss(
                        matchId,
                        actorUserId(jwt),
                        request
                )
        );
    }

    @Operation(summary = "Mark result under review")
    @PostMapping("/review")
    public MatchResponse review(
            @PathVariable Long matchId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody MatchOperationReasonRequest request
    ) {
        return operationResponse(
                operationsService.markUnderReview(
                        matchId,
                        actorUserId(jwt),
                        request
                )
        );
    }

    @Operation(summary = "Restore official result")
    @PostMapping("/restore-result")
    public MatchResponse restoreResult(
            @PathVariable Long matchId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody MatchOperationReasonRequest request
    ) {
        return operationResponse(
                operationsService.restoreOfficial(
                        matchId,
                        actorUserId(jwt),
                        request
                )
        );
    }

    @Operation(summary = "Void result")
    @PostMapping("/void-result")
    public MatchResponse voidResult(
            @PathVariable Long matchId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody MatchOperationReasonRequest request
    ) {
        return operationResponse(
                operationsService.voidResult(
                        matchId,
                        actorUserId(jwt),
                        request
                )
        );
    }

    @Operation(summary = "Order rematch")
    @PostMapping("/rematch")
    public MatchResponse rematch(
            @PathVariable Long matchId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody OrderRematchRequest request
    ) {
        Long rematchId =
                operationsService.orderRematch(
                        matchId,
                        actorUserId(jwt),
                        request
                );

        return fixtureService.getMatch(rematchId);
    }

    private MatchResponse operationResponse(Long matchId) {
        return fixtureService.getMatch(matchId);
    }

    private Long actorUserId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
