package com.eidcricketfest.auth.controller;

import com.eidcricketfest.auth.dto.*;
import com.eidcricketfest.auth.service.AuthRateLimiter;
import com.eidcricketfest.auth.service.AuthService;
import com.eidcricketfest.security.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthRateLimiter authRateLimiter;
    private final ClientIpResolver clientIpResolver;

    public AuthController(
            AuthService authService,
            AuthRateLimiter authRateLimiter,
            ClientIpResolver clientIpResolver
    ) {
        this.authService = authService;
        this.authRateLimiter = authRateLimiter;
        this.clientIpResolver = clientIpResolver;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest servletRequest
    ) {
        authRateLimiter.checkRegistration(
                clientIpResolver.resolve(servletRequest)
        );

        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest
    ) {
        authRateLimiter.checkLogin(
                clientIpResolver.resolve(servletRequest),
                request.identifier()
        );

        return authService.login(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest servletRequest
    ) {
        authRateLimiter.checkRefresh(
                clientIpResolver.resolve(servletRequest)
        );

        return authService.refresh(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            @Valid @RequestBody LogoutRequest request
    ) {
        authService.logout(request);
    }
}
