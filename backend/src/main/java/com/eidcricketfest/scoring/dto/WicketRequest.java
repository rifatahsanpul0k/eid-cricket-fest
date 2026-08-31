package com.eidcricketfest.scoring.dto;

import com.eidcricketfest.scoring.entity.DismissalType;
import jakarta.validation.constraints.NotNull;

public record WicketRequest(

        @NotNull
        DismissalType dismissalType,

        @NotNull
        Long dismissedPlayingXiId,

        Long fielderPlayingXiId
) {}
