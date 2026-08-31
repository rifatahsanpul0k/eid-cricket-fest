package com.eidcricketfest.player.controller;

import com.eidcricketfest.common.dto.PageResponse;
import com.eidcricketfest.player.dto.*;
import com.eidcricketfest.player.service.PlayerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Players")
@RestController
@RequestMapping("/api/v1/players")
public class PlayerController {

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @Operation(summary = "Create my player profile")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/me")
    @ResponseStatus(HttpStatus.CREATED)
    public PlayerResponse createMyProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreatePlayerRequest request
    ) {

        Long userId =
                Long.valueOf(jwt.getSubject());

        return playerService.createMyProfile(
                userId,
                request
        );
    }

    @Operation(summary = "Get my player profile")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    public PlayerResponse getMyProfile(
            @AuthenticationPrincipal Jwt jwt
    ) {

        return playerService.getMyProfile(
                Long.valueOf(jwt.getSubject())
        );
    }

    @Operation(summary = "Create player manually")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlayerResponse createManualPlayer(
            @Valid @RequestBody CreatePlayerRequest request
    ) {
        return playerService.createManualPlayer(request);
    }

    @Operation(summary = "Search players")
    @GetMapping
    public PageResponse<PlayerResponse> searchPlayers(
            @RequestParam(required = false)
            String q,
            @RequestParam(required = false)
            String category,
            @RequestParam(defaultValue = "0")
            Integer page,
            @RequestParam(defaultValue = "20")
            Integer size,
            @RequestParam(defaultValue = "name")
            String sortBy,
            @RequestParam(defaultValue = "asc")
            String direction
    ) {
        return playerService.search(
                q,
                category,
                page,
                size,
                sortBy,
                direction
        );
    }

    @Operation(summary = "Get public player profile")
    @GetMapping("/{id}")
    public PlayerResponse getPlayer(
            @PathVariable Long id
    ) {
        return playerService.getPlayer(id);
    }
}
