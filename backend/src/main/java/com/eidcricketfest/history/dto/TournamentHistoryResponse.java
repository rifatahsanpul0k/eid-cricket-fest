package com.eidcricketfest.history.dto;

import com.eidcricketfest.award.dto.PlayerAwardResponse;

import java.time.LocalDate;
import java.util.List;

public record TournamentHistoryResponse(

        Long tournamentId,
        String tournamentName,

        List<Edition> editions

) {

    public record Edition(

            Long editionId,
            String name,

            LocalDate startDate,
            LocalDate endDate,

            Team champion,
            Team runnerUp,

            String finalResult,

            PlayerStat topRunScorer,
            PlayerStat topWicketTaker,

            List<PlayerAwardResponse> awards
    ) {}

    public record Team(
            Long tournamentTeamId,
            String name
    ) {}

    public record PlayerStat(
            Long playerId,
            String name,
            int value
    ) {}
}
