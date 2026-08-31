package com.eidcricketfest.draft.controller;

import com.eidcricketfest.draft.dto.*;
import com.eidcricketfest.draft.service.DraftService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Draft")
@RestController
@RequestMapping("/api/v1")
public class DraftController {

    private final DraftService draftService;

    public DraftController(DraftService draftService) {
        this.draftService = draftService;
    }

    @PostMapping(
            "/tournament-editions/{editionId}/draft"
    )
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "bearerAuth")
    public DraftStateResponse createDraft(
            @PathVariable Long editionId,
            @Valid @RequestBody CreateDraftRequest request
    ) {
        return draftService.createDraft(
                editionId,
                request
        );
    }

    @PostMapping("/drafts/{draftId}/lottery")
    @SecurityRequirement(name = "bearerAuth")
    public DraftStateResponse generateLottery(
            @PathVariable Long draftId
    ) {
        return draftService.generateLottery(draftId);
    }

    @PostMapping("/drafts/{draftId}/start")
    @SecurityRequirement(name = "bearerAuth")
    public DraftStateResponse start(
            @PathVariable Long draftId
    ) {
        return draftService.startDraft(draftId);
    }

    @PostMapping("/drafts/{draftId}/picks")
    @SecurityRequirement(name = "bearerAuth")
    public DraftPickResponse makePick(
            @PathVariable Long draftId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody DraftPickRequest request
    ) {

        List<String> roles =
                jwt.getClaimAsStringList("roles");

        boolean privileged =
                roles != null
                && (
                    roles.contains("ORGANIZER")
                    || roles.contains("ADMIN")
                );

        return draftService.makePick(
                draftId,
                Long.valueOf(jwt.getSubject()),
                privileged,
                request
        );
    }

    @GetMapping(
            "/tournament-editions/{editionId}/draft"
    )
    public DraftStateResponse getDraft(
            @PathVariable Long editionId
    ) {
        return draftService.getDraftByEdition(
                editionId
        );
    }

    @GetMapping("/drafts/{draftId}/picks")
    public List<DraftPickResponse> getPicks(
            @PathVariable Long draftId
    ) {
        return draftService.getPicks(draftId);
    }
}
