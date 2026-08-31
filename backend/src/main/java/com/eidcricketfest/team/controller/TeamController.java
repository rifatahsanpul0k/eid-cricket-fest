package com.eidcricketfest.team.controller;

import com.eidcricketfest.team.dto.*;
import com.eidcricketfest.team.service.TeamService;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Teams")
@RestController
@RequestMapping("/api/v1")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @Operation(summary = "Create team")
    @PostMapping("/teams")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "bearerAuth")
    public TeamResponse createTeam(
            @Valid @RequestBody CreateTeamRequest request
    ) {
        return teamService.createTeam(request);
    }

    @Operation(summary = "List teams")
    @GetMapping("/teams")
    public List<TeamResponse> getTeams() {
        return teamService.getTeams();
    }

    @Operation(summary = "Add team to tournament edition")
    @PostMapping(
            "/tournament-editions/{editionId}/teams/{teamId}"
    )
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "bearerAuth")
    public TournamentTeamResponse addTeamToEdition(
            @PathVariable Long editionId,
            @PathVariable Long teamId
    ) {
        return teamService.addTeamToEdition(
                editionId,
                teamId
        );
    }

    @Operation(summary = "List tournament edition teams")
    @GetMapping(
            "/tournament-editions/{editionId}/teams"
    )
    public List<TournamentTeamResponse> getEditionTeams(
            @PathVariable Long editionId
    ) {
        return teamService.getEditionTeams(editionId);
    }

    @Operation(summary = "Assign team captain")
    @PatchMapping(
            "/tournament-teams/{tournamentTeamId}/captain"
    )
    @SecurityRequirement(name = "bearerAuth")
    public TournamentTeamResponse assignCaptain(
            @PathVariable Long tournamentTeamId,
            @Valid @RequestBody AssignCaptainRequest request
    ) {
        return teamService.assignCaptain(
                tournamentTeamId,
                request
        );
    }
}
