package com.eidcricketfest.match.event;

import com.eidcricketfest.match.entity.MatchStage;

public record MatchCompletedEvent(

        Long matchId,
        Long tournamentEditionId,
        MatchStage stage

) {}
