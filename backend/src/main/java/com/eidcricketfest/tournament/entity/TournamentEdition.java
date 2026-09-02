package com.eidcricketfest.tournament.entity;

import com.eidcricketfest.common.entity.BaseEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import com.eidcricketfest.match.entity.CricketMatch;
import com.eidcricketfest.team.entity.TournamentTeam;

@Entity
@Table(name = "tournament_editions")
public class TournamentEdition extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "registration_start_at")
    private Instant registrationStartAt;

    @Column(name = "registration_end_at")
    private Instant registrationEndAt;

    @Column(name = "overs_per_innings", nullable = false)
    private Integer oversPerInnings;

    @Column(name = "squad_size", nullable = false)
    private Integer squadSize;

    @Column(name = "playing_xi_size", nullable = false)
    private Integer playingXiSize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TournamentEditionStatus status;

    @Column(
            name = "registration_fee",
            nullable = false,
            precision = 10,
            scale = 2
    )
    private BigDecimal registrationFee;

    @Column(
            name = "registration_currency",
            nullable = false,
            length = 3
    )
    private String registrationCurrency;

    @Column(
            name = "win_points",
            nullable = false,
            precision = 5,
            scale = 2
    )
    private BigDecimal winPoints;

    @Column(
            name = "tie_points",
            nullable = false,
            precision = 5,
            scale = 2
    )
    private BigDecimal tiePoints;

    @Column(
            name = "no_result_points",
            nullable = false,
            precision = 5,
            scale = 2
    )
    private BigDecimal noResultPoints;

    @Column(
            name = "loss_points",
            nullable = false,
            precision = 5,
            scale = 2
    )
    private BigDecimal lossPoints;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "champion_tournament_team_id")
    private TournamentTeam championTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "runner_up_tournament_team_id")
    private TournamentTeam runnerUpTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "final_match_id")
    private CricketMatch finalMatch;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected TournamentEdition() {
    }

    public TournamentEdition(
            Tournament tournament,
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
            BigDecimal lossPoints
    ) {
        this.tournament = tournament;
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.registrationStartAt = registrationStartAt;
        this.registrationEndAt = registrationEndAt;
        this.oversPerInnings = oversPerInnings;
        this.squadSize = squadSize;
        this.playingXiSize = playingXiSize;

        this.registrationFee =
                registrationFee != null
                        ? registrationFee
                        : BigDecimal.ZERO;

        this.registrationCurrency =
                registrationCurrency != null
                        ? registrationCurrency
                        : "BDT";

        this.winPoints =
                winPoints != null
                        ? winPoints
                        : BigDecimal.valueOf(2);

        this.tiePoints =
                tiePoints != null
                        ? tiePoints
                        : BigDecimal.ONE;

        this.noResultPoints =
                noResultPoints != null
                        ? noResultPoints
                        : BigDecimal.ONE;

        this.lossPoints =
                lossPoints != null
                        ? lossPoints
                        : BigDecimal.ZERO;

        this.status = TournamentEditionStatus.DRAFT;
    }

    public Tournament getTournament() {
        return tournament;
    }

    public String getName() {
        return name;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public Instant getRegistrationStartAt() {
        return registrationStartAt;
    }

    public Instant getRegistrationEndAt() {
        return registrationEndAt;
    }

    public Integer getOversPerInnings() {
        return oversPerInnings;
    }

    public Integer getSquadSize() {
        return squadSize;
    }

    public Integer getPlayingXiSize() {
        return playingXiSize;
    }

    public TournamentEditionStatus getStatus() {
        return status;
    }
    public BigDecimal getRegistrationFee() {
        return registrationFee;
    }

    public String getRegistrationCurrency() {
        return registrationCurrency;
    }

    public BigDecimal getWinPoints() {
        return winPoints;
    }

    public BigDecimal getTiePoints() {
        return tiePoints;
    }

    public BigDecimal getNoResultPoints() {
        return noResultPoints;
    }

    public BigDecimal getLossPoints() {
        return lossPoints;
    }

    public TournamentTeam getChampionTeam() {
        return championTeam;
    }

    public TournamentTeam getRunnerUpTeam() {
        return runnerUpTeam;
    }

    public CricketMatch getFinalMatch() {
        return finalMatch;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void updateConfiguration(
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
            BigDecimal lossPoints
    ) {

        if (status != TournamentEditionStatus.DRAFT) {
            throw new IllegalStateException(
                    "Tournament edition configuration can only be changed while draft"
            );
        }

        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.registrationStartAt = registrationStartAt;
        this.registrationEndAt = registrationEndAt;
        this.oversPerInnings = oversPerInnings;
        this.squadSize = squadSize;
        this.playingXiSize = playingXiSize;
        this.registrationFee =
                registrationFee != null
                        ? registrationFee
                        : BigDecimal.ZERO;
        this.registrationCurrency =
                registrationCurrency != null
                        ? registrationCurrency
                        : "BDT";
        this.winPoints =
                winPoints != null
                        ? winPoints
                        : BigDecimal.valueOf(2);
        this.tiePoints =
                tiePoints != null
                        ? tiePoints
                        : BigDecimal.ONE;
        this.noResultPoints =
                noResultPoints != null
                        ? noResultPoints
                        : BigDecimal.ONE;
        this.lossPoints =
                lossPoints != null
                        ? lossPoints
                        : BigDecimal.ZERO;
    }

    public void transitionTo(
            TournamentEditionStatus target
    ) {

        if (!canTransitionTo(target)) {
            throw new IllegalStateException(
                    "Cannot transition tournament edition from "
                            + status
                            + " to "
                            + target
            );
        }

        status = target;
    }

    public void markCompleted(
            TournamentTeam championTeam,
            TournamentTeam runnerUpTeam,
            CricketMatch finalMatch
    ) {

        if (status == TournamentEditionStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Cancelled tournament cannot be completed"
            );
        }

        if (championTeam == null
                || runnerUpTeam == null
                || finalMatch == null) {
            throw new IllegalArgumentException(
                    "Champion, runner-up, and final match are required"
            );
        }

        this.championTeam = championTeam;
        this.runnerUpTeam = runnerUpTeam;
        this.finalMatch = finalMatch;
        completedAt = Instant.now();
        status = TournamentEditionStatus.COMPLETED;
    }

    public void clearCompletion() {

        championTeam = null;
        runnerUpTeam = null;
        finalMatch = null;
        completedAt = null;

        if (status == TournamentEditionStatus.COMPLETED) {
            status = TournamentEditionStatus.ONGOING;
        }
    }

    private boolean canTransitionTo(
            TournamentEditionStatus target
    ) {

        if (target == null
                || status == TournamentEditionStatus.CANCELLED
                || status == TournamentEditionStatus.COMPLETED) {

            return false;
        }

        return switch (status) {
            case DRAFT ->
                    target == TournamentEditionStatus.REGISTRATION_OPEN
                            || target == TournamentEditionStatus.CANCELLED;
            case REGISTRATION_OPEN ->
                    target == TournamentEditionStatus.REGISTRATION_CLOSED
                            || target == TournamentEditionStatus.CANCELLED;
            case REGISTRATION_CLOSED ->
                    target == TournamentEditionStatus.DRAFTING
                            || target == TournamentEditionStatus.CANCELLED;
            case DRAFTING ->
                    target == TournamentEditionStatus.SCHEDULED
                            || target == TournamentEditionStatus.CANCELLED;
            case SCHEDULED ->
                    target == TournamentEditionStatus.ONGOING
                            || target == TournamentEditionStatus.CANCELLED;
            case ONGOING, COMPLETED, CANCELLED -> false;
        };
    }
}
