package com.eidcricketfest.team.dto;

import com.eidcricketfest.team.entity.RosterStatus;

public record TournamentTeamResponse(

        Long id,
        Long tournamentEditionId,

        Long teamId,
        String teamName,
        String shortName,

        CaptainInfo captain,

        RosterStatus rosterStatus
) {

    public record CaptainInfo(
            Long registrationId,
            Long playerId,
            String name
    ) {}
}
