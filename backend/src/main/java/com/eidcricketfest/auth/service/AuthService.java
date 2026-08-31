package com.eidcricketfest.auth.service;

import com.eidcricketfest.auth.dto.*;
import com.eidcricketfest.auth.entity.*;
import com.eidcricketfest.auth.repository.*;
import com.eidcricketfest.common.exception.ConflictException;
import com.eidcricketfest.common.exception.UnauthorizedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenRevocationService refreshTokenRevocationService;

    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            RefreshTokenRepository refreshTokenRepository,
            RefreshTokenRevocationService refreshTokenRevocationService,
            PasswordEncoder passwordEncoder,
            TokenService tokenService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository =
                refreshTokenRepository;
        this.refreshTokenRevocationService =
                refreshTokenRevocationService;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    public AuthResponse register(
            RegisterRequest request
    ) {

        String email = normalizeEmail(request.email());
        String phone = normalizePhone(request.phone());

        if (email != null
                && userRepository
                .existsByEmailIgnoreCase(email)) {

            throw new ConflictException(
                    "Email is already registered"
            );
        }

        if (phone != null
                && userRepository
                .existsByPhone(phone)) {

            throw new ConflictException(
                    "Phone number is already registered"
            );
        }

        Role playerRole = roleRepository
                .findByCode(RoleCode.PLAYER)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "PLAYER role is missing"
                        )
                );

        User user = new User(
                request.displayName().trim(),
                email,
                phone,
                passwordEncoder.encode(
                        request.password()
                )
        );

        user.addRole(playerRole);

        user = userRepository.save(user);

        return createNewSession(user);
    }

    public AuthResponse login(
            LoginRequest request
    ) {

        String identifier =
                normalizeIdentifier(
                        request.identifier()
                );

        User user = userRepository
                .findByIdentifier(identifier)
                .orElseThrow(this::invalidCredentials);

        if (!user.isEnabled()) {
            throw invalidCredentials();
        }

        if (!passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        )) {
            throw invalidCredentials();
        }

        return createNewSession(user);
    }

    public AuthResponse refresh(
            RefreshTokenRequest request
    ) {

        String hash = tokenService
                .hashRefreshToken(
                        request.refreshToken()
                );

        RefreshToken existing =
                refreshTokenRepository
                        .findForUpdateByTokenHash(hash)
                        .orElseThrow(() ->
                                new UnauthorizedException(
                                        "Invalid refresh token"
                                )
                        );

        Instant now = Instant.now();

        /*
         * A previously rotated token was used again.
         * Treat it as possible token theft.
         */
        if (existing.isRevoked()) {

            refreshTokenRevocationService
                    .revokeFamily(
                            existing.getFamilyId()
                    );

            throw new UnauthorizedException(
                    "Refresh token reuse detected"
            );
        }

        if (existing.isExpired(now)) {

            existing.revoke(now);

            throw new UnauthorizedException(
                    "Refresh token has expired"
            );
        }

        User user = existing.getUser();

        if (!user.isEnabled()) {

            refreshTokenRepository.revokeFamily(
                    existing.getFamilyId(),
                    now
            );

            throw new UnauthorizedException(
                    "User account is disabled"
            );
        }

        /*
         * Rotate old token.
         */
        existing.revoke(now);

        String rawNewRefreshToken =
                tokenService.generateRefreshToken();

        RefreshToken newRefreshToken =
                new RefreshToken(
                        user,
                        tokenService.hashRefreshToken(
                                rawNewRefreshToken
                        ),
                        existing.getFamilyId(),
                        tokenService
                                .refreshTokenExpiration()
                );

        refreshTokenRepository.save(
                newRefreshToken
        );

        TokenService.AccessToken accessToken =
                tokenService.createAccessToken(user);

        return buildResponse(
                user,
                accessToken,
                rawNewRefreshToken
        );
    }

    public void logout(
            LogoutRequest request
    ) {

        String hash = tokenService
                .hashRefreshToken(
                        request.refreshToken()
                );

        refreshTokenRepository
                .findForUpdateByTokenHash(hash)
                .ifPresent(token ->
                        refreshTokenRepository
                                .revokeFamily(
                                        token.getFamilyId(),
                                        Instant.now()
                                )
                );
    }

    private AuthResponse createNewSession(
            User user
    ) {

        UUID familyId = UUID.randomUUID();

        String rawRefreshToken =
                tokenService.generateRefreshToken();

        RefreshToken refreshToken =
                new RefreshToken(
                        user,
                        tokenService.hashRefreshToken(
                                rawRefreshToken
                        ),
                        familyId,
                        tokenService
                                .refreshTokenExpiration()
                );

        refreshTokenRepository.save(
                refreshToken
        );

        TokenService.AccessToken accessToken =
                tokenService.createAccessToken(user);

        return buildResponse(
                user,
                accessToken,
                rawRefreshToken
        );
    }

    private AuthResponse buildResponse(
            User user,
            TokenService.AccessToken accessToken,
            String refreshToken
    ) {

        Set<RoleCode> roles =
                user.getRoles()
                        .stream()
                        .map(Role::getCode)
                        .collect(Collectors.toSet());

        AuthResponse.UserInfo userInfo =
                new AuthResponse.UserInfo(
                        user.getId(),
                        user.getDisplayName(),
                        user.getEmail(),
                        user.getPhone(),
                        roles
                );

        return new AuthResponse(
                accessToken.value(),
                refreshToken,
                "Bearer",
                accessToken.expiresInSeconds(),
                userInfo
        );
    }

    private UnauthorizedException invalidCredentials() {
        return new UnauthorizedException(
                "Invalid email/phone or password"
        );
    }

    private String normalizeEmail(String email) {

        if (email == null || email.isBlank()) {
            return null;
        }

        return email
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private String normalizePhone(String phone) {

        if (phone == null || phone.isBlank()) {
            return null;
        }

        return phone.trim();
    }

    private String normalizeIdentifier(
            String identifier
    ) {

        String value = identifier.trim();

        if (value.contains("@")) {
            return value.toLowerCase(Locale.ROOT);
        }

        return value;
    }
}
