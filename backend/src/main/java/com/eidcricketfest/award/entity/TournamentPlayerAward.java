package com.eidcricketfest.award.entity;

import com.eidcricketfest.auth.entity.User;
import com.eidcricketfest.common.entity.BaseEntity;
import com.eidcricketfest.registration.entity.PlayerRegistration;
import com.eidcricketfest.tournament.entity.TournamentEdition;
import jakarta.persistence.*;

@Entity
@Table(name = "tournament_player_awards")
public class TournamentPlayerAward extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tournament_edition_id")
    private TournamentEdition tournamentEdition;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_registration_id")
    private PlayerRegistration playerRegistration;

    @Enumerated(EnumType.STRING)
    @Column(name = "award_type", nullable = false, length = 40)
    private AwardType awardType;

    @Column(length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "awarded_by_user_id")
    private User awardedBy;

    protected TournamentPlayerAward() {
    }

    public TournamentPlayerAward(
            TournamentEdition tournamentEdition,
            PlayerRegistration playerRegistration,
            AwardType awardType,
            String title,
            String notes,
            User awardedBy
    ) {
        this.tournamentEdition = tournamentEdition;
        this.playerRegistration = playerRegistration;
        this.awardType = awardType;
        this.title = title;
        this.notes = notes;
        this.awardedBy = awardedBy;
    }

    public TournamentEdition getTournamentEdition() {
        return tournamentEdition;
    }

    public PlayerRegistration getPlayerRegistration() {
        return playerRegistration;
    }

    public AwardType getAwardType() {
        return awardType;
    }

    public String getTitle() {
        return title;
    }

    public String getNotes() {
        return notes;
    }
}
