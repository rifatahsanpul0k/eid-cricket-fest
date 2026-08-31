package com.eidcricketfest.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ErrorContractIntegrationTest
        extends AbstractIntegrationTest {

    @Test
    void unauthenticatedRequestShouldUseProblemDetail()
            throws Exception {

        mockMvc.perform(
                        post(
                                "/api/v1/tournaments"
                        )
                                .header(
                                        "X-Request-Id",
                                        "security-test-123"
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "name": "Test"
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isUnauthorized()
                )
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_PROBLEM_JSON
                        )
                )
                .andExpect(
                        jsonPath("$.code")
                                .value("UNAUTHORIZED")
                )
                .andExpect(
                        jsonPath("$.requestId")
                                .value(
                                        "security-test-123"
                                )
                )
                .andExpect(
                        jsonPath("$.timestamp")
                                .exists()
                );
    }


    @Test
    void malformedJsonShouldUseStandardErrorContract()
            throws Exception {

        mockMvc.perform(
                        post(
                                "/api/v1/auth/register"
                        )
                                .header(
                                        "X-Request-Id",
                                        "json-test-123"
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "email":
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.code")
                                .value(
                                        "INVALID_REQUEST"
                                )
                )
                .andExpect(
                        jsonPath("$.requestId")
                                .value(
                                        "json-test-123"
                                )
                );
    }

    @Test
    void validationFailureShouldContainFieldErrors()
            throws Exception {

        mockMvc.perform(
                        post(
                                "/api/v1/auth/register"
                        )
                                .header(
                                        "X-Request-Id",
                                        "validation-test-123"
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "displayName": "",
                                          "email": "not-an-email",
                                          "password": ""
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.code")
                                .value(
                                        "VALIDATION_ERROR"
                                )
                )
                .andExpect(
                        jsonPath("$.requestId")
                                .value(
                                        "validation-test-123"
                                )
                )
                .andExpect(
                        jsonPath("$.errors")
                                .isArray()
                );
    }
}
