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

    @Query("""
        SELECT e
        FROM TournamentEdition e
        LEFT JOIN FETCH e.championTeam champion
        LEFT JOIN FETCH champion.team
        LEFT JOIN FETCH e.runnerUpTeam runnerUp
        LEFT JOIN FETCH runnerUp.team
        LEFT JOIN FETCH e.finalMatch
        WHERE e.tournament.id = :tournamentId
        ORDER BY e.createdAt DESC
    """)
    List<TournamentEdition> findDetailedByTournamentIdOrderByCreatedAtDesc(
            @Param("tournamentId") Long tournamentId
    );

    @Query("""
        SELECT e
        FROM TournamentEdition e
        LEFT JOIN FETCH e.championTeam champion
        LEFT JOIN FETCH champion.team
        LEFT JOIN FETCH e.runnerUpTeam runnerUp
        LEFT JOIN FETCH runnerUp.team
        LEFT JOIN FETCH e.finalMatch
        WHERE e.tournament.id = :tournamentId
          AND e.status = :status
        ORDER BY e.endDate DESC
    """)
    List<TournamentEdition> findDetailedByTournamentIdAndStatusOrderByEndDateDesc(
            @Param("tournamentId") Long tournamentId,
            @Param("status") TournamentEditionStatus status
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
