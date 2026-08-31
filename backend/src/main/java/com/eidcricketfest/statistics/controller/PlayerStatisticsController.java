package com.eidcricketfest.statistics.controller;

import com.eidcricketfest.statistics.dto.PlayerCareerResponse;
import com.eidcricketfest.statistics.service.StatisticsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Player Statistics")
@RestController
@RequestMapping("/api/v1/players")
public class PlayerStatisticsController {

    private final StatisticsService statisticsService;

    public PlayerStatisticsController(
            StatisticsService statisticsService
    ) {
        this.statisticsService =
                statisticsService;
    }

    @GetMapping("/{playerId}/career")
    public PlayerCareerResponse career(
            @PathVariable Long playerId
    ) {
        return statisticsService
                .playerCareer(playerId);
    }
}
