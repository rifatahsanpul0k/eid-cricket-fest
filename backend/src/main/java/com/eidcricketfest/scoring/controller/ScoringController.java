package com.eidcricketfest.scoring.controller;

import com.eidcricketfest.scoring.dto.*;
import com.eidcricketfest.scoring.service.LiveCentreService;
import com.eidcricketfest.scoring.service.LiveScoreService;
import com.eidcricketfest.scoring.service.ScorecardService;
import com.eidcricketfest.scoring.service.ScoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Live Scoring")
@RestController
@RequestMapping("/api/v1")
public class ScoringController {

    private final ScoringService scoringService;
    private final LiveScoreService liveScoreService;
    private final LiveCentreService liveCentreService;
    private final ScorecardService scorecardService;

    public ScoringController(
            ScoringService scoringService,
            LiveScoreService liveScoreService,
            LiveCentreService liveCentreService,
            ScorecardService scorecardService
    ) {
        this.scoringService = scoringService;
        this.liveScoreService = liveScoreService;
        this.liveCentreService = liveCentreService;
        this.scorecardService = scorecardService;
    }

    @Operation(summary = "Start innings")
    @PostMapping("/matches/{matchId}/innings")
    @SecurityRequirement(name = "bearerAuth")
    public InningsResponse startInnings(
            @PathVariable Long matchId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody StartInningsRequest request
    ) {

        return scoringService.startInnings(
                matchId,
                userId(jwt),
                privileged(jwt),
                request
        );
    }

    @Operation(summary = "Set current batters")
    @PutMapping("/innings/{inningsId}/batters")
    @SecurityRequirement(name = "bearerAuth")
    public InningsResponse setBatters(
            @PathVariable Long inningsId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SetBattersRequest request
    ) {

        return scoringService.setBatters(
                inningsId,
                userId(jwt),
                privileged(jwt),
                request
        );
    }

    @Operation(summary = "Set current bowler")
    @PutMapping("/innings/{inningsId}/bowler")
    @SecurityRequirement(name = "bearerAuth")
    public InningsResponse setBowler(
            @PathVariable Long inningsId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SetBowlerRequest request
    ) {

        return scoringService.setBowler(
                inningsId,
                userId(jwt),
                privileged(jwt),
                request
        );
    }

    @Operation(summary = "Record a delivery")
    @PostMapping("/innings/{inningsId}/deliveries")
    @SecurityRequirement(name = "bearerAuth")
    public InningsResponse scoreBall(
            @PathVariable Long inningsId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody RecordDeliveryRequest request
    ) {

        return scoringService.recordDelivery(
                inningsId,
                userId(jwt),
                privileged(jwt),
                request
        );
    }

    @Operation(summary = "Undo latest delivery")
    @PostMapping(
            "/innings/{inningsId}/deliveries/undo"
    )
    @SecurityRequirement(name = "bearerAuth")
    public InningsResponse undo(
            @PathVariable Long inningsId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UndoDeliveryRequest request
    ) {

        return scoringService.undoLastDelivery(
                inningsId,
                userId(jwt),
                privileged(jwt),
                request
        );
    }

    @Operation(summary = "Correct a delivery")
    @PatchMapping("/deliveries/{deliveryId}")
    @SecurityRequirement(name = "bearerAuth")
    public InningsResponse correctDelivery(
            @PathVariable Long deliveryId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CorrectDeliveryRequest request
    ) {

        return scoringService.correctDelivery(
                deliveryId,
                userId(jwt),
                privileged(jwt),
                request
        );
    }

    @Operation(summary = "Get live match state")
    @GetMapping("/matches/{matchId}/live")
    public LiveMatchResponse live(
            @PathVariable Long matchId
    ) {
        return liveScoreService.getLiveMatch(matchId);
    }

    @Operation(summary = "Get public live centre matches")
    @GetMapping("/matches/live-centre")
    public List<LiveCentreMatchResponse> liveCentre() {
        return liveCentreService.getLiveCentreMatches();
    }

    @Operation(summary = "Get match scorecard")
    @GetMapping("/matches/{matchId}/scorecard")
    public ScorecardResponse scorecard(
            @PathVariable Long matchId
    ) {
        return scorecardService.getScorecard(matchId);
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
