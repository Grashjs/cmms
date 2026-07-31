package com.grash.template;

import com.grash.dto.BrandConfig;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainLayoutConsumerTemplatesTest extends AbstractTemplateTest {

    private static final String TEMPLATE = "signup.html";
    private static final BrandConfig BRAND = BrandConfig.builder().name("Atlas CMMS").shortName("Atlas").build();

    @Override
    protected String templateName() {
        return TEMPLATE;
    }

    @Test
    void rendersApprovedRequest() {
        String html = renderEmail("approved-request.html", vars(
                "workOrderTitle", "Replace conveyor belt",
                "workOrderLink", "https://app.example.com/requests/1"));

        assertRenderedEmail(html,
                "New Work order", "New Work order",
                "The request \"Replace conveyor belt\" just got approved",
                "See more details", "https://app.example.com/requests/1");
    }

    @Test
    void rendersDeletedWorkOrder() {
        String html = renderEmail("deleted-work-order.html", vars(
                "workOrderTitle", "Replace conveyor belt",
                "deleter", "John Doe",
                "workOrdersLink", "https://app.example.com/work-orders"));

        assertRenderedEmail(html,
                "Deleted Work Order", "Deleted Work Order",
                "John Doe just deleted Work Order \"Replace conveyor belt\"",
                "See more details", "https://app.example.com/work-orders");
    }

    @Test
    void rendersCommentUpdated() {
        String html = renderEmail("comment-updated.html", vars(
                "userFullName", "John Doe",
                "workOrderTitle", "Replace conveyor belt",
                "commentContent", "Updated the estimate",
                "commentLink", "https://app.example.com/comments/1"));

        assertRenderedEmail(html,
                "Comment Updated", "Comment updated on work order",
                "John Doe updated a comment on \"Replace conveyor belt\": Updated the estimate",
                "See more details", "https://app.example.com/comments/1");
    }

    @Test
    void rendersInvite() {
        String html = renderEmail("invite.html", vars(
                "inviter", "Jane Smith",
                "company", "Acme Corp",
                "joinLink", "https://app.example.com/join/abc"));

        assertRenderedEmail(html,
                "Invitation to use Atlas CMMS.", "You have been invited to use Atlas CMMS.",
                "Jane Smith has invited you to join the Acme Corp Company on Atlas.",
                "Join Company", "https://app.example.com/join/abc");
    }

    @Test
    void rendersLowStock() {
        String html = renderEmail("low-stock.html", vars(
                "partName", "Conveyor Belt",
                "partLink", "https://app.example.com/parts/1"));

        assertRenderedEmail(html,
                "Low Stock Alert", "Part is running low",
                "The stock of Conveyor Belt is getting low",
                "See more details", "https://app.example.com/parts/1");
    }

    @Test
    void rendersComingWorkOrder() {
        String html = renderEmail("coming-work-order.html", vars(
                "pmTitle", "Monthly PM",
                "pmLink", "https://app.example.com/work-orders/2"));

        assertRenderedEmail(html,
                "Coming Work Order", "Coming work order assigned to you",
                "The work order \"Monthly PM\", assigned to you is forthcoming",
                "See more details", "https://app.example.com/work-orders/2");
    }

    @Test
    void rendersNewPurchaseOrder() {
        String html = renderEmail("new-purchase-order.html", vars(
                "message", "A new PO was created for <b>Acme Corp</b>.",
                "purchaseOrderLink", "https://app.example.com/purchase-orders/1"));

        assertRenderedEmail(html,
                "New Purchase Order", "New Purchase Order",
                "<b>Acme Corp</b>",
                "See more details", "https://app.example.com/purchase-orders/1");
    }

    @Test
    void rendersNewComment() {
        String html = renderEmail("new-comment.html", vars(
                "userFullName", "John Doe",
                "workOrderTitle", "Replace conveyor belt",
                "commentContent", "Please confirm parts",
                "commentLink", "https://app.example.com/comments/2"));

        assertRenderedEmail(html,
                "New Comment", "New comment on work order",
                "John Doe left a comment on \"Replace conveyor belt\": Please confirm parts",
                "See more details", "https://app.example.com/comments/2");
    }

    @Test
    void rendersRejectedRequest() {
        String html = renderEmail("rejected-request.html", vars(
                "requestTitle", "Paint the walls",
                "requestLink", "https://app.example.com/requests/3"));

        assertRenderedEmail(html,
                "Rejected work request", "Rejected work request",
                "The request \"Paint the walls\" just got rejected",
                "See more details", "https://app.example.com/requests/3");
    }

    @Test
    void rendersRequesterUpdate() {
        String html = renderEmail("requester-update.html", vars(
                "message", "Your request was <b>updated</b>.",
                "workOrderLink", "https://app.example.com/work-orders/4"));

        assertRenderedEmail(html,
                "Request Update", "A work order you requested has been updated",
                "<b>updated</b>",
                "See more details", "https://app.example.com/work-orders/4");
    }

    @Test
    void rendersNewRequest() {
        String html = renderEmail("new-request.html", vars(
                "requester", "Alice Johnson",
                "requestTitle", "Paint the walls",
                "requestLink", "https://app.example.com/requests/5"));

        assertRenderedEmail(html,
                "New Work request", "New work request",
                "Alice Johnson created a new Request: Paint the walls",
                "See more details", "https://app.example.com/requests/5");
    }

    @Test
    void rendersResetPassword() {
        String html = renderEmail("reset-password.html", vars(
                "password", "TempPass123",
                "resetConfirmLink", "https://app.example.com/reset/xyz"));

        assertRenderedEmail(html,
                "New Atlas Password", "New Atlas Password",
                null,
                "Confirm reset", "https://app.example.com/reset/xyz");

        assertTrue(html.contains("TempPass123"));
        assertTrue(html.contains("Please make sure to change this password as soon as possible"));
    }

    @Test
    void rendersSignup() {
        String html = renderEmail("signup.html", vars(
                "verifyTokenLink", "https://app.example.com/verify/abc"));

        assertRenderedEmail(html,
                "Welcome,", "We are delighted to have you here! Prepare to dive into your new account.",
                "We are happy to welcome you. Please first check your account. Press the button below.",
                "Verify Account", "https://app.example.com/verify/abc");
    }

    @Test
    void rendersWorkOrderReportEmail_withoutButton() {
        String html = renderEmail("work-order-report-email.html", vars(
                "messageBody", "A work order report is attached: <b>WO-0001</b>"));

        assertRenderedEmail(html,
                "Work Order Report", "A work order has been shared with you.",
                "<b>WO-0001</b>",
                null, null);

        assertFalse(html.contains("target=\"_blank\""));
    }

    @Test
    void rendersInvite_withFrenchMessages() {
        Map<String, Object> vars = context(vars(
                "inviter", "Jane Smith",
                "company", "Acme Corp",
                "joinLink", "https://app.example.com/join/abc"));

        String html = render("invite.html", vars, Locale.FRANCE);

        assertTrue(html.contains("Invitation à utiliser Atlas CMMS."));
        assertTrue(html.contains("Vous avez été invité à utiliser le Atlas CMMS."));
        assertTrue(html.contains("Jane Smith vous a invité à joindre Acme Corp sur Atlas."));
        assertTrue(html.contains("Rejoindre"));
        assertTrue(html.contains("href=\"https://app.example.com/join/abc\""));
        assertTrue(html.contains("équipe Atlas"));
    }

    @Test
    void rendersNewComment_withFrenchMessages() {
        Map<String, Object> vars = context(vars(
                "userFullName", "John Doe",
                "workOrderTitle", "Replace conveyor belt",
                "commentContent", "Please confirm parts",
                "commentLink", "https://app.example.com/comments/2"));

        String html = render("new-comment.html", vars, Locale.FRANCE);

        assertTrue(html.contains("Nouveau Commentaire"));
        assertTrue(html.contains("Nouveau commentaire sur le bon de travail"));
        assertTrue(html.contains("John Doe a laissé un commentaire sur \"Replace conveyor belt\": Please confirm " +
                "parts"));
        assertTrue(html.contains("Voir plus de détails"));
        assertTrue(html.contains("équipe Atlas"));
    }

    private String renderEmail(String template, Map<String, Object> extraVars) {
        return render(template, context(extraVars), Locale.ENGLISH);
    }

    private Map<String, Object> context(Map<String, Object> extraVars) {
        Map<String, Object> all = new HashMap<>();
        all.put("brandConfig", BRAND);
        all.put("backgroundColor", "#2563EB");
        all.put("environment", environment());
        all.putAll(extraVars);
        return all;
    }

    private void assertRenderedEmail(String html, String title, String preheader, String message,
                                     String buttonText, String link) {
        assertTrue(html.contains(title));
        assertTrue(html.contains(preheader));
        if (message != null) {
            assertTrue(html.contains(message));
        }
        if (buttonText != null) {
            assertTrue(html.contains(buttonText));
        }
        if (link != null) {
            assertTrue(html.contains("href=\"" + link + "\""));
        }
        assertTrue(html.contains("https://api.example.com/images/logo.png"));
        assertTrue(html.contains("Atlas Team"));
    }

    private Map<String, Object> vars(Object... keyValues) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return map;
    }
}
