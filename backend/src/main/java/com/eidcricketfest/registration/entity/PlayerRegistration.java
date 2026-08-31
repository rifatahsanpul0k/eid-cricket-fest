package com.eidcricketfest.registration.entity;

import com.eidcricketfest.auth.entity.User;
import com.eidcricketfest.common.entity.BaseEntity;
import com.eidcricketfest.player.entity.*;
import com.eidcricketfest.tournament.entity.TournamentEdition;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "player_registrations")
public class PlayerRegistration extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tournament_edition_id")
    private TournamentEdition tournamentEdition;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id")
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id")
    private PlayerCategory category;

    @Column(
            name = "fee_amount",
            nullable = false,
            precision = 10,
            scale = 2
    )
    private BigDecimal feeAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RegistrationStatus status;

    @Column(name = "registered_at", nullable = false)
    private Instant registeredAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_user_id")
    private User approvedBy;

    @Column(columnDefinition = "TEXT")
    private String notes;
    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rejected_by_user_id")
    private User rejectedBy;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Version
    @Column(nullable = false)
    private Long version;

    protected PlayerRegistration() {
    }

    public PlayerRegistration(
            TournamentEdition tournamentEdition,
            Player player,
            PlayerCategory category,
            BigDecimal feeAmount
    ) {
        this.tournamentEdition = tournamentEdition;
        this.player = player;
        this.category = category;
        this.feeAmount = feeAmount;
        this.status = RegistrationStatus.PENDING;
        this.registeredAt = Instant.now();
    }

    public TournamentEdition getTournamentEdition() {
        return tournamentEdition;
    }

    public Player getPlayer() {
        return player;
    }

    public PlayerCategory getCategory() {
        return category;
    }

    public BigDecimal getFeeAmount() {
        return feeAmount;
    }

    public RegistrationStatus getStatus() {
        return status;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }
    public void approve(User approver) {

        if (status != RegistrationStatus.PENDING) {
            throw new IllegalStateException(
                    "Only pending registrations can be approved"
            );
        }

        status = RegistrationStatus.APPROVED;
        approvedBy = approver;
        approvedAt = Instant.now();

        rejectedBy = null;
        rejectedAt = null;
        rejectionReason = null;
    }

    public void reject(User reviewer, String reason) {

        if (status != RegistrationStatus.PENDING) {
            throw new IllegalStateException(
                    "Only pending registrations can be rejected"
            );
        }

        status = RegistrationStatus.REJECTED;

        rejectedBy = reviewer;
        rejectedAt = Instant.now();
        rejectionReason = reason;
    }
    public String getRejectionReason() {
        return rejectionReason;
    }
}