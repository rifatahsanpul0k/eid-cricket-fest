package com.eidcricketfest.tournament.repository;

import com.eidcricketfest.tournament.entity.TournamentEdition;
import com.eidcricketfest.tournament.entity.TournamentEditionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TournamentEditionRepository
        extends JpaRepository<TournamentEdition, Long> {

    boolean existsByTournament_IdAndNameIgnoreCase(
            Long tournamentId,
            String name
    );

    boolean existsByTournament_IdAndNameIgnoreCaseAndIdNot(
            Long tournamentId,
            String name,
            Long id
    );

    List<TournamentEdition> findByTournament_IdOrderByCreatedAtDesc(
            Long tournamentId
    );

    List<TournamentEdition>
    findByTournament_IdAndStatusOrderByEndDateDesc(
            Long tournamentId,
            TournamentEditionStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT e
        FROM TournamentEdition e
        WHERE e.id = :id
    """)
    Optional<TournamentEdition> findByIdForUpdate(
            @Param("id") Long id
    );
}
