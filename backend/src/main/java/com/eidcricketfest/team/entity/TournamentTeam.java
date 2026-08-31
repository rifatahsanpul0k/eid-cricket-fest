package com.eidcricketfest.team.entity;

import com.eidcricketfest.common.entity.BaseEntity;
import com.eidcricketfest.registration.entity.PlayerRegistration;
import com.eidcricketfest.tournament.entity.TournamentEdition;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "tournament_teams")
public class TournamentTeam extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tournament_edition_id", nullable = false)
    private TournamentEdition tournamentEdition;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "captain_registration_id")
    private PlayerRegistration captainRegistration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vice_captain_registration_id")
    private PlayerRegistration viceCaptainRegistration;

    @Enumerated(EnumType.STRING)
    @Column(name = "roster_status", nullable = false)
    private RosterStatus rosterStatus;

    @Column(name = "roster_locked_at")
    private Instant rosterLockedAt;

    protected TournamentTeam() {}

    public TournamentTeam(
            TournamentEdition tournamentEdition,
            Team team
    ) {
        this.tournamentEdition = tournamentEdition;
        this.team = team;
        this.rosterStatus = RosterStatus.OPEN;
    }

    public void assignCaptain(PlayerRegistration registration) {
        this.captainRegistration = registration;
    }

    public TournamentEdition getTournamentEdition() {
        return tournamentEdition;
    }

    public Team getTeam() {
        return team;
    }

    public PlayerRegistration getCaptainRegistration() {
        return captainRegistration;
    }

    public RosterStatus getRosterStatus() {
        return rosterStatus;
    }
}
