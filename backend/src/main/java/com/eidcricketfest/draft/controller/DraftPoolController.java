package com.eidcricketfest.draft.controller;

import com.eidcricketfest.draft.dto.DraftPoolPlayerResponse;
import com.eidcricketfest.draft.service.DraftPoolService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Draft")
@RestController
@RequestMapping("/api/v1/tournament-editions")
@SecurityRequirement(name = "bearerAuth")
public class DraftPoolController {

    private final DraftPoolService draftPoolService;

    public DraftPoolController(
            DraftPoolService draftPoolService
    ) {
        this.draftPoolService = draftPoolService;
    }

    @Operation(summary = "Get draft pool")
    @GetMapping("/{editionId}/draft-pool")
    public List<DraftPoolPlayerResponse> getDraftPool(
            @PathVariable Long editionId
    ) {
        return draftPoolService.getDraftPool(editionId);
    }
}
