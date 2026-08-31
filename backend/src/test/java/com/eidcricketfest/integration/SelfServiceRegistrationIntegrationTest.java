package com.eidcricketfest.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SelfServiceRegistrationIntegrationTest
        extends AuthTestSupport {

    @Test
    void playerCanReadOwnRegistrationAndPayments()
            throws Exception {

        TestTokens player = registerPlayer();
        long editionId = createOpenEdition("25.00");

        createProfile(player.accessToken(), "Self Service Player");

        var registration =
                mockMvc.perform(
                                post(
                                        "/api/v1/tournament-editions/{editionId}/registrations/me",
                                        editionId
                                )
                                        .header(
                                                "Authorization",
                                                bearer(player)
                                        )
                                        .contentType(
                                                MediaType.APPLICATION_JSON
                                        )
                                        .content(
                                                jsonMapper.writeValueAsString(
                                                        Map.of(
                                                                "categoryId",
                                                                1
                                                        )
                                                )
                                        )
                        )
                        .andExpect(status().isCreated())
                        .andReturn();

        long registrationId =
                jsonMapper
                        .readTree(
                                registration.getResponse()
                                        .getContentAsString()
                        )
                        .get("id")
                        .asLong();

        mockMvc.perform(
                        get(
                                "/api/v1/tournament-editions/{editionId}/registrations/me",
                                editionId
                        )
                                .header(
                                        "Authorization",
                                        bearer(player)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(registrationId)
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("PENDING")
                );

        mockMvc.perform(
                        post(
                                "/api/v1/registrations/{registrationId}/payments/me",
                                registrationId
                        )
                                .header(
                                        "Authorization",
                                        bearer(player)
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        jsonMapper.writeValueAsString(
                                                Map.of(
                                                        "amount",
                                                        10,
                                                        "paymentMethod",
                                                        "CASH"
                                                )
                                        )
                                )
                )
                .andExpect(status().isCreated());

        mockMvc.perform(
                        get(
                                "/api/v1/registrations/{registrationId}/payments/me",
                                registrationId
                        )
                                .header(
                                        "Authorization",
                                        bearer(player)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].registrationId")
                        .value(registrationId))
                .andExpect(jsonPath("$[0].status")
                        .value("PENDING"));
    }

    @Test
    void playerCannotReadAnotherPlayersPayments()
            throws Exception {

        TestTokens owner = registerPlayer();
        TestTokens other = registerPlayer();
        long editionId = createOpenEdition("25.00");

        createProfile(owner.accessToken(), "Owner Player");
        createProfile(other.accessToken(), "Other Player");

        long registrationId =
                registerForEdition(owner, editionId);

        mockMvc.perform(
                        get(
                                "/api/v1/registrations/{registrationId}/payments/me",
                                registrationId
                        )
                                .header(
                                        "Authorization",
                                        bearer(other)
                                )
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousAccessToSelfStatusIsRejected()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/api/v1/tournament-editions/{editionId}/registrations/me",
                                1
                        )
                )
                .andExpect(status().isUnauthorized());

        mockMvc.perform(
                        get(
                                "/api/v1/registrations/{registrationId}/payments/me",
                                1
                        )
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void organizerRegistrationSearchStillWorks()
            throws Exception {

        TestTokens organizer = registerPlayer();
        addRole(organizer.email(), "ORGANIZER");
        organizer = login(
                organizer.email(),
                organizer.password()
        );

        long editionId = createOpenEdition("0.00");

        mockMvc.perform(
                        get(
                                "/api/v1/tournament-editions/{editionId}/registrations",
                                editionId
                        )
                                .header(
                                        "Authorization",
                                        bearer(organizer)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    private long registerForEdition(
            TestTokens tokens,
            long editionId
    ) throws Exception {

        var result =
                mockMvc.perform(
                                post(
                                        "/api/v1/tournament-editions/{editionId}/registrations/me",
                                        editionId
                                )
                                        .header(
                                                "Authorization",
                                                bearer(tokens)
                                        )
                                        .contentType(
                                                MediaType.APPLICATION_JSON
                                        )
                                        .content(
                                                jsonMapper.writeValueAsString(
                                                        Map.of(
                                                                "categoryId",
                                                                1
                                                        )
                                                )
                                        )
                        )
                        .andExpect(status().isCreated())
                        .andReturn();

        return jsonMapper
                .readTree(
                        result.getResponse()
                                .getContentAsString()
                )
                .get("id")
                .asLong();
    }

    private void createProfile(
            String accessToken,
            String name
    ) throws Exception {

        mockMvc.perform(
                        post("/api/v1/players/me")
                                .header(
                                        "Authorization",
                                        "Bearer " + accessToken
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        jsonMapper.writeValueAsString(
                                                Map.of(
                                                        "fullName",
                                                        name + " " + UUID.randomUUID(),
                                                        "primaryCategoryId",
                                                        1
                                                )
                                        )
                                )
                )
                .andExpect(status().isCreated());
    }

    private long createOpenEdition(String fee) {

        long tournamentId =
                jdbcTemplate.queryForObject(
                        """
                        INSERT INTO tournaments (
                            name,
                            created_at,
                            updated_at
                        )
                        VALUES (
                            ?,
                            CURRENT_TIMESTAMP,
                            CURRENT_TIMESTAMP
                        )
                        RETURNING id
                        """,
                        Long.class,
                        "Self Service Tournament "
                                + UUID.randomUUID()
                );

        return jdbcTemplate.queryForObject(
                """
                INSERT INTO tournament_editions (
                    tournament_id,
                    name,
                    start_date,
                    end_date,
                    registration_start_at,
                    registration_end_at,
                    overs_per_innings,
                    squad_size,
                    playing_xi_size,
                    registration_fee,
                    registration_currency,
                    win_points,
                    tie_points,
                    no_result_points,
                    loss_points,
                    status,
                    created_at,
                    updated_at
                )
                VALUES (
                    ?,
                    ?,
                    CURRENT_DATE,
                    CURRENT_DATE + 1,
                    CURRENT_TIMESTAMP - INTERVAL '1 day',
                    CURRENT_TIMESTAMP + INTERVAL '1 day',
                    2,
                    3,
                    3,
                    ?::numeric,
                    'BDT',
                    2,
                    1,
                    1,
                    0,
                    'REGISTRATION_OPEN',
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                )
                RETURNING id
                """,
                Long.class,
                tournamentId,
                "Self Service Edition " + UUID.randomUUID(),
                fee
        );
    }

    private String bearer(TestTokens tokens) {
        return "Bearer " + tokens.accessToken();
    }
}
