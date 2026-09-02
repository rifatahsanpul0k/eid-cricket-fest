package com.eidcricketfest.match.event;

import com.eidcricketfest.match.entity.MatchStage;
import com.eidcricketfest.match.entity.MatchType;

public record MatchCompletedEvent(

        Long matchId,
        Long tournamentEditionId,
        MatchType matchType,
        MatchStage stage

) {}
