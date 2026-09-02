package com.eidcricketfest.scoring.repository;

import com.eidcricketfest.scoring.entity.Innings;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InningsRepository
        extends JpaRepository<Innings, Long> {

    boolean existsByMatch_IdAndInningsNumber(
            Long matchId,
            Short inningsNumber
    );

    boolean existsByMatch_Id(Long matchId);

    Optional<Innings>
    findByMatch_IdAndInningsNumber(
            Long matchId,
            Short inningsNumber
    );

    List<Innings>
    findByMatch_IdOrderByInningsNumber(
            Long matchId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT i
        FROM Innings i
        WHERE i.id = :id
    """)
    Optional<Innings> findByIdForUpdate(
            @Param("id") Long id
    );
}
