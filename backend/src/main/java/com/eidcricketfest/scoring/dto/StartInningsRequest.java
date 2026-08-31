package com.eidcricketfest.scoring.dto;

import jakarta.validation.constraints.NotNull;

public record StartInningsRequest(

        @NotNull Long strikerPlayingXiId,
        @NotNull Long nonStrikerPlayingXiId,
        @NotNull Long bowlerPlayingXiId
) {}
