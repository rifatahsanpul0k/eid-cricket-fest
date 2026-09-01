package com.eidcricketfest.player.dto;

import com.eidcricketfest.team.entity.AcquisitionType;

import java.util.List;

public record MyTeamResponse(
        Long editionId,
        Long tournamentTeamId,
        Long teamId,
        String teamName,
        String teamShortName,
        String teamLogoUrl,
        Captain captain,
        Me me,
        List<SquadMember> squad
) {

    public record Captain(
            Long registrationId,
            Long playerId,
            String playerName
    ) {}

    public record Me(
            Long registrationId,
            Long playerId,
            String playerName,
            AcquisitionType acquisitionType,
            String jerseyNumber,
            boolean captain
    ) {}

    public record SquadMember(
            Long registrationId,
            Long playerId,
            String playerName,
            String photoUrl,
            String category,
            AcquisitionType acquisitionType,
            String jerseyNumber,
            boolean captain
    ) {}
}
