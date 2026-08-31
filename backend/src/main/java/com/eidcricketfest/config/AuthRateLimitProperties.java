package com.eidcricketfest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(
        prefix = "app.rate-limit.auth"
)
public record AuthRateLimitProperties(

        boolean enabled,

        long cacheMaxSize,

        Duration cacheExpireAfterAccess,

        Limit loginIp,

        Limit loginIdentity,

        Limit registerIp,

        Limit refreshIp
) {

    public record Limit(
            long capacity,
            long refillTokens,
            Duration refillPeriod
    ) {
    }
}
