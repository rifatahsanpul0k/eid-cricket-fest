package com.eidcricketfest.common.exception;

public class RateLimitExceededException
        extends RuntimeException {

    private final long retryAfterSeconds;

    public RateLimitExceededException(
            long retryAfterSeconds
    ) {

        super(
                "Too many requests. Try again later."
        );

        this.retryAfterSeconds =
                retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
