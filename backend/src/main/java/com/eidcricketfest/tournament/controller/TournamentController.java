package com.eidcricketfest.tournament.controller;

import com.eidcricketfest.tournament.dto.CreateTournamentEditionRequest;
import com.eidcricketfest.tournament.dto.CreateTournamentRequest;
import com.eidcricketfest.tournament.dto.TournamentEditionResponse;
import com.eidcricketfest.tournament.dto.TournamentResponse;
import com.eidcricketfest.tournament.dto.UpdateTournamentEditionRequest;
import com.eidcricketfest.tournament.dto.UpdateTournamentEditionStatusRequest;
import com.eidcricketfest.tournament.service.TournamentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(
        name = "Tournaments",
        description = "Tournament and tournament edition management"
)
@RestController
@RequestMapping("/api/v1/tournaments")
public class TournamentController {

    private final TournamentService tournamentService;

    public TournamentController(
            TournamentService tournamentService
    ) {
        this.tournamentService = tournamentService;
    }

    @Operation(
            summary = "Create tournament",
            description = "Creates a permanent tournament."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TournamentResponse createTournament(
            @Valid @RequestBody CreateTournamentRequest request
    ) {
        return tournamentService.createTournament(request);
    }

    @Operation(
            summary = "Get tournament",
            description = "Returns a tournament by its ID."
    )
    @GetMapping("/{id}")
    public TournamentResponse getTournament(
            @PathVariable Long id
    ) {
        return tournamentService.getTournament(id);
    }

    @Operation(
            summary = "Get all tournaments"
    )
    @GetMapping
    public List<TournamentResponse> getTournaments() {
        return tournamentService.getTournaments();
    }

    @Operation(
            summary = "Create tournament edition",
            description = "Creates a new edition/season under an existing tournament."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{tournamentId}/editions")
    @ResponseStatus(HttpStatus.CREATED)
    public TournamentEditionResponse createEdition(
            @PathVariable Long tournamentId,
            @Valid @RequestBody CreateTournamentEditionRequest request
    ) {
        return tournamentService.createEdition(
                tournamentId,
                request
        );
    }

    @Operation(
            summary = "Update tournament edition",
            description = "Updates draft-only edition configuration."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{tournamentId}/editions/{editionId}")
    public TournamentEditionResponse updateEdition(
            @PathVariable Long tournamentId,
            @PathVariable Long editionId,
            @Valid @RequestBody UpdateTournamentEditionRequest request
    ) {
        return tournamentService.updateEdition(
                tournamentId,
                editionId,
                request
        );
    }

    @Operation(
            summary = "Transition tournament edition status",
            description = "Applies a controlled tournament lifecycle transition."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{tournamentId}/editions/{editionId}/status")
    public TournamentEditionResponse updateEditionStatus(
            @PathVariable Long tournamentId,
            @PathVariable Long editionId,
            @Valid @RequestBody UpdateTournamentEditionStatusRequest request
    ) {
        return tournamentService.transitionEditionStatus(
                tournamentId,
                editionId,
                request
        );
    }

    @Operation(
            summary = "Get tournament editions",
            description = "Returns all editions of a tournament."
    )
    @GetMapping("/{tournamentId}/editions")
    public List<TournamentEditionResponse> getEditions(
            @PathVariable Long tournamentId
    ) {
        return tournamentService.getEditions(tournamentId);
    }
}
