package com.eidcricketfest.auth.dto;

import com.eidcricketfest.auth.entity.RoleCode;

import java.util.Set;

public record AuthResponse(

        String accessToken,

        String refreshToken,

        String tokenType,

        long expiresIn,

        UserInfo user
) {

    public record UserInfo(
            Long id,
            String displayName,
            String email,
            String phone,
            Set<RoleCode> roles
    ) {
    }
}