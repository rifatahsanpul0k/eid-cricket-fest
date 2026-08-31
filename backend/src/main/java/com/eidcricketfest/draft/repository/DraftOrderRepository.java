package com.eidcricketfest.draft.repository;

import com.eidcricketfest.draft.entity.DraftOrder;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DraftOrderRepository
        extends JpaRepository<DraftOrder, Long> {

    @Query("""
        SELECT o
        FROM DraftOrder o
        JOIN FETCH o.tournamentTeam tt
        JOIN FETCH tt.team
        WHERE o.draft.id = :draftId
        ORDER BY o.position
    """)
    List<DraftOrder> findDetailedByDraftId(
            @Param("draftId") Long draftId
    );
}
