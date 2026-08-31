package com.eidcricketfest.award.controller;

import com.eidcricketfest.award.dto.*;
import com.eidcricketfest.award.service.AwardService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Tournament Awards")
@RestController
@RequestMapping(
        "/api/v1/tournament-editions"
)
public class AwardController {

    private final AwardService awardService;

    public AwardController(
            AwardService awardService
    ) {
        this.awardService = awardService;
    }

    @PostMapping("/{editionId}/awards")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "bearerAuth")
    public PlayerAwardResponse assign(
            @PathVariable Long editionId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid
            @RequestBody
            AssignPlayerAwardRequest request
    ) {

        return awardService.assignAward(
                editionId,
                Long.valueOf(jwt.getSubject()),
                request
        );
    }

    @GetMapping("/{editionId}/awards")
    public List<PlayerAwardResponse> awards(
            @PathVariable Long editionId
    ) {
        return awardService.getAwards(editionId);
    }
}
