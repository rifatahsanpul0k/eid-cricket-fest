package com.eidcricketfest.integration;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class QueryCountSupport
        extends AbstractIntegrationTest {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    protected Statistics statistics() {

        return entityManagerFactory
                .unwrap(SessionFactory.class)
                .getStatistics();
    }

    protected long measureStatements(
            Runnable operation
    ) {

        Statistics statistics =
                statistics();

        boolean previouslyEnabled =
                statistics.isStatisticsEnabled();

        statistics.setStatisticsEnabled(true);
        statistics.clear();

        try {

            operation.run();

            return statistics
                    .getPrepareStatementCount();

        } finally {

            statistics.clear();

            statistics.setStatisticsEnabled(
                    previouslyEnabled
            );
        }
    }
}
