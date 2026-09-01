package com.eidcricketfest.integration;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PlayerCategoryIntegrationTest
        extends AbstractIntegrationTest {

    @Test
    void activePlayerCategoriesArePubliclyReadable()
            throws Exception {

        mockMvc.perform(
                        get("/api/v1/players/categories")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].code").value("BATSMAN"))
                .andExpect(jsonPath("$[0].name").value("Batsman"));
    }
}
