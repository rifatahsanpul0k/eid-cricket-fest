package com.eidcricketfest.integration;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class HealthIntegrationTest
        extends AbstractIntegrationTest {

    @Test
    void healthShouldBeUp()
            throws Exception {

        mockMvc.perform(
                        get("/actuator/health")
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("UP")
                )
                .andExpect(
                        header()
                                .exists("X-Request-Id")
                );
    }

    @Test
    void livenessShouldBePublicAndUp()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/actuator/health/liveness"
                        )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("UP")
                )
                .andExpect(
                        header()
                                .exists("X-Request-Id")
                );
    }

    @Test
    void readinessShouldBeUp()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/actuator/health/readiness"
                        )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("UP")
                )
                .andExpect(
                        header()
                                .exists("X-Request-Id")
                );
    }
}
