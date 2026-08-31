package com.eidcricketfest.scoring.entity;

import com.eidcricketfest.common.entity.BaseEntity;
import com.eidcricketfest.match.entity.CricketMatch;
import com.eidcricketfest.match.entity.PlayingXiEntry;
import com.eidcricketfest.team.entity.TournamentTeam;
import com.eidcricketfest.tournament.entity.TournamentEdition;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "innings")
public class Innings extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id")
    private CricketMatch match;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tournament_edition_id")
    private TournamentEdition tournamentEdition;

    @Column(name = "innings_number", nullable = false)
    private Short inningsNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batting_team_id")
    private TournamentTeam battingTeam;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bowling_team_id")
    private TournamentTeam bowlingTeam;

    @Column(name = "target_runs")
    private Integer targetRuns;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InningsStatus status;

    @Column(name = "total_runs", nullable = false)
    private Integer totalRuns;

    @Column(nullable = false)
    private Integer wickets;

    @Column(name = "legal_balls", nullable = false)
    private Integer legalBalls;

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

    @Column(name = "score_revision", nullable = false)
    private long scoreRevision;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_striker_xi_id")
    private PlayingXiEntry currentStriker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_non_striker_xi_id")
    private PlayingXiEntry currentNonStriker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_bowler_xi_id")
    private PlayingXiEntry currentBowler;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Version
    private Long version;

    protected Innings() {}

    public Innings(
            CricketMatch match,
            short inningsNumber,
            TournamentTeam battingTeam,
            TournamentTeam bowlingTeam,
            Integer targetRuns
    ) {
        this.match = match;
        this.tournamentEdition =
                match.getTournamentEdition();

        this.inningsNumber = inningsNumber;
        this.battingTeam = battingTeam;
        this.bowlingTeam = bowlingTeam;
        this.targetRuns = targetRuns;

        this.status = InningsStatus.IN_PROGRESS;

        this.totalRuns = 0;
        this.wickets = 0;
        this.legalBalls = 0;

        this.wideRuns = 0;
        this.noBallRuns = 0;
        this.byeRuns = 0;
        this.legByeRuns = 0;
        this.penaltyRuns = 0;
        this.scoreRevision = 0;

        this.startedAt = Instant.now();
    }

    public void setBatters(
            PlayingXiEntry striker,
            PlayingXiEntry nonStriker
    ) {
        this.currentStriker = striker;
        this.currentNonStriker = nonStriker;
    }

    public void setBowler(PlayingXiEntry bowler) {
        this.currentBowler = bowler;
    }

    public void clearBatters() {
        currentStriker = null;
        currentNonStriker = null;
    }

    public void clearBowler() {
        currentBowler = null;
    }

    public void swapBatters() {
        PlayingXiEntry temp = currentStriker;
        currentStriker = currentNonStriker;
        currentNonStriker = temp;
    }

    public void applyDelivery(
            Delivery delivery,
            boolean wicket
    ) {
        totalRuns += delivery.calculateTotalRuns();

        wideRuns += delivery.getWideRuns();
        noBallRuns += delivery.getNoBallRuns();
        byeRuns += delivery.getByeRuns();
        legByeRuns += delivery.getLegByeRuns();
        penaltyRuns += delivery.getPenaltyRuns();

        if (delivery.isLegal()) {
            legalBalls++;
        }

        if (wicket) {
            wickets++;
        }
    }

    public void rollbackDelivery(
            Delivery delivery,
            boolean wicket
    ) {
        totalRuns -= delivery.calculateTotalRuns();

        wideRuns -= delivery.getWideRuns();
        noBallRuns -= delivery.getNoBallRuns();
        byeRuns -= delivery.getByeRuns();
        legByeRuns -= delivery.getLegByeRuns();
        penaltyRuns -= delivery.getPenaltyRuns();

        if (delivery.isLegal()) {
            legalBalls--;
        }

        if (wicket) {
            wickets--;
        }

        currentStriker = delivery.getStriker();
        currentNonStriker = delivery.getNonStriker();
        currentBowler = delivery.getBowler();
    }

    public void complete() {
        status = InningsStatus.COMPLETED;
        endedAt = Instant.now();

        currentStriker = null;
        currentNonStriker = null;
        currentBowler = null;
    }

    public void replaceAggregates(
            int totalRuns,
            int wickets,
            int legalBalls,
            int wideRuns,
            int noBallRuns,
            int byeRuns,
            int legByeRuns,
            int penaltyRuns
    ) {
        this.totalRuns = totalRuns;
        this.wickets = wickets;
        this.legalBalls = legalBalls;

        this.wideRuns = wideRuns;
        this.noBallRuns = noBallRuns;
        this.byeRuns = byeRuns;
        this.legByeRuns = legByeRuns;
        this.penaltyRuns = penaltyRuns;
    }

    public CricketMatch getMatch() { return match; }

    public Short getInningsNumber() { return inningsNumber; }

    public TournamentTeam getBattingTeam() { return battingTeam; }

    public TournamentTeam getBowlingTeam() { return bowlingTeam; }

    public Integer getTargetRuns() { return targetRuns; }

    public InningsStatus getStatus() { return status; }

    public Integer getTotalRuns() { return totalRuns; }

    public Integer getWickets() { return wickets; }

    public Integer getLegalBalls() { return legalBalls; }

    public Integer getWideRuns() { return wideRuns; }

    public Integer getNoBallRuns() { return noBallRuns; }

    public Integer getByeRuns() { return byeRuns; }

    public Integer getLegByeRuns() { return legByeRuns; }

    public Integer getPenaltyRuns() { return penaltyRuns; }

    public PlayingXiEntry getCurrentStriker() { return currentStriker; }

    public PlayingXiEntry getCurrentNonStriker() { return currentNonStriker; }

    public PlayingXiEntry getCurrentBowler() { return currentBowler; }

    public long getScoreRevision() { return scoreRevision; }

    public void incrementScoreRevision() {
        scoreRevision++;
    }
}
