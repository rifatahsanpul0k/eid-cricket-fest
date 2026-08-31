package com.eidcricketfest.draft.repository;

import com.eidcricketfest.draft.entity.DraftPick;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DraftPickRepository
        extends JpaRepository<DraftPick, Long> {

    long countByDraft_Id(Long draftId);

    boolean existsByDraft_IdAndPlayerRegistration_Id(
            Long draftId,
            Long registrationId
    );

    @Query("""
        SELECT p
        FROM DraftPick p
        JOIN FETCH p.tournamentTeam tt
        JOIN FETCH tt.team
        JOIN FETCH p.playerRegistration pr
        JOIN FETCH pr.player
        WHERE p.draft.id = :draftId
        ORDER BY p.pickNumber
    """)
    List<DraftPick> findDetailedByDraftId(
            @Param("draftId") Long draftId
    );
}
