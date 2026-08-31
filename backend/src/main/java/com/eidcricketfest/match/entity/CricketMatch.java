package com.eidcricketfest.match.entity;

import com.eidcricketfest.common.entity.BaseEntity;
import com.eidcricketfest.team.entity.TournamentTeam;
import com.eidcricketfest.tournament.entity.TournamentEdition;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "matches")
public class CricketMatch extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tournament_edition_id")
    private TournamentEdition tournamentEdition;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_a_id")
    private TournamentTeam teamA;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_b_id")
    private TournamentTeam teamB;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id")
    private Venue venue;

    @Column(name = "team_a_seed")
    private Integer teamASeed;

    @Column(name = "team_b_seed")
    private Integer teamBSeed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_match_a_id")
    private CricketMatch sourceMatchA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_match_b_id")
    private CricketMatch sourceMatchB;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MatchStage stage;

    @Column(name = "round_number")
    private Integer roundNumber;

    @Column(name = "match_number", nullable = false)
    private Integer matchNumber;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "actual_started_at")
    private Instant actualStartedAt;

    @Column(name = "actual_ended_at")
    private Instant actualEndedAt;

    @Column(name = "overs_per_innings", nullable = false)
    private Integer oversPerInnings;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MatchStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_team_id")
    private TournamentTeam winnerTeam;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_type", length = 30)
    private MatchResultType resultType;

    @Column(name = "winning_margin")
    private Integer winningMargin;

    @Column(name = "result_summary", columnDefinition = "TEXT")
    private String resultSummary;

    @Version
    @Column(nullable = false)
    private Long version;

    protected CricketMatch() {}

    public CricketMatch(
            TournamentEdition edition,
            TournamentTeam teamA,
            TournamentTeam teamB,
            MatchStage stage,
            Integer roundNumber,
            Integer matchNumber,
            Integer oversPerInnings,
            Venue venue
    ) {
        this.tournamentEdition = edition;
        this.teamA = teamA;
        this.teamB = teamB;
        this.stage = stage;
        this.roundNumber = roundNumber;
        this.matchNumber = matchNumber;
        this.oversPerInnings = oversPerInnings;
        this.venue = venue;
        this.status = MatchStatus.PLANNED;
    }

    public void schedule(
            Instant scheduledAt,
            Venue venue
    ) {
        this.scheduledAt = scheduledAt;
        this.venue = venue;
        this.status = MatchStatus.SCHEDULED;
    }

    public void markReady() {
        if (status == MatchStatus.PLANNED
                || status == MatchStatus.SCHEDULED) {
            status = MatchStatus.READY;
        }
    }

    public void markTossCompleted() {
        if (status != MatchStatus.READY) {
            throw new IllegalStateException(
                    "Match must be ready before toss"
            );
        }

        status = MatchStatus.TOSS_COMPLETED;
    }

    public void startMatch() {

        if (status != MatchStatus.TOSS_COMPLETED
                && status != MatchStatus.INNINGS_BREAK) {

            throw new IllegalStateException(
                    "Match cannot enter live state"
            );
        }

        status = MatchStatus.LIVE;

        if (actualStartedAt == null) {
            actualStartedAt = Instant.now();
        }
    }

    public void markInningsBreak() {
        status = MatchStatus.INNINGS_BREAK;
    }

    public void complete(
            TournamentTeam winner,
            MatchResultType resultType,
            Integer margin,
            String summary
    ) {
        this.winnerTeam = winner;
        this.resultType = resultType;
        this.winningMargin = margin;
        this.resultSummary = summary;

        this.status = MatchStatus.COMPLETED;
        this.actualEndedAt = Instant.now();
    }

    public void setQualificationSeeds(
            Integer teamASeed,
            Integer teamBSeed
    ) {
        this.teamASeed = teamASeed;
        this.teamBSeed = teamBSeed;
    }

    public void setSourceMatches(
            CricketMatch sourceMatchA,
            CricketMatch sourceMatchB
    ) {
        this.sourceMatchA = sourceMatchA;
        this.sourceMatchB = sourceMatchB;
    }

    public void resolveKnockoutWinner(
            TournamentTeam winner,
            MatchResultType resolutionType,
            String summary
    ) {

        if (stage == MatchStage.LEAGUE) {
            throw new IllegalStateException(
                    "League matches cannot use knockout resolution"
            );
        }

        boolean participant =
                teamA.getId().equals(winner.getId())
                || teamB.getId().equals(winner.getId());

        if (!participant) {
            throw new IllegalArgumentException(
                    "Winner does not participate in this match"
            );
        }

        if (resolutionType != MatchResultType.TIEBREAKER
                && resolutionType != MatchResultType.FORFEIT) {

            throw new IllegalArgumentException(
                    "Invalid knockout resolution type"
            );
        }

        this.winnerTeam = winner;
        this.resultType = resolutionType;

        this.winningMargin = null;
        this.resultSummary = summary;

        this.status = MatchStatus.COMPLETED;

        if (actualEndedAt == null) {
            actualEndedAt = Instant.now();
        }
    }

    public TournamentEdition getTournamentEdition() {
        return tournamentEdition;
    }

    public TournamentTeam getTeamA() {
        return teamA;
    }

    public TournamentTeam getTeamB() {
        return teamB;
    }

    public Venue getVenue() {
        return venue;
    }

    public Integer getTeamASeed() {
        return teamASeed;
    }

    public Integer getTeamBSeed() {
        return teamBSeed;
    }

    public CricketMatch getSourceMatchA() {
        return sourceMatchA;
    }

    public CricketMatch getSourceMatchB() {
        return sourceMatchB;
    }

    public MatchStage getStage() {
        return stage;
    }

    public Integer getRoundNumber() {
        return roundNumber;
    }

    public Integer getMatchNumber() {
        return matchNumber;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public Integer getOversPerInnings() {
        return oversPerInnings;
    }

    public MatchStatus getStatus() {
        return status;
    }

    public TournamentTeam getWinnerTeam() {
        return winnerTeam;
    }

    public MatchResultType getResultType() {
        return resultType;
    }

    public String getResultSummary() {
        return resultSummary;
    }

    public void markNoResult(
            String summary
    ) {

        if (status == MatchStatus.COMPLETED
                || status == MatchStatus.CANCELLED) {

            throw new IllegalStateException(
                    "Match can no longer be marked as no result"
            );
        }

        winnerTeam = null;

        resultType =
                MatchResultType.NO_RESULT;

        winningMargin = null;

        resultSummary =
                summary;

        status =
                MatchStatus.ABANDONED;

        actualEndedAt =
                Instant.now();
    }
}
