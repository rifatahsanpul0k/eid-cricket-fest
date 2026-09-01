package com.eidcricketfest.player.controller;

import com.eidcricketfest.player.dto.MyEditionStatisticsResponse;
import com.eidcricketfest.player.dto.MyMatchResponse;
import com.eidcricketfest.player.dto.MyTeamResponse;
import com.eidcricketfest.player.service.MyCricketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "My Cricket")
@RestController
@RequestMapping("/api/v1/players/me")
public class MyCricketController {

    private final MyCricketService myCricketService;

    public MyCricketController(
            MyCricketService myCricketService
    ) {
        this.myCricketService = myCricketService;
    }

    @Operation(summary = "Get my tournament team")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/team")
    public MyTeamResponse getMyTeam(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam Long editionId
    ) {
        return myCricketService.getMyTeam(
                Long.valueOf(jwt.getSubject()),
                editionId
        );
    }

    @Operation(summary = "Get my tournament matches")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/matches")
    public List<MyMatchResponse> getMyMatches(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam Long editionId
    ) {
        return myCricketService.getMyMatches(
                Long.valueOf(jwt.getSubject()),
                editionId
        );
    }

    @Operation(summary = "Get my tournament statistics")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/statistics")
    public MyEditionStatisticsResponse getMyStatistics(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam Long editionId
    ) {
        return myCricketService.getMyStatistics(
                Long.valueOf(jwt.getSubject()),
                editionId
        );
    }
}
