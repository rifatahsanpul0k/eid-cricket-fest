package com.eidcricketfest.draft.entity;

import com.eidcricketfest.common.entity.BaseEntity;
import com.eidcricketfest.tournament.entity.TournamentEdition;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "drafts")
public class Draft extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tournament_edition_id", nullable = false)
    private TournamentEdition tournamentEdition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DraftStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "pick_mode", nullable = false, length = 20)
    private DraftPickMode pickMode;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected Draft() {}

    public Draft(
            TournamentEdition tournamentEdition,
            DraftPickMode pickMode
    ) {
        this.tournamentEdition = tournamentEdition;
        this.pickMode = pickMode;
        this.status = DraftStatus.PENDING;
    }

    public void markOrderGenerated() {
        if (status != DraftStatus.PENDING) {
            throw new IllegalStateException(
                    "Draft order cannot be generated now"
            );
        }

        status = DraftStatus.ORDER_GENERATED;
    }

    public void start() {
        if (status != DraftStatus.ORDER_GENERATED) {
            throw new IllegalStateException(
                    "Draft cannot be started"
            );
        }

        status = DraftStatus.IN_PROGRESS;
        startedAt = Instant.now();
    }

    public void complete() {
        status = DraftStatus.COMPLETED;
        completedAt = Instant.now();
    }

    public TournamentEdition getTournamentEdition() {
        return tournamentEdition;
    }

    public DraftStatus getStatus() {
        return status;
    }

    public DraftPickMode getPickMode() {
        return pickMode;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
