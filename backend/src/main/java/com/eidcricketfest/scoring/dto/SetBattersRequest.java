package com.eidcricketfest.scoring.dto;

import jakarta.validation.constraints.NotNull;

public record SetBattersRequest(

        @NotNull Long strikerPlayingXiId,
        @NotNull Long nonStrikerPlayingXiId
) {}
