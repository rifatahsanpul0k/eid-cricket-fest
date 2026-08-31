package com.eidcricketfest.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SecurityIntegrationTest
        extends AuthTestSupport {

    @Test
    void playerCannotCreateTournament()
            throws Exception {

        TestTokens player =
                registerPlayer();

        mockMvc.perform(
                        post("/api/v1/tournaments")
                                .header(
                                        "Authorization",
                                        "Bearer "
                                                + player.accessToken()
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "name": "Forbidden Tournament"
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isForbidden()
                );
    }

    @Test
    void organizerCanCreateTournament()
            throws Exception {

        TestTokens account =
                registerPlayer();

        addRole(
                account.email(),
                "ORGANIZER"
        );

        /*
         * Login again because the old access token
         * only contains PLAYER.
         */
        TestTokens organizer =
                login(
                        account.email(),
                        account.password()
                );

        String tournamentName =
                "Tournament-"
                + UUID.randomUUID();

        mockMvc.perform(
                        post("/api/v1/tournaments")
                                .header(
                                        "Authorization",
                                        "Bearer "
                                                + organizer.accessToken()
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        jsonMapper.writeValueAsString(
                                                Map.of(
                                                        "name",
                                                        tournamentName
                                                )
                                        )
                                )
                )
                .andExpect(
                        status().isCreated()
                )
                .andExpect(
                        jsonPath("$.name")
                                .value(tournamentName)
                );
    }

    @Test
    void tournamentGetShouldBePublic()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/api/v1/tournaments"
                        )
                )
                .andExpect(
                        status().isOk()
                );
    }

    @Test
    void protectedEndpointWithoutJwtShouldReturn401()
            throws Exception {

        mockMvc.perform(
                        post(
                                "/api/v1/teams"
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "name": "Unauthorized Team"
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isUnauthorized()
                );
    }
}
