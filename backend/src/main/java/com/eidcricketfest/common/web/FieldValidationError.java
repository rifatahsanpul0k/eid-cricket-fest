package com.eidcricketfest.common.web;

public record FieldValidationError(
        String field,
        String message
) {}
