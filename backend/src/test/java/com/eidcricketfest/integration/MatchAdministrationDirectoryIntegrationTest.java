package com.eidcricketfest.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MatchAdministrationDirectoryIntegrationTest
        extends AuthTestSupport {

    @Test
    void organizerCanListVenuesAndScorers()
            throws Exception {

        TestTokens account =
                registerPlayer();

        addRole(account.email(), "ORGANIZER");
        addRole(account.email(), "SCORER");

        TestTokens organizer =
                login(
                        account.email(),
                        account.password()
                );

        String venueName =
                "Venue-" + UUID.randomUUID();

        mockMvc.perform(
                        post("/api/v1/venues")
                                .header(
                                        "Authorization",
                                        "Bearer " + organizer.accessToken()
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        jsonMapper.writeValueAsString(
                                                Map.of(
                                                        "name",
                                                        venueName,
                                                        "address",
                                                        "Dhaka"
                                                )
                                        )
                                )
                )
                .andExpect(status().isCreated());

        mockMvc.perform(
                        get("/api/v1/venues")
                                .header(
                                        "Authorization",
                                        "Bearer " + organizer.accessToken()
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == '" + venueName + "')]")
                        .exists());

        mockMvc.perform(
                        get("/api/v1/users/scorers")
                                .header(
                                        "Authorization",
                                        "Bearer " + organizer.accessToken()
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.email == '" + account.email() + "')]")
                        .exists());
    }

    @Test
    void playerCannotListOperationalDirectories()
            throws Exception {

        TestTokens player =
                registerPlayer();

        mockMvc.perform(
                        get("/api/v1/venues")
                                .header(
                                        "Authorization",
                                        "Bearer " + player.accessToken()
                                )
                )
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        get("/api/v1/users/scorers")
                                .header(
                                        "Authorization",
                                        "Bearer " + player.accessToken()
                                )
                )
                .andExpect(status().isForbidden());
    }
}
