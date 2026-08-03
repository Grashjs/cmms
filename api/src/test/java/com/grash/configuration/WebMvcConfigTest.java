package com.grash.configuration;

import com.grash.security.CurrentUserResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class WebMvcConfigTest {

    private static class ExposedCorsRegistry extends CorsRegistry {
        Map<String, CorsConfiguration> getConfigurations() {
            return getCorsConfigurations();
        }
    }

    @Mock
    private CurrentUserResolver currentUserResolver;

    private WebMvcConfig webMvcConfig;

    @BeforeEach
    void setUp() {
        webMvcConfig = new WebMvcConfig(currentUserResolver);
        ReflectionTestUtils.setField(webMvcConfig, "frontendUrl", "http://localhost:3000");
        ReflectionTestUtils.setField(webMvcConfig, "frontendHomeUrl", "http://localhost:3001");
        ReflectionTestUtils.setField(webMvcConfig, "enableCors", true);
    }

    @Test
    void addCorsMappings_whenEnabled_configuresAllowedOrigins() {
        ExposedCorsRegistry registry = new ExposedCorsRegistry();

        webMvcConfig.addCorsMappings(registry);

        CorsConfiguration configuration = registry.getConfigurations().get("/**");
        assertEquals(List.of("http://localhost:3000", "http://localhost:3001"), configuration.getAllowedOrigins());
        assertTrue(configuration.getAllowedMethods().contains("GET"));
        assertTrue(configuration.getAllowedMethods().contains("DELETE"));
        assertTrue(configuration.getAllowCredentials());
        assertEquals(3600, configuration.getMaxAge());
    }

    @Test
    void addCorsMappings_whenDisabled_allowsAllMethods() {
        ReflectionTestUtils.setField(webMvcConfig, "enableCors", false);
        ExposedCorsRegistry registry = new ExposedCorsRegistry();

        webMvcConfig.addCorsMappings(registry);

        CorsConfiguration configuration = registry.getConfigurations().get("/**");
        assertTrue(configuration.getAllowedMethods().contains("*"));
    }

    @Test
    void addArgumentResolvers_addsCurrentUserResolver() {
        List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();

        webMvcConfig.addArgumentResolvers(resolvers);

        assertEquals(1, resolvers.size());
        assertSame(currentUserResolver, resolvers.get(0));
    }

    @Test
    void addResourceHandlers_mapsImagesPattern() {
        ResourceHandlerRegistry registry =
                new ResourceHandlerRegistry(mock(ApplicationContext.class), null);

        webMvcConfig.addResourceHandlers(registry);

        assertTrue(registry.hasMappingForPattern("/images/**"));
    }
}
