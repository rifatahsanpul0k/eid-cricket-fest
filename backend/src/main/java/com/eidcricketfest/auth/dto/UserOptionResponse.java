package com.eidcricketfest.auth.dto;

public record UserOptionResponse(

        Long id,
        String displayName,
        String email,
        String phone
) {}
