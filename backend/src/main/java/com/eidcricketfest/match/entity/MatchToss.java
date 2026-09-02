package com.eidcricketfest.match.entity;

import com.eidcricketfest.auth.entity.User;
import com.eidcricketfest.team.entity.TournamentTeam;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "match_tosses")
public class MatchToss {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id")
    private CricketMatch match;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_edition_id")
    private com.eidcricketfest.tournament.entity.TournamentEdition edition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_team_id")
    private TournamentTeam winnerTeam;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "winner_match_side_id")
    private MatchSide winnerSide;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TossDecision decision;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by_user_id")
    private User recordedBy;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected MatchToss() {}

    public MatchToss(
            CricketMatch match,
            TournamentTeam winnerTeam,
            TossDecision decision,
            User recordedBy
    ) {
        this.match = match;
        this.edition = match.getTournamentEdition();
        this.winnerTeam = winnerTeam;
        this.winnerSide = match.sideForTournamentTeam(winnerTeam);
        this.decision = decision;
        this.recordedBy = recordedBy;
        this.recordedAt = Instant.now();
    }

    public MatchToss(
            CricketMatch match,
            MatchSide winnerSide,
            TossDecision decision,
            User recordedBy
    ) {
        this.match = match;
        this.edition = match.getTournamentEdition();
        this.winnerSide = winnerSide;
        this.winnerTeam = winnerSide.getTournamentTeam();
        this.decision = decision;
        this.recordedBy = recordedBy;
        this.recordedAt = Instant.now();
    }

    public TournamentTeam getWinnerTeam() {
        return winnerTeam;
    }

    public MatchSide getWinnerSide() {
        return winnerSide;
    }

    public TossDecision getDecision() {
        return decision;
    }
}
