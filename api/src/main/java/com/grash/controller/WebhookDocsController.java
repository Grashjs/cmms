package com.grash.controller;

import com.grash.dto.webhook.WebhookEventInfo;
import com.grash.model.enums.Status;
import com.grash.model.enums.webhook.WebhookEvent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/webhook-docs")
@Tag(name = "Webhook Documentation", description = "Documentation and examples for Atlas CMMS webhooks")
@RequiredArgsConstructor
public class WebhookDocsController {

    @GetMapping("/guide")
    @Operation(summary = "Get webhook guide", description = "Comprehensive guide explaining what webhooks are and how" +
            " to use them in Atlas CMMS")
    public ResponseEntity<Map<String, Object>> getGuide() {
        Map<String, Object> guide = new LinkedHashMap<>();
        guide.put("title", "Webhooks in Atlas CMMS");
        guide.put("description", """
                Webhooks are HTTP callbacks that allow different systems to communicate with each other in real-time. They're like automated messengers that deliver information when something happens, rather than requiring you to ask for it.
                
                In the context of Atlas CMMS, webhooks are a way for our system to automatically notify your application when specific events occur in your account. Instead of your application repeatedly checking our API for updates (a process known as "polling"), webhooks allow you to receive real-time notifications about important events.
                
                When an event occurs, Atlas CMMS sends an HTTP POST request to the endpoint you specify. The request contains details about the event, allowing your application to react immediately to changes.""");
        guide.put("setup", Map.of(
                "steps", List.of(
                        "Navigate to Settings > Integrations > Webhooks in the Atlas CMMS web application",
                        "Click + New Webhook",
                        "Configure a webhook endpoint URL and select the events you want to receive",
                        "Save the configuration and securely store the webhook secret"
                ),
                "apiAlternative", "You can also configure webhooks programmatically using our REST API endpoints for " +
                        "webhook endpoints."
        ));
        guide.put("security", Map.of(
                "description", "To verify that webhook requests come from Atlas CMMS and haven't been tampered with, " +
                        "we include security signatures in each webhook request.",
                "headers", Map.of(
                        "X-Webhook-Signature", "HMAC-SHA256 signature of the payload",
                        "X-Webhook-Timestamp", "Unix timestamp of when the webhook was sent (prefixed with t=)",
                        "X-Webhook-Id", "Unique identifier for the webhook delivery attempt",
                        "X-Webhook-Event", "The event type (e.g., WORK_ORDER_STATUS_CHANGE)"
                ),
                "verificationSteps", List.of(
                        "Extract the timestamp and signature from the headers",
                        "Compute HMAC-SHA256 using your webhook secret and the payload",
                        "Compare the computed signature with the one in the X-Webhook-Signature header",
                        "Verify the timestamp is within your tolerance window (recommended: 5 minutes)"
                )
        ));
        guide.put("bestPractices", List.of(
                "Always verify webhook signatures to ensure requests are genuine",
                "Return a 2xx status code as quickly as possible to acknowledge receipt",
                "Process webhook events asynchronously after acknowledging receipt",
                "Implement proper error handling for failed webhook deliveries",
                "Implement idempotency in your webhook handler to prevent duplicate processing",
                "Set up monitoring for your webhook endpoint to detect failures",
                "Use the webhook testing tools during development to simulate events"
        ));
        guide.put("limitations", Map.of(
                "timeout", "Webhook calls will timeout after 10 seconds",
                "retryPolicy", "Failed webhooks may be retried. Webhooks that consistently fail may be disabled.",
                "maxUrlLength", "Webhook URLs are limited to a maximum of 512 characters"
        ));
        return ResponseEntity.ok(guide);
    }

    @GetMapping("/events")
    @Operation(summary = "Get webhook event types", description = "Returns all available webhook event types with " +
            "descriptions and example payloads")
    public ResponseEntity<List<WebhookEventInfo>> getEventTypes() {
        List<WebhookEventInfo> events = List.of(
                new WebhookEventInfo(
                        WebhookEvent.WORK_ORDER_STATUS_CHANGE,
                        "Triggered when a work order status changes (e.g., OPEN → IN_PROGRESS, IN_PROGRESS → COMPLETE)",
                        Map.of(
                                "workOrderId", 12345,
                                "workOrderTitle", "HVAC Maintenance",
                                "previousStatus", Status.OPEN,
                                "newStatus", Status.IN_PROGRESS,
                                "occurredAt", "2024-01-15T10:30:00Z",
                                "companyId", 1001,
                                "changedWorkOrder", "{... WorkOrderShowDTO ...}"
                        ),
                        List.of("STATUS")
                ),
                new WebhookEventInfo(
                        WebhookEvent.WORK_REQUEST_STATUS_CHANGE,
                        "Triggered when a work request is approved or cancelled",
                        Map.of(
                                "requestId", 67890,
                                "requestTitle", "Light bulb replacement request",
                                "previousStatus", "PENDING",
                                "newStatus", "APPROVED",
                                "workOrderId", 12345,
                                "occurredAt", "2024-01-15T10:30:00Z",
                                "companyId", 1001,
                                "changedRequest", "{... RequestShowDTO ...}"
                        ),
                        null
                ),
                new WebhookEventInfo(
                        WebhookEvent.PART_QUANTITY_CHANGED,
                        "Triggered when a part's quantity changes (through consumption, purchase order, or direct " +
                                "update)",
                        Map.of(
                                "partId", 111,
                                "partName", "Air Filter",
                                "previousQuantity", 50.0,
                                "newQuantity", 45.0,
                                "changedAmount", -5.0,
                                "workOrderId", 12345,
                                "workOrderTitle", "HVAC Maintenance",
                                "occurredAt", "2024-01-15T10:30:00Z",
                                "companyId", 1001,
                                "changedPartQuantity", "{... PartShowDTO ...}"
                        ),
                        List.of("PartField.QUANTITY")
                ),
                new WebhookEventInfo(
                        WebhookEvent.PART_CHANGE,
                        "Triggered when a part is updated",
                        Map.of(
                                "partId", 111,
                                "occurredAt", "2024-01-15T10:30:00Z",
                                "companyId", 1001,
                                "changedPart", "{... PartShowDTO ...}"
                        ),
                        List.of("PartField.NAME", "PartField.COST", "PartField.QUANTITY", "PartField.CATEGORY", "etc.")
                ),
                new WebhookEventInfo(
                        WebhookEvent.ASSET_STATUS_CHANGE,
                        "Triggered when an asset's status changes (e.g., OPERATIONAL → DOWN)",
                        Map.of(
                                "assetId", 222,
                                "assetName", "Conveyor Belt A",
                                "previousStatus", "OPERATIONAL",
                                "newStatus", "DOWN",
                                "occurredAt", "2024-01-15T10:30:00Z",
                                "companyId", 1001,
                                "changedAsset", "{... AssetShowDTO ...}"
                        ),
                        List.of("AssetStatus.OPERATIONAL", "AssetStatus.DOWN", "etc.")
                ),
                new WebhookEventInfo(
                        WebhookEvent.METER_TRIGGER_STATUS_CHANGE,
                        "Triggered when a meter reading triggers a work order based on threshold conditions",
                        Map.ofEntries(
                                Map.entry("meterName", "Temperature Gauge"),
                                Map.entry("meterId", 333),
                                Map.entry("meterTriggerId", 444),
                                Map.entry("meterTriggerName", "High Temperature Alert"),
                                Map.entry("readingValue", 85),
                                Map.entry("triggerValue", 80),
                                Map.entry("triggerCondition", "MORE_THAN"),
                                Map.entry("workOrderId", 12345),
                                Map.entry("occurredAt", "2024-01-15T10:30:00Z"),
                                Map.entry("companyId", 1001),
                                Map.entry("triggeredWorkOrder", "{... WorkOrderShowDTO ...}")
                        ),
                        null
                ),
                new WebhookEventInfo(
                        WebhookEvent.NEW_CATEGORY_ON_WORK_ORDER,
                        "Triggered when a category is added or changed on a work order",
                        Map.of(
                                "workOrderId", 12345,
                                "workOrderTitle", "HVAC Maintenance",
                                "previousCategoryId", null,
                                "newCategoryId", 555,
                                "newCategoryName", "Preventive Maintenance",
                                "occurredAt", "2024-01-15T10:30:00Z",
                                "companyId", 1001,
                                "changedWorkOrder", "{... WorkOrderShowDTO ...}"
                        ),
                        List.of("CATEGORY")
                ),
                new WebhookEventInfo(
                        WebhookEvent.NEW_WORK_ORDER,
                        "Triggered when a new work order is created",
                        Map.of(
                                "workOrderId", 12345,
                                "occurredAt", "2024-01-15T10:30:00Z",
                                "companyId", 1001,
                                "newWorkOrder", "{... WorkOrderShowDTO ...}"
                        ),
                        null
                ),
                new WebhookEventInfo(
                        WebhookEvent.WORK_ORDER_CHANGE,
                        "Triggered when a work order is updated",
                        Map.of(
                                "workOrderId", 12345,
                                "workOrderTitle", "HVAC Maintenance",
                                "occurredAt", "2024-01-15T10:30:00Z",
                                "companyId", 1001,
                                "changedWorkOrder", "{... WorkOrderShowDTO ...}"
                        ),
                        List.of("ASSET", "ASSIGNEES", "CATEGORY", "DESCRIPTION", "etc.")
                ),
                new WebhookEventInfo(
                        WebhookEvent.WORK_ORDER_DELETE,
                        "Triggered when a work order is deleted",
                        Map.of(
                                "workOrderId", 12345,
                                "workOrderTitle", "HVAC Maintenance",
                                "occurredAt", "2024-01-15T10:30:00Z",
                                "companyId", 1001,
                                "deleteWorkOrder", "{... WorkOrderShowDTO ...}"
                        ),
                        null
                ),
                new WebhookEventInfo(
                        WebhookEvent.NEW_ASSET,
                        "Triggered when a new asset is created",
                        Map.of(
                                "assetId", 222,
                                "occurredAt", "2024-01-15T10:30:00Z",
                                "companyId", 1001,
                                "newAsset", "{... AssetShowDTO ...}"
                        ),
                        null
                ),
                new WebhookEventInfo(
                        WebhookEvent.NEW_PART,
                        "Triggered when a new part is created",
                        Map.of(
                                "partId", 111,
                                "occurredAt", "2024-01-15T10:30:00Z",
                                "companyId", 1001,
                                "newPart", "{... PartShowDTO ...}"
                        ),
                        null
                ),
                new WebhookEventInfo(
                        WebhookEvent.PART_DELETE,
                        "Triggered when a part is deleted",
                        Map.of(
                                "partId", 111,
                                "occurredAt", "2024-01-15T10:30:00Z",
                                "companyId", 1001,
                                "deletePart", "{... PartShowDTO ...}"
                        ),
                        null
                ),
                new WebhookEventInfo(
                        WebhookEvent.NEW_REQUEST,
                        "Triggered when a new work request is created",
                        Map.of(
                                "requestId", 67890,
                                "occurredAt", "2024-01-15T10:30:00Z",
                                "companyId", 1001,
                                "newRequest", "{... RequestShowDTO ...}"
                        ),
                        null
                ),
                new WebhookEventInfo(
                        WebhookEvent.NEW_LOCATION,
                        "Triggered when a new location is created",
                        Map.of(
                                "locationId", 777,
                                "occurredAt", "2024-01-15T10:30:00Z",
                                "companyId", 1001,
                                "newLocation", "{... LocationShowDTO ...}"
                        ),
                        null
                ),
                new WebhookEventInfo(
                        WebhookEvent.NEW_VENDOR,
                        "Triggered when a new vendor is created",
                        Map.of(
                                "vendorId", 888,
                                "occurredAt", "2024-01-15T10:30:00Z",
                                "companyId", 1001,
                                "newVendor", "{... Vendor ...}"
                        ),
                        null
                )
        );
        return ResponseEntity.ok(events);
    }

    @GetMapping("/security/example")
    @Operation(summary = "Get webhook security verification example", description = "Code examples showing how to " +
            "verify webhook signatures")
    public ResponseEntity<Map<String, Object>> getSecurityExample() {
        Map<String, Object> example = new LinkedHashMap<>();
        example.put("headers", Map.of(
                "X-Webhook-Signature", "t=1705312200,v1=a1b2c3d4e5f6...",
                "X-Webhook-Timestamp", "1705312200",
                "X-Webhook-Id", "550e8400-e29b-41d4-a716-446655440000",
                "X-Webhook-Event", "WORK_ORDER_STATUS_CHANGE"
        ));
        example.put("verificationSteps", List.of(
                "1. Extract timestamp and signature from X-Webhook-Signature header",
                "2. Split by comma to get elements: t=1705312200,v1=a1b2c3d4e5f6...",
                "3. Split each element by equals to get timestamp and signature",
                "4. Prepare signed payload: timestamp + '.' + request body",
                "5. Compute HMAC-SHA256 using your webhook secret as key",
                "6. Compare computed signature with the one in the header",
                "7. Verify timestamp is within tolerance (recommended: 5 minutes)"
        ));
        example.put("javaExample", """
                import javax.crypto.Mac;
                import javax.crypto.spec.SecretKeySpec;
                
                public boolean verifyWebhook(String payload, String signatureHeader, String secret) {
                    // Parse header: t=<timestamp>,v1=<signature>
                    String[] parts = signatureHeader.split(",");
                    String timestamp = parts[0].split("=")[1];
                    String receivedSignature = parts[1].split("=")[1];
                
                    // Prepare signed payload
                    String signedPayload = timestamp + "." + payload;
                
                    // Compute HMAC-SHA256
                    Mac mac = Mac.getInstance("HmacSHA256");
                    SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
                    mac.init(keySpec);
                    byte[] hash = mac.doFinal(signedPayload.getBytes());
                    String expectedSignature = HexFormat.of().formatHex(hash);
                
                    // Compare signatures (use constant-time comparison in production)
                    boolean signatureMatch = expectedSignature.equals(receivedSignature);
                
                    // Check timestamp tolerance (5 minutes = 300000ms)
                    long now = System.currentTimeMillis() / 1000;
                    long timestampDiff = Math.abs(now - Long.parseLong(timestamp));
                    boolean timestampValid = timestampDiff <= 300;
                
                    return signatureMatch && timestampValid;
                }
                """);
        example.put("nodeExample", """
                const crypto = require('crypto');
                
                function verifyWebhook(payload, signatureHeader, secret) {
                    const timestamp = signatureHeader.match(/t=([^,]+)/)?.[1];
                    const receivedSignature = signatureHeader.match(/v1=([^,]+)/)?.[1];
                
                    const signedPayload = `${timestamp}.${payload}`;
                    const expectedSignature = crypto
                        .createHmac('sha256', secret)
                        .update(signedPayload, 'utf8')
                        .digest('hex');
                
                    const signatureMatch = crypto.timingSafeEqual(
                        Buffer.from(expectedSignature),
                        Buffer.from(receivedSignature)
                    );
                
                    const timestampDiff = Math.abs(Date.now() / 1000 - parseInt(timestamp));
                    const timestampValid = timestampDiff <= 300;
                
                    return signatureMatch && timestampValid;
                }
                """);
        example.put("bestPractices", List.of(
                "Use constant-time string comparison for signature verification to prevent timing attacks",
                "Reject webhooks with timestamps outside your tolerance window to prevent replay attacks",
                "Store webhook secrets securely (e.g., environment variables, secret managers)",
                "Rotate secrets periodically using the /webhook-endpoints/{id}/rotate-secret endpoint"
        ));
        return ResponseEntity.ok(example);
    }
}
