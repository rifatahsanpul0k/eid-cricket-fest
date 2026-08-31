package com.eidcricketfest.scoring.entity;

import com.eidcricketfest.auth.entity.User;
import com.eidcricketfest.match.entity.PlayingXiEntry;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "deliveries")
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "innings_id")
    private Innings innings;

    @Column(name = "match_id", nullable = false)
    private Long matchId;

    @Column(name = "sequence_no", nullable = false)
    private Integer sequenceNo;

    @Column(name = "client_event_id")
    private UUID clientEventId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "striker_xi_id")
    private PlayingXiEntry striker;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "non_striker_xi_id")
    private PlayingXiEntry nonStriker;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bowler_xi_id")
    private PlayingXiEntry bowler;

    @Column(name = "runs_off_bat", nullable = false)
    private Integer runsOffBat;

    @Column(name = "wide_runs", nullable = false)
    private Integer wideRuns;

    @Column(name = "no_ball_runs", nullable = false)
    private Integer noBallRuns;

    @Column(name = "bye_runs", nullable = false)
    private Integer byeRuns;

    @Column(name = "leg_bye_runs", nullable = false)
    private Integer legByeRuns;

    @Column(name = "penalty_runs", nullable = false)
    private Integer penaltyRuns;

    @Column(name = "swap_ends", nullable = false)
    private boolean swapEnds;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "voided_at")
    private Instant voidedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voided_by_user_id")
    private User voidedBy;

    @Column(name = "void_reason", columnDefinition = "TEXT")
    private String voidReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "correction_of_delivery_id")
    private Delivery correctionOf;

    @Column(columnDefinition = "TEXT")
    private String commentary;

    @Version
    private Long version;

    protected Delivery() {}

    public Delivery(
            Innings innings,
            Integer sequenceNo,
            UUID clientEventId,
            PlayingXiEntry striker,
            PlayingXiEntry nonStriker,
            PlayingXiEntry bowler,
            Integer runsOffBat,
            Integer wideRuns,
            Integer noBallRuns,
            Integer byeRuns,
            Integer legByeRuns,
            Integer penaltyRuns,
            boolean swapEnds,
            User createdBy
    ) {
        this.innings = innings;
        this.matchId = innings.getMatch().getId();

        this.sequenceNo = sequenceNo;
        this.clientEventId = clientEventId;

        this.striker = striker;
        this.nonStriker = nonStriker;
        this.bowler = bowler;

        this.runsOffBat = runsOffBat;
        this.wideRuns = wideRuns;
        this.noBallRuns = noBallRuns;
        this.byeRuns = byeRuns;
        this.legByeRuns = legByeRuns;
        this.penaltyRuns = penaltyRuns;

        this.swapEnds = swapEnds;
        this.createdBy = createdBy;

        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public Delivery(
            Innings innings,
            Integer sequenceNo,
            UUID clientEventId,
            PlayingXiEntry striker,
            PlayingXiEntry nonStriker,
            PlayingXiEntry bowler,
            Integer runsOffBat,
            Integer wideRuns,
            Integer noBallRuns,
            Integer byeRuns,
            Integer legByeRuns,
            Integer penaltyRuns,
            boolean swapEnds,
            User createdBy,
            Delivery correctionOf,
            String commentary
    ) {
        this(
                innings,
                sequenceNo,
                clientEventId,
                striker,
                nonStriker,
                bowler,
                runsOffBat,
                wideRuns,
                noBallRuns,
                byeRuns,
                legByeRuns,
                penaltyRuns,
                swapEnds,
                createdBy
        );

        this.correctionOf = correctionOf;
        this.commentary = commentary;
    }

    public int calculateTotalRuns() {
        return runsOffBat
                + wideRuns
                + noBallRuns
                + byeRuns
                + legByeRuns
                + penaltyRuns;
    }

    public boolean isLegal() {
        return wideRuns == 0
                && noBallRuns == 0;
    }

    public void voidDelivery(
            User user,
            String reason
    ) {
        this.voidedAt = Instant.now();
        this.voidedBy = user;
        this.voidReason = reason;
        this.updatedAt = Instant.now();
    }

    public Innings getInnings() {
        return innings;
    }

    public Long getId() { return id; }

    public Integer getSequenceNo() { return sequenceNo; }

    public UUID getClientEventId() {
        return clientEventId;
    }

    public PlayingXiEntry getStriker() { return striker; }

    public PlayingXiEntry getNonStriker() { return nonStriker; }

    public PlayingXiEntry getBowler() { return bowler; }

    public Integer getRunsOffBat() { return runsOffBat; }

    public Integer getWideRuns() { return wideRuns; }

    public Integer getNoBallRuns() { return noBallRuns; }

    public Integer getByeRuns() { return byeRuns; }

    public Integer getLegByeRuns() { return legByeRuns; }

    public Integer getPenaltyRuns() { return penaltyRuns; }

    public boolean isSwapEnds() { return swapEnds; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getVoidedAt() { return voidedAt; }

    public Delivery getCorrectionOf() {
        return correctionOf;
    }

    public String getCommentary() {
        return commentary;
    }
}
