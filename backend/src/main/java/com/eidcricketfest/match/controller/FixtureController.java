package com.eidcricketfest.match.controller;

import com.eidcricketfest.common.dto.PageResponse;
import com.eidcricketfest.common.exception.ConflictException;
import com.eidcricketfest.match.dto.*;
import com.eidcricketfest.match.entity.*;
import com.eidcricketfest.match.repository.VenueRepository;
import com.eidcricketfest.match.service.FixtureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Fixtures & Matches")
@RestController
@RequestMapping("/api/v1")
public class FixtureController {

    private final FixtureService fixtureService;
    private final VenueRepository venueRepository;

    public FixtureController(
            FixtureService fixtureService,
            VenueRepository venueRepository
    ) {
        this.fixtureService = fixtureService;
        this.venueRepository = venueRepository;
    }

    @Operation(summary = "Create venue")
    @PostMapping("/venues")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "bearerAuth")
    public Venue createVenue(
            @Valid @RequestBody CreateVenueRequest request
    ) {

        if (venueRepository
                .existsByNameIgnoreCase(request.name().trim())) {

            throw new ConflictException(
                    "Venue already exists"
            );
        }

        return venueRepository.save(
                new Venue(
                        request.name().trim(),
                        request.address()
                )
        );
    }

    @Operation(summary = "Generate round-robin fixtures")
    @PostMapping(
            "/tournament-editions/{editionId}/fixtures/round-robin"
    )
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "bearerAuth")
    public List<MatchResponse> generateRoundRobin(
            @PathVariable Long editionId,
            @RequestBody GenerateRoundRobinRequest request
    ) {
        return fixtureService.generateRoundRobin(
                editionId,
                request
        );
    }

    @Operation(summary = "Search matches")
    @GetMapping(
            "/tournament-editions/{editionId}/matches"
    )
    public PageResponse<MatchResponse> getMatches(
            @PathVariable Long editionId,
            @RequestParam(required = false)
            MatchStatus status,
            @RequestParam(required = false)
            MatchStage stage,
            @RequestParam(required = false)
            Long teamId,
            @RequestParam(defaultValue = "0")
            Integer page,
            @RequestParam(defaultValue = "20")
            Integer size,
            @RequestParam(defaultValue = "matchNumber")
            String sortBy,
            @RequestParam(defaultValue = "asc")
            String direction
    ) {
        return fixtureService
                .searchMatches(
                        editionId,
                        status,
                        stage,
                        teamId,
                        page,
                        size,
                        sortBy,
                        direction
                );
    }

    @Operation(summary = "Schedule match")
    @PatchMapping("/matches/{matchId}/schedule")
    @SecurityRequirement(name = "bearerAuth")
    public MatchResponse scheduleMatch(
            @PathVariable Long matchId,
            @Valid @RequestBody ScheduleMatchRequest request
    ) {
        return fixtureService.scheduleMatch(
                matchId,
                request
        );
    }
}
