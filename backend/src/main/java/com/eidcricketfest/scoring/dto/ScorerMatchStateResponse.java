package com.eidcricketfest.scoring.dto;

import com.eidcricketfest.match.dto.MatchResponse;

import java.util.List;

public record ScorerMatchStateResponse(
        MatchResponse match,
        LiveMatchResponse live,
        List<PlayingXiPlayer> teamAPlayingXi,
        List<PlayingXiPlayer> teamBPlayingXi,
        Long nextInningsBattingTeamId,
        Long nextInningsBowlingTeamId,
        boolean assignedToCurrentUser
) {

    public record PlayingXiPlayer(
            Long playingXiId,
            Long tournamentTeamId,
            String teamName,
            Long playerId,
            String playerName,
            boolean captain,
            boolean wicketkeeper
    ) {}
}
