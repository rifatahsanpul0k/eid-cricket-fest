package com.eidcricketfest.match.dto;

import java.util.List;

public record MatchSetupDetailsResponse(
        List<ScorerAssignment> scorers,
        TeamPlayingXi teamAPlayingXi,
        TeamPlayingXi teamBPlayingXi
) {

    public record ScorerAssignment(
            Long userId,
            String displayName,
            String email,
            boolean primary
    ) {}

    public record TeamPlayingXi(
            Long tournamentTeamId,
            List<Long> registrationIds,
            Long wicketkeeperRegistrationId
    ) {}
}
