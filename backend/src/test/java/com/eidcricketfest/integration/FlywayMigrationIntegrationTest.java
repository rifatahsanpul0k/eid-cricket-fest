package com.eidcricketfest.integration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayMigrationIntegrationTest
        extends AbstractIntegrationTest {

    @Autowired
    private Flyway flyway;

    @Test
    void shouldApplyAllMigrations() {

        var current =
                flyway.info().current();

        assertThat(current)
                .isNotNull();

        assertThat(
                current.getVersion().getVersion()
        ).isEqualTo("17");
    }

    @Test
    void shouldCreateCoreTables() {

        assertTableExists("users");
        assertTableExists("player_registrations");
        assertTableExists("registration_payments");
        assertTableExists("drafts");
        assertTableExists("matches");
        assertTableExists("innings");
        assertTableExists("deliveries");
        assertTableExists("wickets");
        assertTableExists("tournament_player_awards");
    }

    private void assertTableExists(
            String table
    ) {

        Boolean exists =
                jdbcTemplate.queryForObject(
                        """
                        SELECT EXISTS (
                            SELECT 1
                            FROM information_schema.tables
                            WHERE table_schema = 'public'
                              AND table_name = ?
                        )
                        """,
                        Boolean.class,
                        table
                );

        assertThat(exists).isTrue();
    }
}
