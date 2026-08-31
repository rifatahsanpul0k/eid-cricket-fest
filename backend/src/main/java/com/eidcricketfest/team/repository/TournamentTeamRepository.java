package com.eidcricketfest.team.repository;

import com.eidcricketfest.team.entity.TournamentTeam;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TournamentTeamRepository
        extends JpaRepository<TournamentTeam, Long> {

    boolean existsByTournamentEdition_IdAndTeam_Id(
            Long editionId,
            Long teamId
    );

    @Query("""
        SELECT tt
        FROM TournamentTeam tt
        JOIN FETCH tt.team
        LEFT JOIN FETCH tt.captainRegistration cr
        LEFT JOIN FETCH cr.player
        WHERE tt.tournamentEdition.id = :editionId
        ORDER BY tt.team.name
    """)
    List<TournamentTeam> findDetailedByEditionId(
            @Param("editionId") Long editionId
    );

    @Query("""
        SELECT tt
        FROM TournamentTeam tt
        JOIN FETCH tt.team
        JOIN FETCH tt.tournamentEdition
        LEFT JOIN FETCH tt.captainRegistration cr
        LEFT JOIN FETCH cr.player
        WHERE tt.id = :id
    """)
    Optional<TournamentTeam> findDetailedById(
            @Param("id") Long id
    );
}
