package com.eidcricketfest.history.controller;

import com.eidcricketfest.history.dto.TournamentHistoryResponse;
import com.eidcricketfest.history.service.TournamentHistoryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Tournament History")
@RestController
@RequestMapping("/api/v1/tournaments")
public class TournamentHistoryController {

    private final TournamentHistoryService historyService;

    public TournamentHistoryController(
            TournamentHistoryService historyService
    ) {
        this.historyService = historyService;
    }

    @GetMapping("/{tournamentId}/history")
    public TournamentHistoryResponse history(
            @PathVariable Long tournamentId
    ) {
        return historyService.history(
                tournamentId
        );
    }
}
