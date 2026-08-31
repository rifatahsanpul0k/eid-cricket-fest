package com.eidcricketfest.draft.entity;

import com.eidcricketfest.team.entity.TournamentTeam;
import com.eidcricketfest.tournament.entity.TournamentEdition;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "draft_orders")
public class DraftOrder {

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

    @Column(nullable = false)
    private Integer position;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DraftOrder() {}

    public DraftOrder(
            Draft draft,
            TournamentEdition edition,
            TournamentTeam team,
            Integer position
    ) {
        this.draft = draft;
        this.tournamentEdition = edition;
        this.tournamentTeam = team;
        this.position = position;
        this.createdAt = Instant.now();
    }

    public TournamentTeam getTournamentTeam() {
        return tournamentTeam;
    }

    public Integer getPosition() {
        return position;
    }
}
