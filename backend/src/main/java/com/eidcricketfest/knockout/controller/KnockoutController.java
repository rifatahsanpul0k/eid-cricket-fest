package com.eidcricketfest.knockout.controller;

import com.eidcricketfest.knockout.dto.KnockoutBracketResponse;
import com.eidcricketfest.knockout.service.KnockoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Knockout")
@RestController
@RequestMapping(
        "/api/v1/tournament-editions"
)
public class KnockoutController {

    private final KnockoutService knockoutService;

    public KnockoutController(
            KnockoutService knockoutService
    ) {
        this.knockoutService =
                knockoutService;
    }

    @Operation(summary = "Generate knockout semi-finals")
    @PostMapping(
            "/{editionId}/knockout/semi-finals"
    )
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "bearerAuth")
    public KnockoutBracketResponse generateSemiFinals(
            @PathVariable Long editionId
    ) {

        return knockoutService
                .generateSemiFinals(
                        editionId
                );
    }

    @Operation(summary = "Get knockout bracket")
    @GetMapping(
            "/{editionId}/knockout"
    )
    public KnockoutBracketResponse bracket(
            @PathVariable Long editionId
    ) {

        return knockoutService
                .getBracket(editionId);
    }
}
