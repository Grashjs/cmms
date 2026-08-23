package com.grash.configuration;

import org.junit.jupiter.api.Test;
import org.springdoc.core.models.GroupedOpenApi;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SwaggerConfigTest {

    private final SwaggerConfig swaggerConfig = new SwaggerConfig();

    @Test
    void publicApi_hasExpectedGroup() {
        GroupedOpenApi groupedOpenApi = swaggerConfig.publicApi();

        assertEquals("atlas-cmms", groupedOpenApi.getGroup());
    }
}
