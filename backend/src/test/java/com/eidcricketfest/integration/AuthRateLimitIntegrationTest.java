package com.eidcricketfest.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@TestPropertySource(
        properties = {
                "app.rate-limit.auth.enabled=true",

                "app.rate-limit.auth.login-ip.capacity=100",
                "app.rate-limit.auth.login-ip.refill-tokens=100",
                "app.rate-limit.auth.login-ip.refill-period=1h",

                "app.rate-limit.auth.login-identity.capacity=2",
                "app.rate-limit.auth.login-identity.refill-tokens=2",
                "app.rate-limit.auth.login-identity.refill-period=1h",

                "app.rate-limit.auth.register-ip.capacity=2",
                "app.rate-limit.auth.register-ip.refill-tokens=2",
                "app.rate-limit.auth.register-ip.refill-period=1h",

                "app.rate-limit.auth.refresh-ip.capacity=2",
                "app.rate-limit.auth.refresh-ip.refill-tokens=2",
                "app.rate-limit.auth.refresh-ip.refill-period=1h"
        }
)
class AuthRateLimitIntegrationTest
        extends AuthTestSupport {

    @Test
    void registrationShouldBeRateLimitedByIp()
            throws Exception {

        String ip =
                "203.0.113.10";

        registerFromIp(
                ip,
                randomEmail()
        )
                .andExpect(status().isCreated());

        registerFromIp(
                ip,
                randomEmail()
        )
                .andExpect(status().isCreated());

        registerFromIp(
                ip,
                randomEmail()
        )
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code")
                        .value("RATE_LIMITED"))
                .andExpect(jsonPath("$.retryAfterSeconds")
                        .isNumber())
                .andExpect(header().exists("Retry-After"))
                .andExpect(header().exists("X-Request-Id"));
    }

    @Test
    void registrationLimitShouldBeIsolatedByIp()
            throws Exception {

        String exhaustedIp =
                "203.0.113.20";

        registerFromIp(
                exhaustedIp,
                randomEmail()
        )
                .andExpect(status().isCreated());

        registerFromIp(
                exhaustedIp,
                randomEmail()
        )
                .andExpect(status().isCreated());

        registerFromIp(
                exhaustedIp,
                randomEmail()
        )
                .andExpect(status().isTooManyRequests());

        registerFromIp(
                "203.0.113.21",
                randomEmail()
        )
                .andExpect(status().isCreated());
    }

    @Test
    void loginShouldBeRateLimitedByIdentity()
            throws Exception {

        String ip =
                "203.0.113.30";

        String email =
                randomEmail();

        registerFromIp(
                "203.0.113.31",
                email
        )
                .andExpect(status().isCreated());

        loginFromIp(
                ip,
                email,
                "wrong-password"
        )
                .andExpect(status().isUnauthorized());

        loginFromIp(
                ip,
                email,
                "wrong-password"
        )
                .andExpect(status().isUnauthorized());

        loginFromIp(
                ip,
                email,
                "wrong-password"
        )
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code")
                        .value("RATE_LIMITED"));
    }

    @Test
    void loginIdentityNormalizationShouldPreventBypass()
            throws Exception {

        String ip =
                "203.0.113.40";

        String email =
                randomEmail();

        registerFromIp(
                "203.0.113.41",
                email
        )
                .andExpect(status().isCreated());

        loginFromIp(
                ip,
                email.toUpperCase(),
                "wrong-password"
        )
                .andExpect(status().isUnauthorized());

        loginFromIp(
                ip,
                email,
                "wrong-password"
        )
                .andExpect(status().isUnauthorized());

        loginFromIp(
                ip,
                " " + email + " ",
                "wrong-password"
        )
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code")
                        .value("RATE_LIMITED"));
    }

    @Test
    void loginShouldBeRateLimitedByIp()
            throws Exception {

        String ip =
                "203.0.113.50";

        for (int i = 0; i < 100; i++) {
            loginFromIp(
                    ip,
                    "missing-" + i + "-" + randomEmail(),
                    "wrong-password"
            )
                    .andExpect(status().isUnauthorized());
        }

        loginFromIp(
                ip,
                "missing-final-" + randomEmail(),
                "wrong-password"
        )
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code")
                        .value("RATE_LIMITED"));
    }

    @Test
    void refreshShouldBeRateLimitedByIp()
            throws Exception {

        String ip =
                "203.0.113.60";

        refreshFromIp(
                ip,
                "invalid-refresh-token-1"
        )
                .andExpect(status().isUnauthorized());

        refreshFromIp(
                ip,
                "invalid-refresh-token-2"
        )
                .andExpect(status().isUnauthorized());

        refreshFromIp(
                ip,
                "invalid-refresh-token-3"
        )
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code")
                        .value("RATE_LIMITED"))
                .andExpect(jsonPath("$.retryAfterSeconds")
                        .isNumber())
                .andExpect(header().exists("Retry-After"))
                .andExpect(header().exists("X-Request-Id"));
    }

    private ResultActions registerFromIp(
            String ip,
            String email
    ) throws Exception {

        return mockMvc.perform(
                post("/api/v1/auth/register")
                        .with(remoteAddr(ip))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                jsonMapper.writeValueAsString(
                                        Map.of(
                                                "displayName",
                                                "Rate Limit User",

                                                "email",
                                                email,

                                                "password",
                                                "StrongPassword123"
                                        )
                                )
                        )
        );
    }

    private ResultActions loginFromIp(
            String ip,
            String identifier,
            String password
    ) throws Exception {

        return mockMvc.perform(
                post("/api/v1/auth/login")
                        .with(remoteAddr(ip))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                jsonMapper.writeValueAsString(
                                        Map.of(
                                                "identifier",
                                                identifier,

                                                "password",
                                                password
                                        )
                                )
                        )
        );
    }

    private ResultActions refreshFromIp(
            String ip,
            String refreshToken
    ) throws Exception {

        return mockMvc.perform(
                post("/api/v1/auth/refresh")
                        .with(remoteAddr(ip))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                jsonMapper.writeValueAsString(
                                        Map.of(
                                                "refreshToken",
                                                refreshToken
                                        )
                                )
                        )
        );
    }

    private RequestPostProcessor remoteAddr(
            String ip
    ) {

        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }

    private String randomEmail() {
        return "rate-"
                + UUID.randomUUID()
                + "@example.com";
    }
}
