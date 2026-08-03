package com.grash.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class SwaggerSecurityConfigTest {

    private final SwaggerSecurityConfig swaggerSecurityConfig = new SwaggerSecurityConfig();

    @Test
    void swaggerSecurityFilterChain_buildsChain() throws Exception {
        ObjectPostProcessor<Object> objectPostProcessor = new ObjectPostProcessor<>() {
            @Override
            public <O> O postProcess(O object) {
                return object;
            }
        };
        AuthenticationManagerBuilder authBuilder = new AuthenticationManagerBuilder(objectPostProcessor);
        Map<Class<?>, Object> sharedObjects = new HashMap<>();
        sharedObjects.put(AuthenticationManagerBuilder.class, authBuilder);
        GenericWebApplicationContext context = new GenericWebApplicationContext();
        context.registerBean("mvcHandlerMappingIntrospector", HandlerMappingIntrospector.class);
        context.registerBean(ObjectPostProcessor.class, () -> objectPostProcessor);
        context.refresh();
        sharedObjects.put(ApplicationContext.class, context);
        HttpSecurity http = new HttpSecurity(objectPostProcessor, authBuilder, sharedObjects);

        SecurityFilterChain chain = swaggerSecurityConfig.swaggerSecurityFilterChain(http);

        assertNotNull(chain);
    }
}
