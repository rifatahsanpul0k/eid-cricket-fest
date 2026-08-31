package com.eidcricketfest.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileConfigurationIntegrationTest
        extends AbstractIntegrationTest {

    @Autowired
    private Environment environment;

    @Autowired
    private DataSource dataSource;

    @Test
    void testProfileShouldUseTestcontainersDatasource()
            throws SQLException {

        assertThat(environment.getActiveProfiles())
                .contains("test");

        try (Connection connection =
                     dataSource.getConnection()) {

            String url =
                    connection.getMetaData()
                            .getURL();

            assertThat(url)
                    .contains("eid_cricket_fest_test");

            assertThat(url)
                    .doesNotContain(
                            "localhost:5433/eid_cricket_fest"
                    );
        }
    }
}
