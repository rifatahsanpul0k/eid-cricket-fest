package com.eidcricketfest.auth.service;

import com.eidcricketfest.auth.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class TokenService {

    private static final SecureRandom SECURE_RANDOM =
            new SecureRandom();

    private final JwtEncoder jwtEncoder;

    private final String issuer;
    private final Duration accessTokenTtl;
    private final Duration refreshTokenTtl;

    public TokenService(
            JwtEncoder jwtEncoder,

            @Value("${app.jwt.issuer}")
            String issuer,

            @Value("${app.jwt.access-token-ttl}")
            Duration accessTokenTtl,

            @Value("${app.jwt.refresh-token-ttl}")
            Duration refreshTokenTtl
    ) {
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.accessTokenTtl = accessTokenTtl;
        this.refreshTokenTtl = refreshTokenTtl;
    }

    public AccessToken createAccessToken(User user) {

        Instant now = Instant.now();
        Instant expiresAt = now.plus(accessTokenTtl);

        List<String> roles = user.getRoles()
                .stream()
                .map(role -> role.getCode().name())
                .sorted()
                .toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())

                .claim("userId", user.getId())
                .claim("displayName", user.getDisplayName())
                .claim("roles", roles)

                .build();

        JwsHeader header = JwsHeader
                .with(SignatureAlgorithm.RS256)
                .build();

        String token = jwtEncoder
                .encode(
                        JwtEncoderParameters.from(
                                header,
                                claims
                        )
                )
                .getTokenValue();

        return new AccessToken(
                token,
                accessTokenTtl.toSeconds()
        );
    }

    public String generateRefreshToken() {

        byte[] bytes = new byte[32];

        SECURE_RANDOM.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    public String hashRefreshToken(String rawToken) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    rawToken.getBytes(
                            StandardCharsets.UTF_8
                    )
            );

            return HexFormat.of()
                    .formatHex(hash);

        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(
                    "SHA-256 is not available",
                    ex
            );
        }
    }

    public Instant refreshTokenExpiration() {
        return Instant.now()
                .plus(refreshTokenTtl);
    }

    public record AccessToken(
            String value,
            long expiresInSeconds
    ) {
    }
}