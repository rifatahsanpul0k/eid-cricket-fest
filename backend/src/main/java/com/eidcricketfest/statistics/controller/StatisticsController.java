package com.eidcricketfest.statistics.controller;

import com.eidcricketfest.statistics.dto.TournamentStatisticsResponse;
import com.eidcricketfest.statistics.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Tournament Statistics")
@RestController
@RequestMapping(
        "/api/v1/tournament-editions"
)
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(
            StatisticsService statisticsService
    ) {
        this.statisticsService =
                statisticsService;
    }

    @Operation(summary = "Get tournament statistics")
    @GetMapping("/{editionId}/statistics")
    public TournamentStatisticsResponse statistics(
            @PathVariable Long editionId
    ) {

        return statisticsService
                .statistics(editionId);
    }
}
