package com.eidcricketfest.standings.controller;

import com.eidcricketfest.standings.dto.StandingsResponse;
import com.eidcricketfest.standings.service.StandingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Standings")
@RestController
@RequestMapping(
        "/api/v1/tournament-editions"
)
public class StandingsController {

    private final StandingsService standingsService;

    public StandingsController(
            StandingsService standingsService
    ) {
        this.standingsService =
                standingsService;
    }

    @Operation(summary = "Get tournament edition standings")
    @GetMapping("/{editionId}/standings")
    public StandingsResponse standings(
            @PathVariable Long editionId
    ) {
        return standingsService
                .getStandings(editionId);
    }
}
