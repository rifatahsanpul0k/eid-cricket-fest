package com.eidcricketfest.integration;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OpenApiContractIntegrationTest
        extends AbstractIntegrationTest {

    private static final Set<String> HTTP_METHODS =
            Set.of(
                    "get",
                    "post",
                    "put",
                    "patch",
                    "delete"
            );

    @Test
    void openApiDocumentShouldExposeBackendV1Contract()
            throws Exception {

        JsonNode document =
                apiDocs();

        assertThat(document.path("openapi").asString())
                .isNotBlank();

        assertThat(document.path("info").path("title").asString())
                .isEqualTo("Eid Cricket Fest API");

        assertThat(document.path("info").path("version").asString())
                .isEqualTo("v1");

        JsonNode bearerAuth =
                document.path("components")
                        .path("securitySchemes")
                        .path("bearerAuth");

        assertThat(bearerAuth.path("type").asString())
                .isEqualTo("http");

        assertThat(bearerAuth.path("scheme").asString())
                .isEqualTo("bearer");

        assertThat(bearerAuth.path("bearerFormat").asString())
                .isEqualTo("JWT");

        JsonNode paths =
                document.path("paths");

        assertThat(paths.has("/api/v1/auth/login"))
                .isTrue();

        assertThat(paths.has("/api/v1/players"))
                .isTrue();

        assertThat(paths.has("/api/v1/players/categories"))
                .isTrue();

        assertThat(paths.has(
                "/api/v1/tournament-editions/{editionId}/matches"
        )).isTrue();

        assertThat(paths.has("/api/v1/innings/{inningsId}/deliveries"))
                .isTrue();

        assertThat(paths.has(
                "/api/v1/innings/{inningsId}/deliveries/undo"
        )).isTrue();

        assertThat(paths.has("/api/v1/matches/{matchId}/live"))
                .isTrue();

        assertThat(paths.has("/api/v1/matches/{matchId}/scorecard"))
                .isTrue();

        assertThat(paths.has(
                "/api/v1/tournament-editions/{editionId}/standings"
        )).isTrue();

        assertThat(hasBearerRequirement(
                paths.path("/api/v1/auth/login")
                        .path("post")
        )).isFalse();

        assertThat(hasBearerRequirement(
                paths.path("/api/v1/matches/{matchId}/live")
                        .path("get")
        )).isFalse();

        assertThat(hasBearerRequirement(
                paths.path("/api/v1/innings/{inningsId}/deliveries")
                        .path("post")
        )).isTrue();

        assertThat(hasBearerRequirement(
                paths.path(
                        "/api/v1/innings/{inningsId}/deliveries/undo"
                ).path("post")
        )).isTrue();
    }

    @Test
    void apiOperationsShouldHaveTagsAndSummaries()
            throws Exception {

        JsonNode paths =
                apiDocs().path("paths");

        for (var pathEntry : paths.properties()) {

            String path =
                    pathEntry.getKey();

            if (!path.startsWith("/api/v1/")) {
                continue;
            }

            for (var operationEntry
                    : pathEntry.getValue().properties()) {

                String method =
                        operationEntry.getKey();

                if (!HTTP_METHODS.contains(method)) {
                    continue;
                }

                JsonNode operation =
                        operationEntry.getValue();

                assertThat(operation.path("tags").size())
                        .as("%s %s tags", method, path)
                        .isGreaterThan(0);

                assertThat(operation.path("summary").asString())
                        .as("%s %s summary", method, path)
                        .isNotBlank();
            }
        }
    }

    private JsonNode apiDocs()
            throws Exception {

        String content =
                mockMvc.perform(get("/v3/api-docs"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        return jsonMapper.readTree(content);
    }

    private boolean hasBearerRequirement(
            JsonNode operation
    ) {

        JsonNode security =
                operation.path("security");

        if (security.isMissingNode()
                || security.isEmpty()) {
            return false;
        }

        for (JsonNode requirement : security) {
            if (requirement.has("bearerAuth")) {
                return true;
            }
        }

        return false;
    }
}
