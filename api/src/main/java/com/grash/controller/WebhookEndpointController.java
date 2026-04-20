package com.grash.controller;

import com.grash.dto.SuccessResponse;
import com.grash.dto.webhookEndpoint.WebhookEndpointPatchDTO;
import com.grash.dto.webhookEndpoint.WebhookEndpointPostDTO;
import com.grash.dto.webhookEndpoint.WebhookEndpointShowDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.WebhookEndpointMapper;
import com.grash.model.User;
import com.grash.model.WebhookEndpoint;
import com.grash.security.CurrentUser;
import com.grash.service.WebhookEndpointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/webhook-endpoints")
@RequiredArgsConstructor
@Tag(name = "Webhook Subscriptions", description = """
        Manage webhook subscriptions to receive real-time notifications when events occur in Atlas CMMS.
        
        ## What are Webhooks?
        Webhooks are HTTP callbacks that allow Atlas CMMS to automatically notify your application when specific events occur,
        such as work order status changes, new assets, part updates, and more.
        
        ## How it Works
        1. Create a webhook subscription with your endpoint URL and the events you want to receive
        2. When an event occurs, Atlas CMMS sends an HTTP POST request to your endpoint
        3. Each request includes a signature you can use to verify the request came from Atlas CMMS
        4. Your application should return a 2xx status code to acknowledge receipt
        
        ## Security
        All webhook requests include an `X-Webhook-Signature` header containing an HMAC-SHA256 signature.
        Verify this signature using the webhook secret provided when you create the subscription.
        
        ## Full events list
        See the [endpoints](#webhook/undefined/) for comprehensive documentation and examples.
        """)
public class WebhookEndpointController {

    private final WebhookEndpointService webhookEndpointService;
    private final WebhookEndpointMapper webhookEndpointMapper;

    @PostMapping
    @Operation(
            summary = "Create a webhook subscription",
            description = """
                    Create a new webhook subscription to receive notifications for specific events.
                    
                    When creating a webhook, you'll receive a secret that should be securely stored on your end.
                    This secret is essential for signature verification and can also be accessed later.
                    
                    **Supported Events:**
                    - `WORK_ORDER_STATUS_CHANGE` - Work order status changes
                    - `WORK_REQUEST_STATUS_CHANGE` - Request approval/cancellation
                    - `PART_CHANGE` - Part updates
                    - `PART_QUANTITY_CHANGED` - Part quantity changes
                    - `ASSET_STATUS_CHANGE` - Asset status changes
                    - `METER_TRIGGER_STATUS_CHANGE` - Meter trigger activations
                    - `NEW_CATEGORY_ON_WORK_ORDER` - Category changes on work orders
                    - `NEW_WORK_ORDER`, `WORK_ORDER_CHANGE`, `WORK_ORDER_DELETE`
                    - `NEW_ASSET`, `NEW_PART`, `PART_DELETE`
                    - `NEW_REQUEST`, `NEW_LOCATION`, `NEW_VENDOR`
                    
                    **Filtering:**
                    You can filter events by specific fields (e.g., only trigger when WOField.STATUS changes).
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Webhook created successfully",
                    content = @Content(schema = @Schema(implementation = WebhookEndpointShowDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "403", description = "Access denied or webhook feature not available in your " +
                    "plan")
    })
    public ResponseEntity<WebhookEndpointShowDTO> create(
            @Parameter(hidden = true) @CurrentUser User user,
            @RequestBody WebhookEndpointPostDTO request) {
        WebhookEndpoint endpoint = webhookEndpointService.create(request, user);
        return ResponseEntity.ok(webhookEndpointMapper.toShowDto(endpoint));
    }

    @GetMapping
    @Operation(
            summary = "List webhook subscriptions",
            description = "Retrieve all webhook subscriptions for your company"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of webhook subscriptions",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation =
                            WebhookEndpointShowDTO.class))))
    })
    public ResponseEntity<List<WebhookEndpointShowDTO>> listEndpoints(
            @Parameter(hidden = true) @CurrentUser User user) {

        List<WebhookEndpoint> endpoints = webhookEndpointService
                .getActiveEndpointsByCompany(user.getCompany().getId());

        return ResponseEntity.ok(endpoints.stream()
                .map(webhookEndpointMapper::toShowDto)
                .toList());
    }

    @PatchMapping("/{id}")
    @Operation(
            summary = "Update a webhook subscription",
            description = "Update webhook configuration (URL, events, enabled status, etc.)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Webhook updated successfully",
                    content = @Content(schema = @Schema(implementation = WebhookEndpointShowDTO.class))),
            @ApiResponse(responseCode = "404", description = "Webhook not found")
    })
    public ResponseEntity<WebhookEndpointShowDTO> updateEndpoint(
            @Parameter(hidden = true) @CurrentUser User user,
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Webhook fields to update",
                    content = @Content(examples = {
                            @ExampleObject(
                                    name = "Disable Webhook",
                                    value = """
                                            {
                                              "enabled": false
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "Change Event Type",
                                    value = """
                                            {
                                              "event": "ASSET_STATUS_CHANGE"
                                            }
                                            """
                            )
                    })
            ) @RequestBody WebhookEndpointPatchDTO request) {

        WebhookEndpoint endpoint = webhookEndpointService.update(id, request, user);
        return ResponseEntity.ok(webhookEndpointMapper.toShowDto(endpoint));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete a webhook subscription",
            description = """
                    Delete a webhook subscription. The webhook will no longer receive notifications.
                    
                    ⚠️ **Warning:** This action cannot be undone. You'll need to create a new webhook subscription to receive notifications again.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Webhook deleted successfully",
                    content = @Content(schema = @Schema(implementation = SuccessResponse.class))),
            @ApiResponse(responseCode = "404", description = "Webhook not found")
    })
    public ResponseEntity<SuccessResponse> deleteEndpoint(
            @Parameter(hidden = true) @CurrentUser User user,
            @PathVariable Long id) {
        Optional<WebhookEndpoint> optionalWebhookEndpoint = webhookEndpointService.findById(id);
        if (optionalWebhookEndpoint.isPresent()) {
            webhookEndpointService.delete(id);
            return new ResponseEntity<>(new SuccessResponse(true, "Deleted successfully"),
                    HttpStatus.OK);
        } else throw new CustomException("Webhook endpoint not found", HttpStatus.NOT_FOUND);
    }

    @PatchMapping("/{id}/rotate-secret")
    @Operation(
            summary = "Rotate webhook secret",
            description = """
                    Generate a new signing secret for the webhook subscription.
                    
                    After rotating the secret, you must update your application to use the new secret
                    for signature verification. The old secret will no longer be valid.
                    
                    **Security Best Practices:**
                    - Store secrets securely (environment variables, secret managers)
                    - Rotate secrets periodically
                    - Never expose secrets in client-side code or logs
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Secret rotated successfully",
                    content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "message": "whsec_a1b2c3d4e5f6..."
                                    }
                                    """))),
            @ApiResponse(responseCode = "404", description = "Webhook not found")
    })
    public ResponseEntity<SuccessResponse> rotateSecret(
            @Parameter(hidden = true) @CurrentUser User user,
            @PathVariable Long id) {

        String newSecret = webhookEndpointService.rotateSecret(
                id,
                user.getCompany().getId()
        );

        return ResponseEntity.ok(new SuccessResponse(true, newSecret));
    }
}
