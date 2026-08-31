package com.eidcricketfest.draft.repository;

import com.eidcricketfest.draft.entity.Draft;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DraftRepository
        extends JpaRepository<Draft, Long> {

    boolean existsByTournamentEdition_Id(Long editionId);

    Optional<Draft> findByTournamentEdition_Id(Long editionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT d
        FROM Draft d
        JOIN FETCH d.tournamentEdition
        WHERE d.id = :id
    """)
    Optional<Draft> findByIdForUpdate(
            @Param("id") Long id
    );
}
