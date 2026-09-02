package com.eidcricketfest.match.repository;

import com.eidcricketfest.match.entity.MatchOperationAudit;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface MatchOperationAuditRepository
        extends JpaRepository<MatchOperationAudit, Long> {

    @Query("""
        SELECT a
        FROM MatchOperationAudit a
        JOIN FETCH a.actor actor
        LEFT JOIN FETCH a.relatedMatch related
        WHERE a.match.id = :matchId
        ORDER BY a.createdAt DESC
    """)
    List<MatchOperationAudit> findDetailedByMatchId(
            @Param("matchId") Long matchId
    );

    @Query("""
        SELECT a
        FROM MatchOperationAudit a
        WHERE a.match.id IN :matchIds
        ORDER BY a.createdAt DESC
    """)
    List<MatchOperationAudit> findByMatchIds(
            @Param("matchIds") Set<Long> matchIds
    );
}
