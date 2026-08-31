package com.eidcricketfest.scoring.dto;

import com.eidcricketfest.match.dto.MatchResponse;

public record ScorerMatchResponse(
        MatchResponse match,
        boolean assignedToCurrentUser
) {}
