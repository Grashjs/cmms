package com.grash.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import org.junit.jupiter.api.Test;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiConfigTest {

    private final OpenApiConfig openApiConfig = new OpenApiConfig();

    @Test
    void webhookCustomiser_addsWebhooksAndSchemas() {
        GlobalOpenApiCustomizer customizer = openApiConfig.webhookCustomiser();
        OpenAPI openApi = new OpenAPI();

        customizer.customise(openApi);

        assertNotNull(openApi.getComponents());
        assertNotNull(openApi.getComponents().getSchemas());
        assertTrue(openApi.getComponents().getSchemas().containsKey("workOrderStatusChangePayload"));
        assertTrue(openApi.getComponents().getSchemas().containsKey("newWorkOrderPayload"));

        Map<String, PathItem> webhooks = openApi.getWebhooks();
        assertNotNull(webhooks);
        assertTrue(webhooks.containsKey("workOrderStatusChange"));
        assertTrue(webhooks.containsKey("workOrderDelete"));
        assertTrue(webhooks.containsKey("newVendor"));
    }

    @Test
    void webhookApi_groupsSubscriptionsAndWebhooks() {
        GroupedOpenApi groupedOpenApi = openApiConfig.webhookApi();

        assertEquals("Subscriptions & Webhooks", groupedOpenApi.getGroup());
    }
}
