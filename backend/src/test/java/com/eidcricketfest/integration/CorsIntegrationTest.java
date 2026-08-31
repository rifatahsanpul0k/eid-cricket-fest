package com.eidcricketfest.integration;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CorsIntegrationTest
        extends AbstractIntegrationTest {

    @Test
    void configuredFrontendOriginShouldBeAllowed()
            throws Exception {

        mockMvc.perform(
                        options(
                                "/api/v1/tournaments"
                        )
                                .header(
                                        "Origin",
                                        "http://localhost:3000"
                                )
                                .header(
                                        "Access-Control-Request-Method",
                                        "GET"
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        header().string(
                                "Access-Control-Allow-Origin",
                                "http://localhost:3000"
                        )
                )
                .andExpect(
                        header().exists(
                                "Access-Control-Allow-Methods"
                        )
                );
    }


    @Test
    void unknownOriginShouldNotBeAllowed()
            throws Exception {

        mockMvc.perform(
                        options(
                                "/api/v1/tournaments"
                        )
                                .header(
                                        "Origin",
                                        "https://evil.example"
                                )
                                .header(
                                        "Access-Control-Request-Method",
                                        "GET"
                                )
                )
                .andExpect(
                        status().isForbidden()
                )
                .andExpect(
                        header().doesNotExist(
                                "Access-Control-Allow-Origin"
                        )
                );
    }
}
