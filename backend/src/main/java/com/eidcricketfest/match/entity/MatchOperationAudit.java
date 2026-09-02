package com.eidcricketfest.match.entity;

import com.eidcricketfest.auth.entity.User;
import com.eidcricketfest.common.entity.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "match_operation_audits")
public class MatchOperationAudit extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id")
    private CricketMatch match;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 40)
    private MatchOperationType operationType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_user_id")
    private User actor;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", length = 30)
    private MatchStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 30)
    private MatchStatus newStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_result_status", length = 30)
    private MatchResultStatus oldResultStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_result_status", length = 30)
    private MatchResultStatus newResultStatus;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_match_id")
    private CricketMatch relatedMatch;

    protected MatchOperationAudit() {}

    public MatchOperationAudit(
            CricketMatch match,
            MatchOperationType operationType,
            User actor,
            String reason,
            MatchStatus oldStatus,
            MatchStatus newStatus,
            MatchResultStatus oldResultStatus,
            MatchResultStatus newResultStatus,
            String metadata,
            CricketMatch relatedMatch
    ) {
        this.match = match;
        this.operationType = operationType;
        this.actor = actor;
        this.reason = reason;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.oldResultStatus = oldResultStatus;
        this.newResultStatus = newResultStatus;
        this.metadata = metadata;
        this.relatedMatch = relatedMatch;
    }

    public CricketMatch getMatch() {
        return match;
    }

    public MatchOperationType getOperationType() {
        return operationType;
    }

    public User getActor() {
        return actor;
    }

    public String getReason() {
        return reason;
    }

    public MatchStatus getOldStatus() {
        return oldStatus;
    }

    public MatchStatus getNewStatus() {
        return newStatus;
    }

    public MatchResultStatus getOldResultStatus() {
        return oldResultStatus;
    }

    public MatchResultStatus getNewResultStatus() {
        return newResultStatus;
    }

    public String getMetadata() {
        return metadata;
    }

    public CricketMatch getRelatedMatch() {
        return relatedMatch;
    }
}
