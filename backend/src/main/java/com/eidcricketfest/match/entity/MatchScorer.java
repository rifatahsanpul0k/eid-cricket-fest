package com.eidcricketfest.match.entity;

import com.eidcricketfest.auth.entity.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "match_scorers")
@IdClass(MatchScorerId.class)
public class MatchScorer {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id")
    private CricketMatch match;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "primary_scorer", nullable = false)
    private boolean primaryScorer;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by_user_id")
    private User assignedBy;

    protected MatchScorer() {}

    public MatchScorer(
            CricketMatch match,
            User user,
            boolean primaryScorer,
            User assignedBy
    ) {
        this.match = match;
        this.user = user;
        this.primaryScorer = primaryScorer;
        this.assignedBy = assignedBy;
        this.assignedAt = Instant.now();
    }

    public CricketMatch getMatch() {
        return match;
    }

    public User getUser() {
        return user;
    }

    public boolean isPrimaryScorer() {
        return primaryScorer;
    }
}
