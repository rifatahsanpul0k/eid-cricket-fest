package com.eidcricketfest.match.controller;

import com.eidcricketfest.match.dto.NoResultRequest;
import com.eidcricketfest.match.dto.ResolveKnockoutMatchRequest;
import com.eidcricketfest.match.service.MatchResultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Match Results")
@RestController
@RequestMapping("/api/v1/matches")
@SecurityRequirement(name = "bearerAuth")
public class MatchResultController {

    private final MatchResultService service;

    public MatchResultController(
            MatchResultService service
    ) {
        this.service = service;
    }

    @Operation(summary = "Mark match as no result")
    @PatchMapping("/{matchId}/no-result")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void noResult(
            @PathVariable Long matchId,
            @Valid @RequestBody NoResultRequest request
    ) {

        service.markNoResult(
                matchId,
                request
        );
    }

    @Operation(summary = "Resolve knockout winner")
    @PatchMapping(
            "/{matchId}/knockout-winner"
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resolveKnockout(
            @PathVariable Long matchId,
            @Valid
            @RequestBody
            ResolveKnockoutMatchRequest request
    ) {

        service.resolveKnockout(
                matchId,
                request
        );
    }
}
