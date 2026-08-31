package com.eidcricketfest.auth.service;

import com.eidcricketfest.auth.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class RefreshTokenRevocationService {

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenRevocationService(
            RefreshTokenRepository refreshTokenRepository
    ) {
        this.refreshTokenRepository =
                refreshTokenRepository;
    }

    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public void revokeFamily(UUID familyId) {

        refreshTokenRepository.revokeFamily(
                familyId,
                Instant.now()
        );
    }
}
