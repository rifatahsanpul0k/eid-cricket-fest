package com.eidcricketfest.scoring.entity;

import com.eidcricketfest.match.entity.PlayingXiEntry;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "wickets")
public class Wicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_id")
    private Delivery delivery;

    @Column(name = "match_id", nullable = false)
    private Long matchId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dismissed_playing_xi_id")
    private PlayingXiEntry dismissedPlayer;

    @Enumerated(EnumType.STRING)
    @Column(name = "dismissal_type", nullable = false)
    private DismissalType dismissalType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fielder_playing_xi_id")
    private PlayingXiEntry fielder;

    @Column(name = "credited_to_bowler", nullable = false)
    private boolean creditedToBowler;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Wicket() {}

    public Wicket(
            Delivery delivery,
            PlayingXiEntry dismissedPlayer,
            DismissalType dismissalType,
            PlayingXiEntry fielder
    ) {
        this.delivery = delivery;
        this.matchId = dismissedPlayer.getMatch().getId();
        this.dismissedPlayer = dismissedPlayer;
        this.dismissalType = dismissalType;
        this.fielder = fielder;
        this.creditedToBowler =
                dismissalType.isBowlerCredited();
        this.createdAt = Instant.now();
    }

    public DismissalType getDismissalType() {
        return dismissalType;
    }

    public PlayingXiEntry getDismissedPlayer() {
        return dismissedPlayer;
    }

    public PlayingXiEntry getFielder() {
        return fielder;
    }

    public boolean isCreditedToBowler() {
        return creditedToBowler;
    }

    public Delivery getDelivery() {
        return delivery;
    }
}
