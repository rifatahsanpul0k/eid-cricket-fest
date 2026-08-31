package com.eidcricketfest.match.entity;

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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id")
    private CricketMatch match;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tournament_edition_id")
    private TournamentEdition edition;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tournament_team_id")
    private TournamentTeam tournamentTeam;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_registration_id")
    private PlayerRegistration registration;

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
        this.registration = registration;
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

    public PlayerRegistration getRegistration() {
        return registration;
    }

    public boolean isCaptain() {
        return captain;
    }

    public boolean isWicketkeeper() {
        return wicketkeeper;
    }
}
