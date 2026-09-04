package com.eidcricketfest.auth.controller;

import com.eidcricketfest.auth.dto.*;
import com.eidcricketfest.auth.service.AuthRateLimiter;
import com.eidcricketfest.auth.service.AuthService;
import com.eidcricketfest.security.ClientIpResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Authentication")
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

    @Operation(summary = "Register account")
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

    @Operation(summary = "Bootstrap first admin account")
    @PostMapping("/bootstrap-admin")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse bootstrapAdmin(
            @Valid @RequestBody BootstrapAdminRequest request,
            HttpServletRequest servletRequest
    ) {
        authRateLimiter.checkRegistration(
                clientIpResolver.resolve(servletRequest)
        );

        return authService.bootstrapAdmin(request);
    }

    @Operation(summary = "Login")
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

    @Operation(summary = "Refresh access token")
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

    @Operation(summary = "Logout")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            @Valid @RequestBody LogoutRequest request
    ) {
        authService.logout(request);
    }
}
