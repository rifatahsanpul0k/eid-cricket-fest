package com.eidcricketfest.match.entity;

import com.eidcricketfest.common.entity.BaseEntity;
import com.eidcricketfest.team.entity.TournamentTeam;
import jakarta.persistence.*;

@Entity
@Table(name = "match_sides")
public class MatchSide extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id")
    private CricketMatch match;

    @Enumerated(EnumType.STRING)
    @Column(name = "side_key", nullable = false, length = 1)
    private MatchSideKey sideKey;

    @Column(name = "display_name", nullable = false, length = 150)
    private String displayName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_team_id")
    private TournamentTeam tournamentTeam;

    protected MatchSide() {}

    public MatchSide(
            CricketMatch match,
            MatchSideKey sideKey,
            String displayName,
            TournamentTeam tournamentTeam
    ) {
        this.match = match;
        this.sideKey = sideKey;
        this.displayName = displayName;
        this.tournamentTeam = tournamentTeam;
    }

    public CricketMatch getMatch() {
        return match;
    }

    public MatchSideKey getSideKey() {
        return sideKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void rename(String displayName) {
        this.displayName = displayName;
    }

    public TournamentTeam getTournamentTeam() {
        return tournamentTeam;
    }
}
