package com.eidcricketfest.award.repository;

import com.eidcricketfest.award.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TournamentPlayerAwardRepository
        extends JpaRepository<TournamentPlayerAward, Long> {

    boolean existsByTournamentEdition_IdAndAwardTypeAndPlayerRegistration_Id(
            Long editionId,
            AwardType awardType,
            Long registrationId
    );

    @Query("""
        SELECT a
        FROM TournamentPlayerAward a
        JOIN FETCH a.playerRegistration pr
        JOIN FETCH pr.player p
        WHERE a.tournamentEdition.id = :editionId
        ORDER BY a.id
    """)
    List<TournamentPlayerAward> findDetailedByEditionId(
            @Param("editionId") Long editionId
    );
}
