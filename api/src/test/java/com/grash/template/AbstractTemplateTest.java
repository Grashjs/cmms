package com.grash.template;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

abstract class AbstractTemplateTest {

    protected SpringTemplateEngine templateEngine;
    protected Map<String, Object> variables;

    @BeforeEach
    void setUpTemplateEngine() {
        templateEngine = createTemplateEngine();
    }

    protected abstract String templateName();

    protected String render(Map<String, Object> variables, Locale locale) {
        return render(templateName(), variables, locale);
    }

    protected String render(String template, Map<String, Object> variables, Locale locale) {
        Context context = new Context(locale);
        context.setVariables(variables);
        return templateEngine.process(template, context);
    }

    protected SpringTemplateEngine createTemplateEngine() {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode("HTML");
        templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setCacheable(false);

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(templateResolver);
        engine.setTemplateEngineMessageSource(messageSource());
        return engine;
    }

    protected ResourceBundleMessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("mailMessages");
        messageSource.setDefaultEncoding("UTF-8");
        return messageSource;
    }

    protected StandardEnvironment environment() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("api",
                Collections.singletonMap("api.host", "https://api.example.com")));
        return environment;
    }

    protected Date utc(int day, int hour, int minute) {
        return Date.from(LocalDateTime.of(2024, 1, day, hour, minute).atZone(ZoneId.of("UTC")).toInstant());
    }
}
