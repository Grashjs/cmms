package com.grash.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailConfigurationTest {

    private final EmailConfiguration emailConfiguration = new EmailConfiguration();

    @Test
    void emailTemplateResolver_configuredForHtmlTemplates() {
        ITemplateResolver resolver = emailConfiguration.emailTemplateResolver();

        ClassLoaderTemplateResolver templateResolver = (ClassLoaderTemplateResolver) resolver;
        assertEquals("templates/", templateResolver.getPrefix());
        assertEquals(".html", templateResolver.getSuffix());
    }

    @Test
    void emailMessageSource_configuredWithMailMessagesBundle() {
        ResourceBundleMessageSource messageSource = emailConfiguration.emailMessageSource();

        assertTrue(messageSource.getBasenameSet().contains("mailMessages"));
        assertEquals("UTF-8", ReflectionTestUtils.getField(messageSource, "defaultEncoding"));
    }

    @Test
    void thymeleafTemplateEngine_usesProvidedResolver() {
        ITemplateResolver resolver = emailConfiguration.emailTemplateResolver();

        SpringTemplateEngine templateEngine = emailConfiguration.thymeleafTemplateEngine(resolver);

        assertNotNull(templateEngine);
        assertTrue(templateEngine.getTemplateResolvers().contains(resolver));
    }
}
