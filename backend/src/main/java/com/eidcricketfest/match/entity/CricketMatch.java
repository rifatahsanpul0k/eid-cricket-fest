package com.eidcricketfest.match.entity;

import com.eidcricketfest.common.entity.BaseEntity;
import com.eidcricketfest.team.entity.TournamentTeam;
import com.eidcricketfest.tournament.entity.TournamentEdition;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "matches")
public class CricketMatch extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "match_type", nullable = false, length = 30)
    private MatchType matchType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_edition_id")
    private TournamentEdition tournamentEdition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_a_id")
    private TournamentTeam teamA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_b_id")
    private TournamentTeam teamB;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_a_side_id")
    private MatchSide teamASide;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_b_side_id")
    private MatchSide teamBSide;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_side_id")
    private MatchSide winnerSide;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_type", length = 30)
    private MatchResultType resultType;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_status", length = 30)
    private MatchResultStatus resultStatus;

    @Column(name = "winning_margin")
    private Integer winningMargin;

    @Column(name = "result_summary", columnDefinition = "TEXT")
    private String resultSummary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rematch_of_match_id")
    private CricketMatch rematchOfMatch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "superseded_by_match_id")
    private CricketMatch supersededByMatch;

    @Enumerated(EnumType.STRING)
    @Column(name = "suspended_from_status", length = 30)
    private MatchStatus suspendedFromStatus;

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
        this.matchType = MatchType.TOURNAMENT;
        this.teamA = teamA;
        this.teamB = teamB;
        this.stage = stage;
        this.roundNumber = roundNumber;
        this.matchNumber = matchNumber;
        this.oversPerInnings = oversPerInnings;
        this.venue = venue;
        this.status = MatchStatus.PLANNED;
    }

    public static CricketMatch friendly(
            Integer oversPerInnings,
            Venue venue,
            Instant scheduledAt
    ) {
        CricketMatch match = new CricketMatch();
        match.matchType = MatchType.FRIENDLY;
        match.oversPerInnings = oversPerInnings;
        match.venue = venue;
        match.scheduledAt = scheduledAt;
        match.status = scheduledAt == null
                ? MatchStatus.PLANNED
                : MatchStatus.SCHEDULED;
        return match;
    }

    public void attachSides(
            MatchSide teamASide,
            MatchSide teamBSide
    ) {
        this.teamASide = teamASide;
        this.teamBSide = teamBSide;
    }

    public void schedule(
            Instant scheduledAt,
            Venue venue
    ) {
        this.scheduledAt = scheduledAt;
        this.venue = venue;
        this.status = MatchStatus.SCHEDULED;
    }

    public void reschedule(
            Instant scheduledAt,
            Venue venue,
            Integer oversPerInnings
    ) {
        if (status == MatchStatus.COMPLETED
                || status == MatchStatus.ABANDONED
                || status == MatchStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Finished matches cannot be rescheduled"
            );
        }

        this.scheduledAt = scheduledAt;
        this.venue = venue;

        if (oversPerInnings != null) {
            this.oversPerInnings = oversPerInnings;
        }

        if (status == MatchStatus.PLANNED
                || status == MatchStatus.POSTPONED) {
            status = MatchStatus.SCHEDULED;
        }
    }

    public void postpone() {
        if (status == MatchStatus.COMPLETED
                || status == MatchStatus.ABANDONED
                || status == MatchStatus.CANCELLED
                || status == MatchStatus.LIVE
                || status == MatchStatus.INNINGS_BREAK
                || status == MatchStatus.SUSPENDED) {
            throw new IllegalStateException(
                    "Match cannot be postponed in its current state"
            );
        }

        status = MatchStatus.POSTPONED;
    }

    public void suspend() {
        if (status != MatchStatus.TOSS_COMPLETED
                && status != MatchStatus.LIVE
                && status != MatchStatus.INNINGS_BREAK) {
            throw new IllegalStateException(
                    "Only started matches can be suspended"
            );
        }

        suspendedFromStatus = status;
        status = MatchStatus.SUSPENDED;
    }

    public void resume() {
        if (status != MatchStatus.SUSPENDED) {
            throw new IllegalStateException(
                    "Only suspended matches can be resumed"
            );
        }

        status = suspendedFromStatus != null
                ? suspendedFromStatus
                : MatchStatus.LIVE;
        suspendedFromStatus = null;
    }

    public void cancel() {
        if (status == MatchStatus.LIVE
                || status == MatchStatus.INNINGS_BREAK
                || status == MatchStatus.SUSPENDED
                || status == MatchStatus.COMPLETED
                || status == MatchStatus.ABANDONED
                || status == MatchStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Match cannot be cancelled after meaningful play starts"
            );
        }

        clearResult();
        status = MatchStatus.CANCELLED;
        actualEndedAt = Instant.now();
    }

    public void abandon(String summary) {
        if (status != MatchStatus.TOSS_COMPLETED
                && status != MatchStatus.LIVE
                && status != MatchStatus.INNINGS_BREAK
                && status != MatchStatus.SUSPENDED) {
            throw new IllegalStateException(
                    "Only started matches can be abandoned"
            );
        }

        clearResult();
        resultType = MatchResultType.NO_RESULT;
        resultStatus = MatchResultStatus.OFFICIAL;
        resultSummary = summary;
        status = MatchStatus.ABANDONED;
        suspendedFromStatus = null;
        actualEndedAt = Instant.now();
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
            MatchSide winner,
            MatchResultType resultType,
            Integer margin,
            String summary
    ) {
        this.winnerSide = winner;
        this.winnerTeam = winner != null
                ? winner.getTournamentTeam()
                : null;
        this.resultType = resultType;
        this.resultStatus = MatchResultStatus.OFFICIAL;
        this.winningMargin = margin;
        this.resultSummary = summary;

        this.status = MatchStatus.COMPLETED;
        this.actualEndedAt = Instant.now();
    }

    public void complete(
            TournamentTeam winner,
            MatchResultType resultType,
            Integer margin,
            String summary
    ) {
        complete(
                sideForTournamentTeam(winner),
                resultType,
                margin,
                summary
        );
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
        this.winnerSide = sideForTournamentTeam(winner);
        this.resultType = resolutionType;
        this.resultStatus = MatchResultStatus.OFFICIAL;

        this.winningMargin = null;
        this.resultSummary = summary;

        this.status = MatchStatus.COMPLETED;

        if (actualEndedAt == null) {
            actualEndedAt = Instant.now();
        }
    }

    public void resetToss() {
        if (status != MatchStatus.TOSS_COMPLETED) {
            throw new IllegalStateException(
                    "Toss can only be reset before innings starts"
            );
        }

        status = MatchStatus.READY;
    }

    public void markResultUnderReview() {
        requireCompletedOfficialResult();
        resultStatus = MatchResultStatus.UNDER_REVIEW;
    }

    public void restoreOfficialResult() {
        if (status != MatchStatus.COMPLETED
                || resultStatus != MatchResultStatus.UNDER_REVIEW) {
            throw new IllegalStateException(
                    "Only results under review can be restored"
            );
        }

        resultStatus = MatchResultStatus.OFFICIAL;
    }

    public void voidResult(String summary) {
        if (status != MatchStatus.COMPLETED
                || resultStatus == MatchResultStatus.VOID
                || resultStatus == MatchResultStatus.SUPERSEDED) {
            throw new IllegalStateException(
                    "Only active completed results can be voided"
            );
        }

        clearWinner();
        resultType = MatchResultType.NO_RESULT;
        resultStatus = MatchResultStatus.VOID;
        winningMargin = null;
        resultSummary = summary;
    }

    public void markSupersededBy(CricketMatch rematch) {
        if (status != MatchStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Only completed matches can be superseded"
            );
        }

        this.resultStatus = MatchResultStatus.SUPERSEDED;
        this.supersededByMatch = rematch;
    }

    public void markRematchOf(CricketMatch original) {
        this.rematchOfMatch = original;
    }

    private void requireCompletedOfficialResult() {
        if (status != MatchStatus.COMPLETED
                || resultStatus != MatchResultStatus.OFFICIAL) {
            throw new IllegalStateException(
                    "Only official completed results can be reviewed"
            );
        }
    }

    private void clearResult() {
        clearWinner();
        resultType = null;
        resultStatus = null;
        winningMargin = null;
        resultSummary = null;
    }

    private void clearWinner() {
        winnerTeam = null;
        winnerSide = null;
    }

    public TournamentEdition getTournamentEdition() {
        return tournamentEdition;
    }

    public TournamentEdition requireTournamentEdition() {
        if (!isTournament() || tournamentEdition == null) {
            throw new IllegalStateException(
                    "Match is not a tournament match"
            );
        }
        return tournamentEdition;
    }

    public boolean isTournament() {
        return matchType == MatchType.TOURNAMENT;
    }

    public boolean isFriendly() {
        return matchType == MatchType.FRIENDLY;
    }

    public MatchType getMatchType() {
        return matchType;
    }

    public TournamentTeam getTeamA() {
        return teamA;
    }

    public TournamentTeam getTeamB() {
        return teamB;
    }

    public MatchSide getTeamASide() {
        return teamASide;
    }

    public MatchSide getTeamBSide() {
        return teamBSide;
    }

    public MatchSide sideA() {
        return teamASide;
    }

    public MatchSide sideB() {
        return teamBSide;
    }

    public MatchSide sideForTournamentTeam(
            TournamentTeam team
    ) {
        if (team == null) {
            return null;
        }

        if (teamA != null
                && team.getId().equals(teamA.getId())) {
            return teamASide;
        }

        if (teamB != null
                && team.getId().equals(teamB.getId())) {
            return teamBSide;
        }

        return null;
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

    public MatchSide getWinnerSide() {
        return winnerSide;
    }

    public MatchResultType getResultType() {
        return resultType;
    }

    public MatchResultStatus getResultStatus() {
        return resultStatus;
    }

    public String getResultSummary() {
        return resultSummary;
    }

    public CricketMatch getRematchOfMatch() {
        return rematchOfMatch;
    }

    public CricketMatch getSupersededByMatch() {
        return supersededByMatch;
    }

    public MatchStatus getSuspendedFromStatus() {
        return suspendedFromStatus;
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

        resultStatus =
                MatchResultStatus.OFFICIAL;

        winningMargin = null;

        resultSummary =
                summary;

        status =
                MatchStatus.ABANDONED;

        actualEndedAt =
                Instant.now();
    }
}
