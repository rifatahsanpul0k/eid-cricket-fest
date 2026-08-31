package com.eidcricketfest.auth.repository;

import com.eidcricketfest.auth.entity.RefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository
        extends JpaRepository<RefreshToken, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT rt
        FROM RefreshToken rt
        JOIN FETCH rt.user u
        LEFT JOIN FETCH u.roles
        WHERE rt.tokenHash = :tokenHash
    """)
    Optional<RefreshToken> findForUpdateByTokenHash(
            @Param("tokenHash") String tokenHash
    );

    @Modifying(
            flushAutomatically = true,
            clearAutomatically = true
    )
    @Query("""
        UPDATE RefreshToken rt
        SET rt.revokedAt = :revokedAt
        WHERE rt.familyId = :familyId
          AND rt.revokedAt IS NULL
    """)
    int revokeFamily(
            @Param("familyId") UUID familyId,
            @Param("revokedAt") Instant revokedAt
    );
}