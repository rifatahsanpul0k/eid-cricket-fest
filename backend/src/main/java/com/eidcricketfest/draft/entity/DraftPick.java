package com.eidcricketfest.draft.entity;

import com.eidcricketfest.auth.entity.User;
import com.eidcricketfest.registration.entity.PlayerRegistration;
import com.eidcricketfest.team.entity.TournamentTeam;
import com.eidcricketfest.tournament.entity.TournamentEdition;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "draft_picks")
public class DraftPick {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "draft_id")
    private Draft draft;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tournament_edition_id")
    private TournamentEdition tournamentEdition;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tournament_team_id")
    private TournamentTeam tournamentTeam;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_registration_id")
    private PlayerRegistration playerRegistration;

    @Column(name = "round_number", nullable = false)
    private Integer roundNumber;

    @Column(name = "pick_number", nullable = false)
    private Integer pickNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_by_user_id")
    private User selectedBy;

    @Column(name = "selected_at", nullable = false)
    private Instant selectedAt;

    protected DraftPick() {}

    public DraftPick(
            Draft draft,
            TournamentEdition edition,
            TournamentTeam team,
            PlayerRegistration registration,
            Integer roundNumber,
            Integer pickNumber,
            User selectedBy
    ) {
        this.draft = draft;
        this.tournamentEdition = edition;
        this.tournamentTeam = team;
        this.playerRegistration = registration;
        this.roundNumber = roundNumber;
        this.pickNumber = pickNumber;
        this.selectedBy = selectedBy;
        this.selectedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public TournamentTeam getTournamentTeam() {
        return tournamentTeam;
    }

    public PlayerRegistration getPlayerRegistration() {
        return playerRegistration;
    }

    public Integer getRoundNumber() {
        return roundNumber;
    }

    public Integer getPickNumber() {
        return pickNumber;
    }

    public Instant getSelectedAt() {
        return selectedAt;
    }
}
