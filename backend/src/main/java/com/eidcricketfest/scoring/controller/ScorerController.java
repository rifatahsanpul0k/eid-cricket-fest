package com.eidcricketfest.scoring.controller;

import com.eidcricketfest.scoring.dto.ScorerMatchResponse;
import com.eidcricketfest.scoring.dto.ScorerMatchStateResponse;
import com.eidcricketfest.scoring.service.ScorerMatchQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Scorer Console")
@RestController
@RequestMapping("/api/v1/scorer")
@SecurityRequirement(name = "bearerAuth")
public class ScorerController {

    private final ScorerMatchQueryService queryService;

    public ScorerController(
            ScorerMatchQueryService queryService
    ) {
        this.queryService = queryService;
    }

    @Operation(summary = "List assigned scorer matches")
    @GetMapping("/matches")
    public List<ScorerMatchResponse> assignedMatches(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return queryService.assignedMatches(
                userId(jwt)
        );
    }

    @Operation(summary = "Get scorer match state")
    @GetMapping("/matches/{matchId}")
    public ScorerMatchStateResponse matchState(
            @PathVariable Long matchId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return queryService.matchState(
                matchId,
                userId(jwt),
                privileged(jwt)
        );
    }

    private long userId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }

    private boolean privileged(Jwt jwt) {

        List<String> roles =
                jwt.getClaimAsStringList("roles");

        return roles != null
                && (
                    roles.contains("ORGANIZER")
                    || roles.contains("ADMIN")
                );
    }
}
