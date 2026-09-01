package com.eidcricketfest.match.controller;

import com.eidcricketfest.match.dto.*;
import com.eidcricketfest.match.service.MatchSetupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Match Setup")
@RestController
@RequestMapping("/api/v1/matches")
@SecurityRequirement(name = "bearerAuth")
public class MatchSetupController {

    private final MatchSetupService matchSetupService;

    public MatchSetupController(
            MatchSetupService matchSetupService
    ) {
        this.matchSetupService = matchSetupService;
    }

    @Operation(summary = "Get match setup details")
    @GetMapping("/{matchId}/setup")
    public MatchSetupDetailsResponse setupDetails(
            @PathVariable Long matchId
    ) {
        return matchSetupService.setupDetails(matchId);
    }

    @Operation(summary = "Assign match scorer")
    @PostMapping("/{matchId}/scorers")
    public void assignScorer(
            @PathVariable Long matchId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AssignScorerRequest request
    ) {
        matchSetupService.assignScorer(
                matchId,
                Long.valueOf(jwt.getSubject()),
                request
        );
    }

    @Operation(summary = "Submit playing XI")
    @PutMapping(
            "/{matchId}/teams/{tournamentTeamId}/playing-xi"
    )
    public void submitPlayingXi(
            @PathVariable Long matchId,
            @PathVariable Long tournamentTeamId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SubmitPlayingXiRequest request
    ) {

        List<String> roles =
                jwt.getClaimAsStringList("roles");

        boolean privileged =
                roles != null
                && (
                    roles.contains("ORGANIZER")
                    || roles.contains("ADMIN")
                );

        matchSetupService.submitPlayingXi(
                matchId,
                tournamentTeamId,
                Long.valueOf(jwt.getSubject()),
                privileged,
                request
        );
    }

    @Operation(summary = "Record toss")
    @PostMapping("/{matchId}/toss")
    public void recordToss(
            @PathVariable Long matchId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody RecordTossRequest request
    ) {

        matchSetupService.recordToss(
                matchId,
                Long.valueOf(jwt.getSubject()),
                request
        );
    }
}
