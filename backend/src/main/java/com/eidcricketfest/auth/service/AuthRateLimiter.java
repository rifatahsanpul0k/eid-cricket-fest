package com.eidcricketfest.auth.service;

import com.eidcricketfest.common.exception.RateLimitExceededException;
import com.eidcricketfest.config.AuthRateLimitProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class AuthRateLimiter {

    private final AuthRateLimitProperties properties;

    private final Cache<String, Bucket> buckets;

    public AuthRateLimiter(
            AuthRateLimitProperties properties
    ) {

        this.properties =
                properties;

        this.buckets =
                Caffeine.newBuilder()
                        .maximumSize(
                                properties.cacheMaxSize()
                        )
                        .expireAfterAccess(
                                properties
                                        .cacheExpireAfterAccess()
                        )
                        .build();
    }

    public void checkLogin(
            String clientIp,
            String email
    ) {

        if (!properties.enabled()) {
            return;
        }

        consume(
                "login-identity:"
                        + hashIdentity(email),

                properties.loginIdentity()
        );

        consume(
                "login-ip:"
                        + clientIp,

                properties.loginIp()
        );
    }

    public void checkRegistration(
            String clientIp
    ) {

        if (!properties.enabled()) {
            return;
        }

        consume(
                "register-ip:"
                        + clientIp,

                properties.registerIp()
        );
    }

    public void checkRefresh(
            String clientIp
    ) {

        if (!properties.enabled()) {
            return;
        }

        consume(
                "refresh-ip:"
                        + clientIp,

                properties.refreshIp()
        );
    }

    private void consume(
            String key,
            AuthRateLimitProperties.Limit limit
    ) {

        Bucket bucket =
                buckets.get(
                        key,
                        ignored ->
                                createBucket(
                                        limit
                                )
                );

        ConsumptionProbe probe =
                bucket
                        .tryConsumeAndReturnRemaining(
                                1
                        );

        if (probe.isConsumed()) {
            return;
        }

        long retryAfterSeconds =
                nanosToSecondsCeiling(
                        probe.getNanosToWaitForRefill()
                );

        throw new RateLimitExceededException(
                retryAfterSeconds
        );
    }

    private Bucket createBucket(
            AuthRateLimitProperties.Limit limit
    ) {

        return Bucket.builder()
                .addLimit(
                        bandwidth ->
                                bandwidth
                                        .capacity(
                                                limit.capacity()
                                        )
                                        .refillGreedy(
                                                limit.refillTokens(),
                                                limit.refillPeriod()
                                        )
                )
                .build();
    }

    private long nanosToSecondsCeiling(
            long nanos
    ) {

        if (nanos <= 0) {
            return 1;
        }

        return Math.max(
                1,
                (nanos + 999_999_999L)
                        / 1_000_000_000L
        );
    }

    private String hashIdentity(
            String email
    ) {

        String normalized =
                email == null
                        ? ""
                        : email
                        .trim()
                        .toLowerCase(Locale.ROOT);

        try {

            MessageDigest digest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );

            byte[] hash =
                    digest.digest(
                            normalized.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            return HexFormat.of()
                    .formatHex(hash);

        } catch (NoSuchAlgorithmException ex) {

            throw new IllegalStateException(
                    "SHA-256 unavailable",
                    ex
            );
        }
    }
}
