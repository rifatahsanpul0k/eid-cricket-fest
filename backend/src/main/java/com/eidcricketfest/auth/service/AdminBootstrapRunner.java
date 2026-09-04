package com.eidcricketfest.auth.service;

import com.eidcricketfest.common.exception.ConflictException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrapRunner
        implements ApplicationRunner {

    private static final Logger log =
            LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final AuthService authService;
    private final String displayName;
    private final String email;
    private final String password;

    public AdminBootstrapRunner(
            AuthService authService,
            @Value("${app.bootstrap.admin-display-name:}") String displayName,
            @Value("${app.bootstrap.admin-email:}") String email,
            @Value("${app.bootstrap.admin-password:}") String password
    ) {
        this.authService = authService;
        this.displayName = displayName == null ? "" : displayName;
        this.email = email == null ? "" : email;
        this.password = password == null ? "" : password;
    }

    @Override
    public void run(
            ApplicationArguments args
    ) {

        if (displayName.isBlank()
                && email.isBlank()
                && password.isBlank()) {
            return;
        }

        if (displayName.isBlank()
                || email.isBlank()
                || password.isBlank()) {
            throw new IllegalStateException(
                    "First admin bootstrap requires display name, email, and password"
            );
        }

        if (password.length() < 8
                || password.length() > 72) {
            throw new IllegalStateException(
                    "First admin bootstrap password must be 8 to 72 characters"
            );
        }

        try {
            authService.bootstrapConfiguredAdmin(
                    displayName,
                    email,
                    password
            );

            log.info(
                    "Bootstrapped first admin account for {}",
                    email
            );
        } catch (ConflictException ex) {
            log.info(
                    "First admin bootstrap skipped: {}",
                    ex.getMessage()
            );
        }
    }
}
