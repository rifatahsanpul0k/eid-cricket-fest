package com.eidcricketfest.scoring.dto;

import jakarta.validation.constraints.NotNull;

public record SetBowlerRequest(
        @NotNull Long bowlerPlayingXiId
) {}
