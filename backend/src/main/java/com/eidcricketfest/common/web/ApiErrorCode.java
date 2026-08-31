package com.eidcricketfest.common.web;

public enum ApiErrorCode {

    VALIDATION_ERROR,
    INVALID_REQUEST,

    UNAUTHORIZED,
    FORBIDDEN,

    NOT_FOUND,

    CONFLICT,
    DATA_CONFLICT,
    CONCURRENT_MODIFICATION,
    INVALID_STATE,

    RATE_LIMITED,

    INTERNAL_ERROR
}
