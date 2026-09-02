package com.eidcricketfest.match.dto;

import com.eidcricketfest.match.entity.*;

import java.time.Instant;

public record MatchOperationHistoryResponse(
        Long id,
        MatchOperationType operationType,
        Long actorUserId,
        String actorName,
        String reason,
        MatchStatus oldStatus,
        MatchStatus newStatus,
        MatchResultStatus oldResultStatus,
        MatchResultStatus newResultStatus,
        String metadata,
        Long relatedMatchId,
        Instant createdAt
) {}
