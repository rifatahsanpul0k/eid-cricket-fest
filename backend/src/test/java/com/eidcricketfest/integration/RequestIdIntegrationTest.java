package com.eidcricketfest.integration;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RequestIdIntegrationTest
        extends AbstractIntegrationTest {

    @Test
    void shouldGenerateRequestIdWhenMissing()
            throws Exception {

        mockMvc.perform(
                        get("/actuator/health")
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        header()
                                .exists(
                                        "X-Request-Id"
                                )
                );
    }


    @Test
    void shouldPreserveValidClientRequestId()
            throws Exception {

        mockMvc.perform(
                        get("/actuator/health")
                                .header(
                                        "X-Request-Id",
                                        "test-request-123"
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        header()
                                .string(
                                        "X-Request-Id",
                                        "test-request-123"
                                )
                );
    }
}
