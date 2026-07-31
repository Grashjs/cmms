package com.grash.template;

import com.grash.dto.BrandConfig;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainLayoutTemplateTest extends AbstractTemplateTest {

    private static final String TEMPLATE = "fragments/main-layout.html";
    private static final String NEW_WORK_ORDER_TEMPLATE = "new-work-order.html";

    @Override
    protected String templateName() {
        return TEMPLATE;
    }

    @Test
    void rendersHeaderPreheaderMessageAndSignature() {
        variables = emailContext("Annual Maintenance", "Discount for long-term partners",
                "Your machinery is due for inspection.", null, null);

        String html = render(variables, Locale.ENGLISH);

        assertTrue(html.contains("Annual Maintenance"));
        assertTrue(html.contains("Discount for long-term partners"));
        assertTrue(html.contains("Your machinery is due for inspection."));
        assertTrue(html.contains("Atlas Team"));
        assertTrue(html.contains("display: none"));
    }

    @Test
    void rendersMessageContent_asRawHtml() {
        variables = emailContext("Invitation", "preheader", "Visit the <b>dashboard</b> today.", null, null);

        String html = render(variables, Locale.ENGLISH);

        assertTrue(html.contains("Visit the <b>dashboard</b> today."));
        assertFalse(html.contains("&lt;b&gt;"));
    }

    @Test
    void rendersButton_whenButtonTextAndLinkProvided() {
        variables = emailContext("New Work order", "preheader", "You have a new job.",
                "See more details", "https://app.example.com/work-orders/1");

        String html = render(variables, Locale.ENGLISH);

        assertTrue(html.contains("See more details"));
        assertTrue(html.contains("href=\"https://app.example.com/work-orders/1\""));
        assertTrue(html.contains("target=\"_blank\""));
    }

    @Test
    void hidesButton_whenEitherParamMissing() {
        variables = emailContext("New Work order", "preheader", "No action required.",
                "Continue", null);

        String html = render(variables, Locale.ENGLISH);

        assertFalse(html.contains("Continue"));
        assertFalse(html.contains("href=\"https://app.example.com/work-orders/1\""));
        assertFalse(html.contains("target=\"_blank\""));
    }

    @Test
    void rendersDefaultLogo_fromApiHost() {
        variables = emailContext("New Work order", "preheader", "body", null, null);

        String html = render(variables, Locale.ENGLISH);

        assertTrue(html.contains("https://api.example.com/images/logo.png"));
    }

    @Test
    void rendersCustomLogo_whenWhiteLabelingConfigured() {
        variables = emailContext("New Work order", "preheader", "body", null, null);
        StandardEnvironment environment = environment();
        environment.getPropertySources().addFirst(new MapPropertySource("whiteLabeling",
                Collections.singletonMap("white-labeling.logo-paths", "/images/custom.png")));
        variables.put("environment", environment);

        String html = render(variables, Locale.ENGLISH);

        assertTrue(html.contains("https://api.example.com/images/custom-logo.png"));
        assertFalse(html.contains("/images/logo.png"));
    }

    @Test
    void rendersConsumerTemplate_withEnglishMessages() {
        variables = emailContext(null, null, null, null, null);
        variables.put("workOrderTitle", "Replace conveyor belt");
        variables.put("workOrderLink", "https://app.example.com/work-orders/1");

        String html = render(NEW_WORK_ORDER_TEMPLATE, variables, Locale.ENGLISH);

        assertTrue(html.contains("New Work order"));
        assertTrue(html.contains("New work order assigned to you"));
        assertTrue(html.contains("You have been assigned this Work Order: Replace conveyor belt"));
        assertTrue(html.contains("See more details"));
        assertTrue(html.contains("Atlas Team"));
        assertTrue(html.contains("https://app.example.com/work-orders/1"));
    }

    @Test
    void rendersConsumerTemplate_withFrenchMessages() {
        variables = emailContext(null, null, null, null, null);
        variables.put("workOrderTitle", "Remplacer le tapis");
        variables.put("workOrderLink", "https://app.example.com/work-orders/1");

        String html = render(NEW_WORK_ORDER_TEMPLATE, variables, Locale.FRANCE);

        assertTrue(html.contains("Nouvel ordre de travail"));
        assertTrue(html.contains("Nouveau bon de travail qui vous est assigné"));
        assertTrue(html.contains("Ce bon de travail vous a été assigné: Remplacer le tapis"));
        assertTrue(html.contains("Voir plus de détails"));
        assertTrue(html.contains("équipe Atlas"));
    }

    private Map<String, Object> emailContext(String headerText, String preheaderText, String messageText,
                                             String buttonText, String buttonLink) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("headerText", headerText);
        vars.put("preheaderText", preheaderText);
        vars.put("messageText", messageText);
        vars.put("buttonText", buttonText);
        vars.put("buttonLink", buttonLink);
        vars.put("brandConfig", BrandConfig.builder().shortName("Atlas").build());
        vars.put("backgroundColor", "#2563EB");
        vars.put("environment", environment());
        return vars;
    }
}
