package com.eidcricketfest.registration.repository;

import com.eidcricketfest.registration.entity.PlayerRegistration;
import com.eidcricketfest.registration.entity.RegistrationStatus;
import com.eidcricketfest.team.entity.RosterEntryStatus;
import com.eidcricketfest.team.entity.TeamRosterEntry;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlayerRegistrationRepository
                extends JpaRepository<PlayerRegistration, Long>,
                JpaSpecificationExecutor<PlayerRegistration> {

        boolean existsByTournamentEdition_IdAndPlayer_Id(
                        Long tournamentEditionId,
                        Long playerId

        );

        @Query("""
                            SELECT pr
                            FROM PlayerRegistration pr
                            JOIN FETCH pr.player p
                            LEFT JOIN FETCH p.user
                            JOIN FETCH pr.tournamentEdition
                            JOIN FETCH pr.category
                            WHERE pr.id = :id
                        """)
        Optional<PlayerRegistration> findDetailedById(
                        @Param("id") Long id);

        @Query("""
                            SELECT pr
                            FROM PlayerRegistration pr
                            JOIN FETCH pr.player p
                            LEFT JOIN FETCH p.user
                            JOIN FETCH pr.tournamentEdition
                            JOIN FETCH pr.category
                            WHERE pr.tournamentEdition.id = :editionId
                              AND p.user.id = :userId
                        """)
        Optional<PlayerRegistration> findMineByEditionIdAndUserId(
                        @Param("editionId") Long editionId,
                        @Param("userId") Long userId);

        @Query("""
                            SELECT pr
                            FROM PlayerRegistration pr
                            JOIN FETCH pr.player p
                            JOIN FETCH pr.category c
                            WHERE pr.tournamentEdition.id = :editionId
                              AND pr.status = :registrationStatus
                              AND NOT EXISTS (
                                  SELECT tre.id
                                  FROM TeamRosterEntry tre
                                  WHERE tre.playerRegistration.id = pr.id
                                    AND tre.status = :rosterStatus
                              )
                            ORDER BY p.fullName
                        """)
        List<PlayerRegistration> findDraftPool(
                        @Param("editionId") Long editionId,
                        @Param("registrationStatus") RegistrationStatus registrationStatus,
                        @Param("rosterStatus") RosterEntryStatus rosterStatus);
}
