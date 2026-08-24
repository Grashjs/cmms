package com.grash.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false)
class ApiDocsIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void atlasCmmsGroup_returnsOpenApiSpec() throws Exception {
        mockMvc.perform(get("/v3/api-docs/atlas-cmms"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.openapi", startsWith("3.")))
                .andExpect(jsonPath("$.info.title").value("Atlas CMMS API"))
                .andExpect(jsonPath("$.servers[*].url", hasItem("https://api.atlas-cmms.com")))
                .andExpect(jsonPath("$.paths.length()", greaterThan(0)))
                .andExpect(jsonPath("$.paths['/subscriptions/upgrade'].post").exists())
                .andExpect(jsonPath("$.components.securitySchemes.apiKey.type").value("apiKey"))
                .andExpect(jsonPath("$.components.securitySchemes.apiKey.in").value("header"))
                .andExpect(jsonPath("$.components.securitySchemes.apiKey.name").value("x-api-key"));
    }

    @Test
    void atlasCmmsGroup_includesWebhookDocumentation() throws Exception {
        mockMvc.perform(get("/v3/api-docs/atlas-cmms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.webhooks.workOrderStatusChange.post.summary")
                        .value("Work Order Status Change"))
                .andExpect(jsonPath("$.components.schemas.workOrderStatusChangePayload").exists());
    }

    @Test
    void unknownGroup_isRejected() throws Exception {
        mockMvc.perform(get("/v3/api-docs/does-not-exist"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void defaultUngroupedDocs_areDisabledAsInProduction() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isNotFound());
    }
}
