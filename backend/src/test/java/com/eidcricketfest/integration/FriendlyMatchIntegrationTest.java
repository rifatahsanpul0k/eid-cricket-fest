package com.eidcricketfest.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FriendlyMatchIntegrationTest
        extends AuthTestSupport {

    @Test
    void organizerCanCreateFriendlyMatchFromMixedPlayerPool()
            throws Exception {

        TestTokens organizer =
                organizer();

        Long venueId =
                venue("Friendly Ground");

        MixedPlayers players =
                mixedPlayers();

        long registrationsBefore =
                count("player_registrations");

        long paymentsBefore =
                count("registration_payments");

        long draftPicksBefore =
                count("draft_picks");

        long rosterBefore =
                count("team_roster_entries");

        var result =
                mockMvc.perform(
                                post("/api/v1/friendly-matches")
                                        .header(
                                                "Authorization",
                                                "Bearer " + organizer.accessToken()
                                        )
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                jsonMapper.writeValueAsString(
                                                        friendlyRequest(
                                                                venueId,
                                                                List.of(
                                                                        players.pulok(),
                                                                        players.nitol(),
                                                                        players.kanok()
                                                                ),
                                                                List.of(
                                                                        players.limon(),
                                                                        players.hridoy(),
                                                                        players.ariyan()
                                                                )
                                                        )
                                                )
                                        )
                        )
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.matchType").value("FRIENDLY"))
                        .andExpect(jsonPath("$.teamA.name").value("Thunder XI"))
                        .andExpect(jsonPath("$.teamB.name").value("Warriors XI"))
                        .andExpect(jsonPath("$.teamA.tournamentTeamId").doesNotExist())
                        .andExpect(jsonPath("$.teamB.tournamentTeamId").doesNotExist())
                        .andExpect(jsonPath("$.teamAPlayingXiSubmitted").value(true))
                        .andExpect(jsonPath("$.teamBPlayingXiSubmitted").value(true))
                        .andReturn();

        Long matchId =
                jsonMapper.readTree(
                        result.getResponse()
                                .getContentAsString()
                ).get("id").asLong();

        assertThat(
                jdbcTemplate.queryForObject(
                        """
                        SELECT tournament_edition_id
                        FROM matches
                        WHERE id = ?
                        """,
                        Long.class,
                        matchId
                )
        ).isNull();

        assertThat(
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM match_sides
                        WHERE match_id = ?
                        """,
                        Long.class,
                        matchId
                )
        ).isEqualTo(2L);

        assertThat(
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM playing_xi_entries
                        WHERE match_id = ?
                          AND player_id IS NOT NULL
                          AND player_registration_id IS NULL
                        """,
                        Long.class,
                        matchId
                )
        ).isEqualTo(6L);

        assertThat(count("player_registrations"))
                .isEqualTo(registrationsBefore);

        assertThat(count("registration_payments"))
                .isEqualTo(paymentsBefore);

        assertThat(count("draft_picks"))
                .isEqualTo(draftPicksBefore);

        assertThat(count("team_roster_entries"))
                .isEqualTo(rosterBefore);
    }

    @Test
    void adminCanCreateFriendlyMatch()
            throws Exception {

        TestTokens admin =
                registerPlayer();

        addRole(admin.email(), "ADMIN");

        admin = login(
                admin.email(),
                admin.password()
        );

        MixedPlayers players =
                sixPlainPlayers();

        mockMvc.perform(
                        post("/api/v1/friendly-matches")
                                .header(
                                        "Authorization",
                                        "Bearer " + admin.accessToken()
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        jsonMapper.writeValueAsString(
                                                friendlyRequest(
                                                        venue("Admin Friendly Ground"),
                                                        List.of(
                                                                players.pulok(),
                                                                players.nitol()
                                                        ),
                                                        List.of(
                                                                players.limon(),
                                                                players.ariyan()
                                                        )
                                                )
                                        )
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.matchType").value("FRIENDLY"));
    }

    @Test
    void unauthorizedRolesCannotCreateFriendlyMatch()
            throws Exception {

        MixedPlayers players =
                sixPlainPlayers();

        Map<String, Object> request =
                friendlyRequest(
                        venue("Restricted Friendly Ground"),
                        List.of(players.pulok(), players.nitol()),
                        List.of(players.limon(), players.ariyan())
                );

        TestTokens player =
                registerPlayer();

        mockMvc.perform(
                        post("/api/v1/friendly-matches")
                                .header(
                                        "Authorization",
                                        "Bearer " + player.accessToken()
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(request))
                )
                .andExpect(status().isForbidden());

        TestTokens scorer =
                registerPlayer();

        addRole(scorer.email(), "SCORER");

        scorer = login(
                scorer.email(),
                scorer.password()
        );

        mockMvc.perform(
                        post("/api/v1/friendly-matches")
                                .header(
                                        "Authorization",
                                        "Bearer " + scorer.accessToken()
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(request))
                )
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        post("/api/v1/friendly-matches")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(request))
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validatesFriendlyMatchPlayersAndOvers()
            throws Exception {

        TestTokens organizer =
                organizer();

        Long venueId =
                venue("Validation Friendly Ground");

        MixedPlayers players =
                sixPlainPlayers();

        mockMvc.perform(
                        post("/api/v1/friendly-matches")
                                .header(
                                        "Authorization",
                                        "Bearer " + organizer.accessToken()
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        jsonMapper.writeValueAsString(
                                                friendlyRequest(
                                                        venueId,
                                                        List.of(
                                                                players.pulok(),
                                                                players.pulok()
                                                        ),
                                                        List.of(
                                                                players.limon(),
                                                                players.ariyan()
                                                        )
                                                )
                                        )
                                )
                )
                .andExpect(status().isConflict());

        mockMvc.perform(
                        post("/api/v1/friendly-matches")
                                .header(
                                        "Authorization",
                                        "Bearer " + organizer.accessToken()
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        jsonMapper.writeValueAsString(
                                                friendlyRequest(
                                                        venueId,
                                                        List.of(
                                                                players.pulok(),
                                                                players.nitol()
                                                        ),
                                                        List.of(
                                                                players.pulok(),
                                                                players.ariyan()
                                                        )
                                                )
                                        )
                                )
                )
                .andExpect(status().isConflict());

        mockMvc.perform(
                        post("/api/v1/friendly-matches")
                                .header(
                                        "Authorization",
                                        "Bearer " + organizer.accessToken()
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        jsonMapper.writeValueAsString(
                                                friendlyRequest(
                                                        venueId,
                                                        List.of(
                                                                players.pulok()
                                                        ),
                                                        List.of(
                                                                players.limon(),
                                                                players.ariyan()
                                                        )
                                                )
                                        )
                                )
                )
                .andExpect(status().isConflict());

        mockMvc.perform(
                        post("/api/v1/friendly-matches")
                                .header(
                                        "Authorization",
                                        "Bearer " + organizer.accessToken()
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        jsonMapper.writeValueAsString(
                                                friendlyRequest(
                                                        venueId,
                                                        List.of(
                                                                players.pulok(),
                                                                999_999L
                                                        ),
                                                        List.of(
                                                                players.limon(),
                                                                players.ariyan()
                                                        )
                                                )
                                        )
                                )
                )
                .andExpect(status().isNotFound());

        Map<String, Object> invalidOvers =
                friendlyRequest(
                        venueId,
                        List.of(players.pulok(), players.nitol()),
                        List.of(players.limon(), players.ariyan())
                );

        invalidOvers.put("oversPerInnings", 0);

        mockMvc.perform(
                        post("/api/v1/friendly-matches")
                                .header(
                                        "Authorization",
                                        "Bearer " + organizer.accessToken()
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(invalidOvers))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void organizerCanListFriendlyPlayerOptions()
            throws Exception {

        TestTokens organizer =
                organizer();

        Long playerId =
                player("Option Player", "BATSMAN");

        mockMvc.perform(
                        get("/api/v1/friendly-matches/player-options")
                                .header(
                                        "Authorization",
                                        "Bearer " + organizer.accessToken()
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.playerId == " + playerId + ")]")
                        .exists());
    }

    private TestTokens organizer()
            throws Exception {

        TestTokens account =
                registerPlayer();

        addRole(account.email(), "ORGANIZER");

        return login(
                account.email(),
                account.password()
        );
    }

    private Long venue(String baseName) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO venues (name, address)
                VALUES (?, 'Dhaka')
                RETURNING id
                """,
                Long.class,
                baseName + "-" + UUID.randomUUID()
        );
    }

    private MixedPlayers mixedPlayers() {

        Long editionA =
                edition("Friendly Fixture Edition A");

        Long editionB =
                edition("Friendly Fixture Edition B");

        Long pulok =
                player("Pulok", "ALL_ROUNDER");

        Long limon =
                player("Limon", "BATSMAN");

        Long nitol =
                player("Nitol", "BOWLER");

        Long kanok =
                player("Kanok", "WICKETKEEPER");

        Long hridoy =
                player("Hridoy", "BATSMAN");

        Long ariyan =
                player("Ariyan", "BOWLER");

        Long pulokRegistration =
                registration(
                        editionA,
                        pulok,
                        "APPROVED"
                );

        payment(
                pulokRegistration,
                "VERIFIED"
        );

        registration(
                editionA,
                limon,
                "PENDING"
        );

        registration(
                editionA,
                kanok,
                "APPROVED"
        );

        registration(
                editionA,
                hridoy,
                "REJECTED"
        );

        registration(
                editionB,
                ariyan,
                "APPROVED"
        );

        return new MixedPlayers(
                pulok,
                limon,
                nitol,
                kanok,
                hridoy,
                ariyan
        );
    }

    private MixedPlayers sixPlainPlayers() {
        return new MixedPlayers(
                player("Pulok-" + UUID.randomUUID(), "ALL_ROUNDER"),
                player("Limon-" + UUID.randomUUID(), "BATSMAN"),
                player("Nitol-" + UUID.randomUUID(), "BOWLER"),
                player("Kanok-" + UUID.randomUUID(), "WICKETKEEPER"),
                player("Hridoy-" + UUID.randomUUID(), "BATSMAN"),
                player("Ariyan-" + UUID.randomUUID(), "BOWLER")
        );
    }

    private Long edition(String baseName) {
        Long tournamentId =
                jdbcTemplate.queryForObject(
                        """
                        INSERT INTO tournaments (name)
                        VALUES (?)
                        RETURNING id
                        """,
                        Long.class,
                        baseName + " Tournament " + UUID.randomUUID()
                );

        return jdbcTemplate.queryForObject(
                """
                INSERT INTO tournament_editions (
                    tournament_id,
                    name,
                    overs_per_innings,
                    status
                )
                VALUES (?, ?, 2, 'REGISTRATION_OPEN')
                RETURNING id
                """,
                Long.class,
                tournamentId,
                baseName + " " + UUID.randomUUID()
        );
    }

    private Long player(
            String name,
            String category
    ) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO players (
                    full_name,
                    primary_category_id
                )
                SELECT ?, id
                FROM player_categories
                WHERE code = ?
                RETURNING id
                """,
                Long.class,
                name,
                category
        );
    }

    private Long registration(
            Long editionId,
            Long playerId,
            String status
    ) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO player_registrations (
                    tournament_edition_id,
                    player_id,
                    category_id,
                    status
                )
                SELECT ?, ?, id, ?
                FROM player_categories
                WHERE code = 'BATSMAN'
                RETURNING id
                """,
                Long.class,
                editionId,
                playerId,
                status
        );
    }

    private void payment(
            Long registrationId,
            String status
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO registration_payments (
                    registration_id,
                    amount,
                    payment_method,
                    status
                )
                VALUES (?, 100, 'CASH', ?)
                """,
                registrationId,
                status
        );
    }

    private long count(String table) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table,
                Long.class
        );
    }

    private Map<String, Object> friendlyRequest(
            Long venueId,
            List<Long> teamA,
            List<Long> teamB
    ) {
        return new java.util.LinkedHashMap<>(
                Map.of(
                        "teamAName",
                        "Thunder XI",
                        "teamBName",
                        "Warriors XI",
                        "teamAPlayerIds",
                        teamA,
                        "teamBPlayerIds",
                        teamB,
                        "oversPerInnings",
                        10,
                        "scheduledAt",
                        Instant.now().plusSeconds(3600).toString(),
                        "venueId",
                        venueId
                )
        );
    }

    private record MixedPlayers(
            Long pulok,
            Long limon,
            Long nitol,
            Long kanok,
            Long hridoy,
            Long ariyan
    ) {}
}
