package com.eidcricketfest.draft.dto;

import com.eidcricketfest.draft.entity.DraftPickMode;
import jakarta.validation.constraints.NotNull;

public record CreateDraftRequest(

        @NotNull
        DraftPickMode pickMode
) {}
