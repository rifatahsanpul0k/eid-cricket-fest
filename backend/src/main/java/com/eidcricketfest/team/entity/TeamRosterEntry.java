package com.eidcricketfest.team.entity;

import com.eidcricketfest.common.entity.BaseEntity;
import com.eidcricketfest.registration.entity.PlayerRegistration;
import com.eidcricketfest.tournament.entity.TournamentEdition;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "team_roster_entries")
public class TeamRosterEntry extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tournament_edition_id")
    private TournamentEdition tournamentEdition;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tournament_team_id")
    private TournamentTeam tournamentTeam;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_registration_id")
    private PlayerRegistration playerRegistration;

    @Enumerated(EnumType.STRING)
    @Column(name = "acquisition_type", nullable = false)
    private AcquisitionType acquisitionType;

    @Column(name = "jersey_number", length = 10)
    private String jerseyNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RosterEntryStatus status;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Column(name = "removed_at")
    private Instant removedAt;

    protected TeamRosterEntry() {}

    public TeamRosterEntry(
            TournamentEdition edition,
            TournamentTeam team,
            PlayerRegistration registration,
            AcquisitionType acquisitionType
    ) {
        this.tournamentEdition = edition;
        this.tournamentTeam = team;
        this.playerRegistration = registration;
        this.acquisitionType = acquisitionType;
        this.status = RosterEntryStatus.ACTIVE;
        this.joinedAt = Instant.now();
    }

    public TournamentTeam getTournamentTeam() {
        return tournamentTeam;
    }
}
