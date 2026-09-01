package com.eidcricketfest.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static java.util.Map.entry;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TournamentLifecycleIntegrationTest
        extends AuthTestSupport {

    @Test
    void adminCanCreateTournament()
            throws Exception {

        TestTokens admin = loginWithRole("ADMIN");
        String name = uniqueName("Admin Tournament");

        mockMvc.perform(
                        post("/api/v1/tournaments")
                                .header("Authorization", bearer(admin))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        jsonMapper.writeValueAsString(
                                                Map.of("name", name)
                                        )
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name));
    }

    @Test
    void playerAndAnonymousCannotMutateTournamentLifecycle()
            throws Exception {

        TestTokens organizer = loginWithRole("ORGANIZER");
        TestTokens player = registerPlayer();
        long tournamentId = createTournament(organizer);
        long editionId = createEdition(organizer, tournamentId);

        mockMvc.perform(
                        post("/api/v1/tournaments")
                                .header("Authorization", bearer(player))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        jsonMapper.writeValueAsString(
                                                Map.of(
                                                        "name",
                                                        uniqueName("Blocked")
                                                )
                                        )
                                )
                )
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        put(
                                "/api/v1/tournaments/{tournamentId}/editions/{editionId}",
                                tournamentId,
                                editionId
                        )
                                .header("Authorization", bearer(player))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(editionJson(uniqueName("Blocked Edit")))
                )
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        patch(
                                "/api/v1/tournaments/{tournamentId}/editions/{editionId}/status",
                                tournamentId,
                                editionId
                        )
                                .header("Authorization", bearer(player))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(statusJson("REGISTRATION_OPEN"))
                )
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        patch(
                                "/api/v1/tournaments/{tournamentId}/editions/{editionId}/status",
                                tournamentId,
                                editionId
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(statusJson("REGISTRATION_OPEN"))
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void draftEditionCanBeEditedButNotAfterRegistrationOpens()
            throws Exception {

        TestTokens organizer = loginWithRole("ORGANIZER");
        long tournamentId = createTournament(organizer);
        long editionId = createEdition(organizer, tournamentId);

        mockMvc.perform(
                        put(
                                "/api/v1/tournaments/{tournamentId}/editions/{editionId}",
                                tournamentId,
                                editionId
                        )
                                .header("Authorization", bearer(organizer))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(editionJson("Edited " + UUID.randomUUID()))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.oversPerInnings").value(8));

        mockMvc.perform(
                        put(
                                "/api/v1/tournaments/{tournamentId}/editions/{editionId}",
                                tournamentId,
                                editionId
                        )
                                .header("Authorization", bearer(organizer))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidPlayingXiJson())
                )
                .andExpect(status().isBadRequest());

        transition(organizer, tournamentId, editionId, "REGISTRATION_OPEN")
                .andExpect(status().isOk());

        mockMvc.perform(
                        put(
                                "/api/v1/tournaments/{tournamentId}/editions/{editionId}",
                                tournamentId,
                                editionId
                        )
                                .header("Authorization", bearer(organizer))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(editionJson("Too Late"))
                )
                .andExpect(status().isConflict());
    }

    @Test
    void lifecycleAllowsOnlyControlledForwardTransitions()
            throws Exception {

        TestTokens organizer = loginWithRole("ORGANIZER");
        long tournamentId = createTournament(organizer);
        long editionId = createEdition(organizer, tournamentId);

        transition(organizer, tournamentId, editionId, "DRAFTING")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("Cannot transition")));

        transition(organizer, tournamentId, editionId, "REGISTRATION_OPEN")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REGISTRATION_OPEN"));

        transition(organizer, tournamentId, editionId, "DRAFT")
                .andExpect(status().isConflict());

        transition(organizer, tournamentId, editionId, "REGISTRATION_CLOSED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REGISTRATION_CLOSED"));

        transition(organizer, tournamentId, editionId, "DRAFTING")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFTING"));

        transition(organizer, tournamentId, editionId, "SCHEDULED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SCHEDULED"));

        transition(organizer, tournamentId, editionId, "ONGOING")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ONGOING"));

        transition(organizer, tournamentId, editionId, "SCHEDULED")
                .andExpect(status().isConflict());
    }

    @Test
    void cancelledEditionIsTerminal()
            throws Exception {

        TestTokens organizer = loginWithRole("ORGANIZER");
        long tournamentId = createTournament(organizer);
        long editionId = createEdition(organizer, tournamentId);

        transition(organizer, tournamentId, editionId, "CANCELLED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        transition(organizer, tournamentId, editionId, "REGISTRATION_OPEN")
                .andExpect(status().isConflict());
    }

    @Test
    void lifecycleTransitionOpensAndClosesSelfRegistration()
            throws Exception {

        TestTokens organizer = loginWithRole("ORGANIZER");
        long tournamentId = createTournament(organizer);
        long editionId = createEdition(organizer, tournamentId);

        transition(organizer, tournamentId, editionId, "REGISTRATION_OPEN")
                .andExpect(status().isOk());

        TestTokens firstPlayer = registerPlayer();
        createMyPlayerProfile(firstPlayer);

        mockMvc.perform(
                        post(
                                "/api/v1/tournament-editions/{editionId}/registrations/me",
                                editionId
                        )
                                .header("Authorization", bearer(firstPlayer))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        jsonMapper.writeValueAsString(
                                                Map.of("categoryId", 1)
                                        )
                                )
                )
                .andExpect(status().isCreated());

        transition(organizer, tournamentId, editionId, "REGISTRATION_CLOSED")
                .andExpect(status().isOk());

        TestTokens secondPlayer = registerPlayer();
        createMyPlayerProfile(secondPlayer);

        mockMvc.perform(
                        post(
                                "/api/v1/tournament-editions/{editionId}/registrations/me",
                                editionId
                        )
                                .header("Authorization", bearer(secondPlayer))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        jsonMapper.writeValueAsString(
                                                Map.of("categoryId", 1)
                                        )
                                )
                )
                .andExpect(status().isConflict());
    }

    private TestTokens loginWithRole(String role)
            throws Exception {

        TestTokens account = registerPlayer();
        addRole(account.email(), role);

        return login(account.email(), account.password());
    }

    private long createTournament(TestTokens tokens)
            throws Exception {

        MvcResult result =
                mockMvc.perform(
                                post("/api/v1/tournaments")
                                        .header("Authorization", bearer(tokens))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                jsonMapper.writeValueAsString(
                                                        Map.of(
                                                                "name",
                                                                uniqueName("Lifecycle")
                                                        )
                                                )
                                        )
                        )
                        .andExpect(status().isCreated())
                        .andReturn();

        return jsonMapper.readTree(
                        result.getResponse()
                                .getContentAsString()
                )
                .get("id")
                .asLong();
    }

    private long createEdition(
            TestTokens tokens,
            long tournamentId
    ) throws Exception {

        MvcResult result =
                mockMvc.perform(
                                post(
                                        "/api/v1/tournaments/{tournamentId}/editions",
                                        tournamentId
                                )
                                        .header("Authorization", bearer(tokens))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(editionJson(uniqueName("Edition")))
                        )
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.status").value("DRAFT"))
                        .andReturn();

        return jsonMapper.readTree(
                        result.getResponse()
                                .getContentAsString()
                )
                .get("id")
                .asLong();
    }

    private org.springframework.test.web.servlet.ResultActions transition(
            TestTokens tokens,
            long tournamentId,
            long editionId,
            String status
    ) throws Exception {

        return mockMvc.perform(
                patch(
                        "/api/v1/tournaments/{tournamentId}/editions/{editionId}/status",
                        tournamentId,
                        editionId
                )
                        .header("Authorization", bearer(tokens))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusJson(status))
        );
    }

    private void createMyPlayerProfile(TestTokens tokens)
            throws Exception {

        mockMvc.perform(
                        post("/api/v1/players/me")
                                .header("Authorization", bearer(tokens))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        jsonMapper.writeValueAsString(
                                                Map.of(
                                                        "fullName",
                                                        uniqueName("Player"),
                                                        "primaryCategoryId",
                                                        1
                                                )
                                        )
                                )
                )
                .andExpect(status().isCreated());
    }

    private String editionJson(String name)
            throws Exception {

        return jsonMapper.writeValueAsString(
                Map.ofEntries(
                        entry("name", name),
                        entry("startDate", "2027-03-01"),
                        entry("endDate", "2027-03-10"),
                        entry("oversPerInnings", 8),
                        entry("squadSize", 15),
                        entry("playingXiSize", 11),
                        entry("registrationFee", 100),
                        entry("registrationCurrency", "BDT"),
                        entry("winPoints", 2),
                        entry("tiePoints", 1),
                        entry("noResultPoints", 1),
                        entry("lossPoints", 0)
                )
        );
    }

    private String invalidPlayingXiJson()
            throws Exception {

        return jsonMapper.writeValueAsString(
                Map.of(
                        "name",
                        uniqueName("Invalid Edition"),
                        "oversPerInnings",
                        5,
                        "squadSize",
                        3,
                        "playingXiSize",
                        4
                )
        );
    }

    private String statusJson(String status)
            throws Exception {

        return jsonMapper.writeValueAsString(
                Map.of("status", status)
        );
    }

    private String bearer(TestTokens tokens) {
        return "Bearer " + tokens.accessToken();
    }

    private String uniqueName(String prefix) {
        return prefix + " " + UUID.randomUUID();
    }
}
