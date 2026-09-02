package com.eidcricketfest.match.entity;

import com.eidcricketfest.player.entity.Player;
import com.eidcricketfest.registration.entity.PlayerRegistration;
import com.eidcricketfest.team.entity.TournamentTeam;
import com.eidcricketfest.tournament.entity.TournamentEdition;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "playing_xi_entries")
public class PlayingXiEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id")
    private CricketMatch match;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_edition_id")
    private TournamentEdition edition;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tournament_team_id")
    private TournamentTeam tournamentTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_side_id")
    private MatchSide matchSide;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_registration_id")
    private PlayerRegistration registration;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id")
    private Player player;

    @Column(name = "is_captain", nullable = false)
    private boolean captain;

    @Column(name = "is_wicketkeeper", nullable = false)
    private boolean wicketkeeper;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PlayingXiEntry() {}

    public PlayingXiEntry(
            CricketMatch match,
            TournamentTeam team,
            PlayerRegistration registration,
            boolean captain,
            boolean wicketkeeper
    ) {
        this.match = match;
        this.edition = match.getTournamentEdition();
        this.tournamentTeam = team;
        this.matchSide = match.sideForTournamentTeam(team);
        this.registration = registration;
        this.player = registration.getPlayer();
        this.captain = captain;
        this.wicketkeeper = wicketkeeper;
        this.createdAt = Instant.now();
    }

    public PlayingXiEntry(
            CricketMatch match,
            MatchSide side,
            Player player,
            boolean captain,
            boolean wicketkeeper
    ) {
        this.match = match;
        this.edition = match.getTournamentEdition();
        this.tournamentTeam = side.getTournamentTeam();
        this.matchSide = side;
        this.player = player;
        this.captain = captain;
        this.wicketkeeper = wicketkeeper;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public CricketMatch getMatch() {
        return match;
    }

    public TournamentTeam getTournamentTeam() {
        return tournamentTeam;
    }

    public MatchSide getMatchSide() {
        return matchSide;
    }

    public PlayerRegistration getRegistration() {
        return registration;
    }

    public Player getPlayer() {
        return player;
    }

    public boolean isCaptain() {
        return captain;
    }

    public boolean isWicketkeeper() {
        return wicketkeeper;
    }
}
