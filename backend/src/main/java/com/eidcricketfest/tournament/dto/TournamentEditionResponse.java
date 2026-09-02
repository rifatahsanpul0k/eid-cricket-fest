package com.eidcricketfest.tournament.dto;

import com.eidcricketfest.tournament.entity.TournamentEditionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record TournamentEditionResponse(
        Long id,
        Long tournamentId,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        Instant registrationStartAt,
        Instant registrationEndAt,
        Integer oversPerInnings,
        Integer squadSize,
        Integer playingXiSize,
        BigDecimal registrationFee,
        String registrationCurrency,
        BigDecimal winPoints,
        BigDecimal tiePoints,
        BigDecimal noResultPoints,
        BigDecimal lossPoints,
        TournamentEditionStatus status,
        TournamentEditionTeamResponse champion,
        TournamentEditionTeamResponse runnerUp,
        Long finalMatchId,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
