package com.eidcricketfest.match.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SubmitPlayingXiRequest(

        @NotEmpty
        List<Long> registrationIds,

        Long wicketkeeperRegistrationId
) {}
