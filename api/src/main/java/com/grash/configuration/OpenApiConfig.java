package com.grash.configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.ExternalDocumentation;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Atlas CMMS API",
                version = "v1",
                description = """
                         ## Getting Started
                        
                         Welcome to the **Atlas CMMS API** documentation. This RESTful API provides programmatic access to all features of the Atlas Computerized Maintenance Management System (CMMS).
                        
                         ### Base URL
                        
                         All API requests should be made to:
                        
                         ```
                         https://api.atlas-cmms.com
                         ```
                        
                         ---
                        
                         ## Authentication
                        
                         All API endpoints require authentication using an **API Key**.
                        
                         ### API Key Authentication
                        
                         Include your API key in the request header:
                        
                         ```
                         x-api-key: {your_api_key}
                         ```
                        
                         ### Obtaining an API Key
                        
                         1. Log in to your Atlas CMMS account
                         2. Navigate to **Settings > Integrations > API Keys**
                         3. Click **Generate New Key**
                         4. Copy and securely store your key (it will only be shown once)
                         5. Use this key in the `x-api-key` header for all API requests
                        
                         ### Example Request
                        
                         ```bash
                         curl -X GET "https://api.atlas-cmms.com/locations" \\
                           -H "x-api-key: your_api_key_here"
                         ```
                        
                         ### API Key Best Practices
                        
                         - Keep your API key secure and never expose it publicly
                         - Store it in environment variables (e.g., `{{apiKey}}` in Postman)
                         - Rotate keys regularly
                         - Revoke compromised keys immediately
                        
                         ---
                        
                         ### Using Postman or Insomnia
                        
                         1. Download and install Postman or Insomnia
                         2. Import the OpenAPI specification from: `https://api.atlas-cmms.com/v3/api-docs/atlas-cmms`
                         3. Set up environment variables:
                            - `baseUrl`: Your API base URL (e.g., `https://api.atlas-cmms.com`)
                            - `apiKey`: Your API key
                        
                         ### Testing Your Setup
                        
                         To verify your configuration:
                        
                         1. Add the `x-api-key` header to your requests
                         2. Make a GET request to `/locations`
                         3. You should receive a JSON response with your organization's locations
                        
                         ---
                        
                         ### Rate Limiting
                        
                         The Atlas CMMS API implements rate limiting to ensure fair usage and system stability.
                        
                        """,
                contact = @Contact(
                        name = "Atlas CMMS Support",
                        email = "contact@atlas-cmms.com"
                ),
                license = @License(
                        name = "Proprietary"
                )
        ),
        security = {
                @SecurityRequirement(name = "apiKey")
        },
        servers = {
                @Server(url = "https://api.atlas-cmms.com", description = "Production server"),
                @Server(url = "http://localhost:8080", description = "Development server")
        }
)
@SecurityScheme(
        name = "apiKey",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        paramName = "x-api-key",
        description = "Enter your API key. You can generate one from Settings > Integrations > API Keys in your Atlas" +
                " CMMS account."
)
public class OpenApiConfig {

    @Bean
    public GlobalOpenApiCustomizer webhookCustomiser() {
        return openApi -> {
            // 1. Add schemas
            if (openApi.getComponents() == null) {
                openApi.setComponents(new io.swagger.v3.oas.models.Components());
            }
            if (openApi.getComponents().getSchemas() == null) {
                openApi.getComponents().setSchemas(new LinkedHashMap<>());
            }
            openApi.getComponents().getSchemas().putAll(createWebhookSchemas());

            // 2. Add webhooks
            Map<String, PathItem> webhooks = new LinkedHashMap<>();
            webhooks.put("workOrderStatusChange", createWebhookOperation(
                    "Work Order Status Change",
                    "Triggered when a work order status changes (e.g., OPEN -> IN_PROGRESS, IN_PROGRESS -> COMPLETE)",
                    "workOrderStatusChangePayload"
            ));
            webhooks.put("workRequestStatusChange", createWebhookOperation(
                    "Work Request Status Change",
                    "Triggered when a work request is approved or cancelled",
                    "workRequestStatusChangePayload"
            ));
            webhooks.put("partQuantityChanged", createWebhookOperation(
                    "Part Quantity Changed",
                    "Triggered when a part's quantity changes (through consumption, purchase order, or direct update)",
                    "partQuantityChangedPayload"
            ));
            webhooks.put("partChange", createWebhookOperation(
                    "Part Change",
                    "Triggered when a part is updated",
                    "partChangePayload"
            ));
            webhooks.put("assetStatusChange", createWebhookOperation(
                    "Asset Status Change",
                    "Triggered when an asset's status changes (e.g., OPERATIONAL -> DOWN)",
                    "assetStatusChangePayload"
            ));
            webhooks.put("meterTriggerStatusChange", createWebhookOperation(
                    "Meter Trigger Status Change",
                    "Triggered when a meter reading triggers a work order based on threshold conditions",
                    "meterTriggerStatusChangePayload"
            ));
            webhooks.put("newCategoryOnWorkOrder", createWebhookOperation(
                    "New Category on Work Order",
                    "Triggered when a category is added or changed on a work order",
                    "newCategoryOnWorkOrderPayload"
            ));
            webhooks.put("newWorkOrder", createWebhookOperation(
                    "New Work Order",
                    "Triggered when a new work order is created",
                    "newWorkOrderPayload"
            ));
            webhooks.put("workOrderChange", createWebhookOperation(
                    "Work Order Change",
                    "Triggered when a work order is updated",
                    "workOrderChangePayload"
            ));
            webhooks.put("workOrderDelete", createWebhookOperation(
                    "Work Order Delete",
                    "Triggered when a work order is deleted",
                    "workOrderDeletePayload"
            ));
            webhooks.put("newAsset", createWebhookOperation(
                    "New Asset",
                    "Triggered when a new asset is created",
                    "newAssetPayload"
            ));
            webhooks.put("newPart", createWebhookOperation(
                    "New Part",
                    "Triggered when a new part is created",
                    "newPartPayload"
            ));
            webhooks.put("partDelete", createWebhookOperation(
                    "Part Delete",
                    "Triggered when a part is deleted",
                    "partDeletePayload"
            ));
            webhooks.put("newRequest", createWebhookOperation(
                    "New Request",
                    "Triggered when a new work request is created",
                    "newRequestPayload"
            ));
            webhooks.put("newLocation", createWebhookOperation(
                    "New Location",
                    "Triggered when a new location is created",
                    "newLocationPayload"
            ));
            webhooks.put("newVendor", createWebhookOperation(
                    "New Vendor",
                    "Triggered when a new vendor is created",
                    "newVendorPayload"
            ));
            openApi.setWebhooks(webhooks);
        };
    }

    private io.swagger.v3.oas.models.PathItem createWebhookOperation(
            String summary, String description, String schemaRef) {

        Schema<?> payloadSchema = new Schema<>().$ref("#/components/schemas/" + schemaRef);

        MediaType mediaType = new MediaType()
                .schema(payloadSchema)
                .example(createExampleForSchema(schemaRef));

        ApiResponse response = new ApiResponse()
                .description("Return a 200 status to indicate that the data was received successfully");

        Operation postOperation = new Operation()
                .summary(summary)
                .description(description)
                .requestBody(new io.swagger.v3.oas.models.parameters.RequestBody()
                        .content(new Content().addMediaType("application/json", mediaType)))
                .responses(new ApiResponses().addApiResponse("200", response));

        return new PathItem().post(postOperation);
    }

    private Object createExampleForSchema(String schemaRef) {
        return switch (schemaRef) {
            case "workOrderStatusChangePayload" -> Map.of(
                    "workOrderId", 12345,
                    "workOrderTitle", "HVAC Maintenance",
                    "previousStatus", "OPEN",
                    "newStatus", "IN_PROGRESS",
                    "changedWorkOrder", Map.of(
                            "id", 12345,
                            "title", "HVAC Maintenance",
                            "status", "IN_PROGRESS",
                            "description", "Regular maintenance for HVAC system"
                    ),
                    "occurredAt", "2024-01-15T10:30:00Z",
                    "companyId", 1001
            );
            case "workRequestStatusChangePayload" -> Map.of(
                    "requestId", 67890,
                    "requestTitle", "Light bulb replacement request",
                    "previousStatus", "PENDING",
                    "newStatus", "APPROVED",
                    "workOrderId", 12345,
                    "changedRequest", Map.of(
                            "id", 67890,
                            "title", "Light bulb replacement request",
                            "status", "APPROVED"
                    ),
                    "occurredAt", "2024-01-15T10:30:00Z",
                    "companyId", 1001
            );
            case "partQuantityChangedPayload" -> Map.of(
                    "partId", 111,
                    "partName", "Air Filter",
                    "previousQuantity", 50.0,
                    "newQuantity", 45.0,
                    "changedAmount", -5.0,
                    "workOrderId", 12345,
                    "changedPart", Map.of(
                            "id", 111,
                            "name", "Air Filter",
                            "quantity", 45.0
                    ),
                    "occurredAt", "2024-01-15T10:30:00Z",
                    "companyId", 1001
            );
            case "partChangePayload" -> Map.of(
                    "partId", 111,
                    "changedPart", Map.of(
                            "id", 111,
                            "name", "Air Filter"
                    ),
                    "occurredAt", "2024-01-15T10:30:00Z",
                    "companyId", 1001
            );
            case "partDeletePayload" -> Map.of(
                    "partId", 111,
                    "deletePart", Map.of(
                            "id", 111,
                            "name", "Air Filter"
                    ),
                    "occurredAt", "2024-01-15T10:30:00Z",
                    "companyId", 1001
            );
            case "assetStatusChangePayload" -> Map.of(
                    "assetId", 222,
                    "assetName", "Conveyor Belt A",
                    "previousStatus", "OPERATIONAL",
                    "newStatus", "DOWN",
                    "changedAsset", Map.of(
                            "id", 222,
                            "name", "Conveyor Belt A",
                            "status", "DOWN"
                    ),
                    "occurredAt", "2024-01-15T10:30:00Z",
                    "companyId", 1001
            );
            case "meterTriggerStatusChangePayload" -> Map.ofEntries(
                    Map.entry("meterId", 333),
                    Map.entry("meterName", "Temperature Gauge"),
                    Map.entry("meterTriggerId", 444),
                    Map.entry("meterTriggerName", "High Temperature Alert"),
                    Map.entry("readingValue", 85.5),
                    Map.entry("triggerValue", 80.0),
                    Map.entry("triggerCondition", "MORE_THAN"),
                    Map.entry("workOrderId", 12345),
                    Map.entry("triggeredWorkOrder", Map.of(
                            "id", 12345,
                            "title", "HVAC Maintenance",
                            "status", "OPEN"
                    )),
                    Map.entry("occurredAt", "2024-01-15T10:30:00Z"),
                    Map.entry("companyId", 1001)
            );
            case "newCategoryOnWorkOrderPayload" -> Map.of(
                    "workOrderId", 12345,
                    "workOrderTitle", "HVAC Maintenance",
                    "previousCategoryId", 153,
                    "newCategoryId", 555,
                    "newCategoryName", "Preventive Maintenance",
                    "changedWorkOrder", Map.of(
                            "id", 12345,
                            "title", "HVAC Maintenance",
                            "category", Map.of("id", 555, "name", "Preventive Maintenance")
                    ),
                    "occurredAt", "2024-01-15T10:30:00Z",
                    "companyId", 1001
            );
            case "newWorkOrderPayload" -> Map.of(
                    "workOrderId", 12345,
                    "workOrderTitle", "HVAC Maintenance",
                    "newWorkOrder", Map.of(
                            "id", 12345,
                            "title", "HVAC Maintenance",
                            "status", "OPEN"
                    ),
                    "occurredAt", "2024-01-15T10:30:00Z",
                    "companyId", 1001
            );
            case "workOrderChangePayload" -> Map.of(
                    "workOrderId", 12345,
                    "workOrderTitle", "HVAC Maintenance",
                    "changedWorkOrder", Map.of(
                            "id", 12345,
                            "title", "HVAC Maintenance",
                            "status", "IN_PROGRESS"
                    ),
                    "occurredAt", "2024-01-15T10:30:00Z",
                    "companyId", 1001
            );
            case "workOrderDeletePayload" -> Map.of(
                    "workOrderId", 12345,
                    "workOrderTitle", "HVAC Maintenance",
                    "deleteWorkOrder", Map.of(
                            "id", 12345,
                            "title", "HVAC Maintenance"
                    ),
                    "occurredAt", "2024-01-15T10:30:00Z",
                    "companyId", 1001
            );
            case "newAssetPayload" -> Map.of(
                    "assetId", 222,
                    "assetName", "Conveyor Belt A",
                    "newAsset", Map.of(
                            "id", 222,
                            "name", "Conveyor Belt A",
                            "status", "OPERATIONAL"
                    ),
                    "occurredAt", "2024-01-15T10:30:00Z",
                    "companyId", 1001
            );
            case "newPartPayload" -> Map.of(
                    "partId", 111,
                    "partName", "Air Filter",
                    "newPart", Map.of(
                            "id", 111,
                            "name", "Air Filter",
                            "quantity", 50.0
                    ),
                    "occurredAt", "2024-01-15T10:30:00Z",
                    "companyId", 1001
            );
            case "newRequestPayload" -> Map.of(
                    "requestId", 67890,
                    "requestTitle", "Light bulb replacement",
                    "newRequest", Map.of(
                            "id", 67890,
                            "title", "Light bulb replacement",
                            "status", "PENDING"
                    ),
                    "occurredAt", "2024-01-15T10:30:00Z",
                    "companyId", 1001
            );
            case "newLocationPayload" -> Map.of(
                    "locationId", 777,
                    "locationName", "Building A - Floor 1",
                    "newLocation", Map.of(
                            "id", 777,
                            "name", "Building A - Floor 1"
                    ),
                    "occurredAt", "2024-01-15T10:30:00Z",
                    "companyId", 1001
            );
            case "newVendorPayload" -> Map.of(
                    "vendorId", 888,
                    "vendorName", "HVAC Supplies Co.",
                    "newVendor", Map.of(
                            "id", 888,
                            "name", "HVAC Supplies Co.",
                            "email", "contact@hvacsupplies.com",
                            "phone", "+1-555-0123"
                    ),
                    "occurredAt", "2024-01-15T10:30:00Z",
                    "companyId", 1001
            );
            default -> Map.of("example", "See event documentation for details");
        };
    }

    private Map<String, Schema> createWebhookSchemas() {
        Map<String, Schema> schemas = new LinkedHashMap<>();

        // Common fields schema
        Schema<?> commonFields = new ObjectSchema()
                .addProperty("occurredAt", new StringSchema()
                        .format("date-time")
                        .description("Date & time at which the event occurred"))
                .addProperty("companyId", new io.swagger.v3.oas.models.media.IntegerSchema()
                        .description("Global ID of the organization"));

        // Work Order Status Change
        schemas.put("workOrderStatusChangePayload", new ObjectSchema()
                .addProperty("workOrderId", new io.swagger.v3.oas.models.media.IntegerSchema()
                        .description("Global ID of the work order"))
                .addProperty("workOrderTitle", new StringSchema()
                        .description("Title of the work order"))
                .addProperty("previousStatus", new StringSchema()
                        .description("Previous status of the work order")
                        .example("OPEN"))
                .addProperty("newStatus", new StringSchema()
                        .description("New status of the work order")
                        .example("IN_PROGRESS"))
                .addProperty("changedWorkOrder", new Schema<>()
                        .$ref("#/components/schemas/WorkOrderShowDTO")
                        .description("Serialized work order object (sent when endpoint serialization is enabled)"))
                .addProperty("occurredAt", new StringSchema().format("date-time")
                        .description("Date & time at which the work order status was changed"))
                .addProperty("companyId", new io.swagger.v3.oas.models.media.IntegerSchema()
                        .description("Global ID of the organization"))
                .required(List.of("workOrderId", "workOrderTitle", "previousStatus", "newStatus", "occurredAt",
                        "companyId")));

        // Work Request Status Change
        schemas.put("workRequestStatusChangePayload", new ObjectSchema()
                .addProperty("requestId", new io.swagger.v3.oas.models.media.IntegerSchema()
                        .description("Global ID of the request"))
                .addProperty("requestTitle", new StringSchema()
                        .description("Title of the request"))
                .addProperty("previousStatus", new StringSchema()
                        .description("Previous status of the request")
                        .example("PENDING"))
                .addProperty("newStatus", new StringSchema()
                        .description("New status of the request")
                        .example("APPROVED"))
                .addProperty("workOrderId", new io.swagger.v3.oas.models.media.IntegerSchema()
                        .description("Global ID of the created work order (if approved)"))
                .addProperty("changedRequest", new Schema<>()
                        .$ref("#/components/schemas/RequestShowDTO")
                        .description("Serialized request object (sent when endpoint serialization is enabled)"))
                .addProperty("occurredAt", new StringSchema().format("date-time")
                        .description("Date & time at which the request status was changed"))
                .addProperty("companyId", new io.swagger.v3.oas.models.media.IntegerSchema()
                        .description("Global ID of the organization"))
                .required(List.of("requestId", "requestTitle", "previousStatus", "newStatus", "occurredAt",
                        "companyId")));

        // Part Quantity Changed
        schemas.put("partQuantityChangedPayload", new ObjectSchema()
                .addProperty("partId", new io.swagger.v3.oas.models.media.IntegerSchema()
                        .description("Global ID of the part"))
                .addProperty("partName", new StringSchema()
                        .description("Name of the part"))
                .addProperty("previousQuantity", new io.swagger.v3.oas.models.media.NumberSchema()
                        .description("Previous quantity of the part"))
                .addProperty("newQuantity", new io.swagger.v3.oas.models.media.NumberSchema()
                        .description("New quantity of the part"))
                .addProperty("changedAmount", new io.swagger.v3.oas.models.media.NumberSchema()
                        .description("Amount changed (positive for additions, negative for deductions)"))
                .addProperty("workOrderId", new io.swagger.v3.oas.models.media.IntegerSchema()
                        .description("Global ID of the work order (if applicable)"))
                .addProperty("changedPart", new Schema<>()
                        .$ref("#/components/schemas/PartShowDTO")
                        .description("Serialized part object (sent when endpoint serialization is enabled)"))
                .addProperty("occurredAt", new StringSchema().format("date-time")
                        .description("Date & time at which the part quantity was changed"))
                .addProperty("companyId", new io.swagger.v3.oas.models.media.IntegerSchema()
                        .description("Global ID of the organization"))
                .required(List.of("partId", "partName", "previousQuantity", "newQuantity", "changedAmount",
                        "occurredAt", "companyId")));

        // Part Change
        schemas.put("partChangePayload", new ObjectSchema()
                .addProperty("partId", new io.swagger.v3.oas.models.media.IntegerSchema()
                        .description("Global ID of the part"))
                .addProperty("changedPart", new Schema<>()
                        .$ref("#/components/schemas/PartShowDTO")
                        .description("Serialized part object (sent when endpoint serialization is enabled)"))
                .addProperty("occurredAt", new StringSchema().format("date-time")
                        .description("Date & time at which the part was changed"))
                .addProperty("companyId", new io.swagger.v3.oas.models.media.IntegerSchema()
                        .description("Global ID of the organization"))
                .required(List.of("partId", "occurredAt", "companyId")));

        // Part Delete
        schemas.put("partDeletePayload", new ObjectSchema()
                .addProperty("partId", new io.swagger.v3.oas.models.media.IntegerSchema()
                        .description("Global ID of the deleted part"))
                .addProperty("deletePart", new Schema<>()
                        .$ref("#/components/schemas/PartShowDTO")
                        .description("Serialized part object (sent when endpoint serialization is enabled)"))
                .addProperty("occurredAt", new StringSchema().format("date-time")
                        .description("Date & time at which the part was deleted"))
                .addProperty("companyId", new io.swagger.v3.oas.models.media.IntegerSchema()
                        .description("Global ID of the organization"))
                .required(List.of("partId", "occurredAt", "companyId")));

        // Asset Status Change
        schemas.put("assetStatusChangePayload", new ObjectSchema()
                .addProperty("assetId", new io.swagger.v3.oas.models.media.IntegerSchema()
                        .description("Global ID of the asset"))
                .addProperty("assetName", new StringSchema()
                        .description("Name of the asset"))
                .addProperty("previousStatus", new StringSchema()
                        .description("Previous status of the asset")
                        .example("OPERATIONAL"))
                .addProperty("newStatus", new StringSchema()
                        .description("New status of the asset")
                        .example("DOWN"))
                .addProperty("changedAsset", new Schema<>()
                        .$ref("#/components/schemas/AssetShowDTO")
                        .description("Serialized asset object (sent when endpoint serialization is enabled)"))
                .addProperty("occurredAt", new StringSchema().format("date-time")
                        .description("Date & time at which the asset status was changed"))
                .addProperty("companyId", new io.swagger.v3.oas.models.media.IntegerSchema()
                        .description("Global ID of the organization"))
                .required(List.of("assetId", "assetName", "previousStatus", "newStatus", "occurredAt", "companyId")));

        // Meter Trigger Status Change
        schemas.put("meterTriggerStatusChangePayload", new ObjectSchema()
                .addProperty("meterId", new io.swagger.v3.oas.models.media.IntegerSchema()
                        .description("Global ID of the meter"))
                .addProperty("meterName", new StringSchema()
                        .description("Name of the meter"))
                .addProperty("meterTriggerId", new io.swagger.v3.oas.models.media.IntegerSchema()
                        .description("Global ID of the meter trigger"))
                .addProperty("meterTriggerName", new StringSchema()
                        .description("Name of the meter trigger"))
                .addProperty("readingValue", new io.swagger.v3.oas.models.media.NumberSchema()
                        .description("The reading value that triggered the event"))
                .addProperty("triggerValue", new io.swagger.v3.oas.models.media.NumberSchema()
                        .description("The threshold value of the trigger"))
                .addProperty("triggerCondition", new StringSchema()
                        .description("The condition that was met (e.g., MORE_THAN, LESS_THAN)"))
                .addProperty("workOrderId", new io.swagger.v3.oas.models.media.IntegerSchema()
                        .description("Global ID of the created work order"))
                .addProperty("triggeredWorkOrder", new Schema<>()
                        .$ref("#/components/schemas/WorkOrderShowDTO")
                        .description("Serialized work order object (sent when endpoint serialization is enabled)"))
                .addProperty("occurredAt", new StringSchema().format("date-time")
                        .description("Date & time at which the meter trigger fired"))
                .addProperty("companyId", new io.swagger.v3.oas.models.media.IntegerSchema()
                        .description("Global ID of the organization"))
                .required(List.of("meterId", "meterName", "meterTriggerId", "meterTriggerName", "readingValue",
                        "triggerValue", "triggerCondition", "workOrderId", "occurredAt", "companyId")));

        // New Category on Work Order
        schemas.put("newCategoryOnWorkOrderPayload", new ObjectSchema()
                .addProperty("workOrderId", new io.swagger.v3.oas.models.media.IntegerSchema()
                        .description("Global ID of the work order"))
                .addProperty("workOrderTitle", new StringSchema()
                        .description("Title of the work order"))
                .addProperty("previousCategoryId", new io.swagger.v3.oas.models.media.IntegerSchema()
                        .description("Previous category ID (null if none)"))
                .addProperty("newCategoryId", new io.swagger.v3.oas.models.media.IntegerSchema()
                        .description("New category ID"))
                .addProperty("newCategoryName", new StringSchema()
                        .description("Name of the new category"))
                .addProperty("changedWorkOrder", new Schema<>()
                        .$ref("#/components/schemas/WorkOrderShowDTO")
                        .description("Serialized work order object (sent when endpoint serialization is enabled)"))
                .addProperty("occurredAt", new StringSchema().format("date-time")
                        .description("Date & time at which the category was changed"))
                .addProperty("companyId", new io.swagger.v3.oas.models.media.IntegerSchema()
                        .description("Global ID of the organization"))
                .required(List.of("workOrderId", "workOrderTitle", "newCategoryId", "newCategoryName", "occurredAt",
                        "companyId")));

        // New Work Order
        schemas.put("newWorkOrderPayload", new ObjectSchema()
                .addProperty("workOrderId", new io.swagger.v3.oas.models.media.IntegerSchema()
                        .description("Global ID of the work order"))
                .addProperty("workOrderTitle", new StringSchema()
                        .description("Title of the work order"))
                .addProperty("newWorkOrder", new Schema<>()
                        .$ref("#/components/schemas/WorkOrderShowDTO")
                        .description("Serialized work order object (sent when endpoint serialization is enabled)"))
                .addProperty("occurredAt", new StringSchema().format("date-time")
                        .description("Date & time at which the work order was created"))
                .addProperty("companyId", new io.swagger.v3.oas.models.media.IntegerSchema()
                        .description("Global ID of the organization"))
                .required(List.of("workOrderId", "workOrderTitle", "occurredAt", "companyId")));

        // Work Order Change
        schemas.put("workOrderChangePayload", new ObjectSchema()
                .addProperty("workOrderId", new io.swagger.v3.oas.models.media.IntegerSchema()
                        .description("Global ID of the work order"))
                .addProperty("workOrderTitle", new StringSchema()
                        .description("Title of the work order"))
                .addProperty("changedWorkOrder", new Schema<>()
                        .$ref("#/components/schemas/WorkOrderShowDTO")
                        .description("Serialized work order object (sent when endpoint serialization is enabled)"))
                .addProperty("occurredAt", new StringSchema().format("date-time")
                        .description("Date & time at which the work order was changed"))
                .addProperty("companyId", new io.swagger.v3.oas.models.media.IntegerSchema()
                        .description("Global ID of the organization"))
                .required(List.of("workOrderId", "workOrderTitle", "occurredAt", "companyId")));

        // Work Order Delete
        schemas.put("workOrderDeletePayload", new ObjectSchema()
                .addProperty("workOrderId", new io.swagger.v3.oas.models.media.IntegerSchema()
                        .description("Global ID of the deleted work order"))
                .addProperty("workOrderTitle", new StringSchema()
                        .description("Title of the deleted work order"))
                .addProperty("deleteWorkOrder", new Schema<>()
                        .$ref("#/components/schemas/WorkOrderShowDTO")
                        .description("Serialized work order object (sent when endpoint serialization is enabled)"))
                .addProperty("occurredAt", new StringSchema().format("date-time")
                        .description("Date & time at which the work order was deleted"))
                .addProperty("companyId", new io.swagger.v3.oas.models.media.IntegerSchema()
                        .description("Global ID of the organization"))
                .required(List.of("workOrderId", "workOrderTitle", "occurredAt", "companyId")));

        // New Asset
        schemas.put("newAssetPayload", new ObjectSchema()
                .addProperty("assetId", new io.swagger.v3.oas.models.media.IntegerSchema()
                        .description("Global ID of the asset"))
                .addProperty("assetName", new StringSchema()
                        .description("Name of the asset"))
                .addProperty("newAsset", new Schema<>()
                        .$ref("#/components/schemas/AssetShowDTO")
                        .description("Serialized asset object (sent when endpoint serialization is enabled)"))
                .addProperty("occurredAt", new StringSchema().format("date-time")
                        .description("Date & time at which the asset was created"))
                .addProperty("companyId", new io.swagger.v3.oas.models.media.IntegerSchema()
                        .description("Global ID of the organization"))
                .required(List.of("assetId", "assetName", "occurredAt", "companyId")));

        // New Part
        schemas.put("newPartPayload", new ObjectSchema()
                .addProperty("partId", new io.swagger.v3.oas.models.media.IntegerSchema()
                        .description("Global ID of the part"))
                .addProperty("partName", new StringSchema()
                        .description("Name of the part"))
                .addProperty("newPart", new Schema<>()
                        .$ref("#/components/schemas/PartShowDTO")
                        .description("Serialized part object (sent when endpoint serialization is enabled)"))
                .addProperty("occurredAt", new StringSchema().format("date-time")
                        .description("Date & time at which the part was created"))
                .addProperty("companyId", new io.swagger.v3.oas.models.media.IntegerSchema()
                        .description("Global ID of the organization"))
                .required(List.of("partId", "partName", "occurredAt", "companyId")));

        // New Request
        schemas.put("newRequestPayload", new ObjectSchema()
                .addProperty("requestId", new io.swagger.v3.oas.models.media.IntegerSchema()
                        .description("Global ID of the request"))
                .addProperty("requestTitle", new StringSchema()
                        .description("Title of the request"))
                .addProperty("newRequest", new Schema<>()
                        .$ref("#/components/schemas/RequestShowDTO")
                        .description("Serialized request object (sent when endpoint serialization is enabled)"))
                .addProperty("occurredAt", new StringSchema().format("date-time")
                        .description("Date & time at which the request was created"))
                .addProperty("companyId", new io.swagger.v3.oas.models.media.IntegerSchema()
                        .description("Global ID of the organization"))
                .required(List.of("requestId", "requestTitle", "occurredAt", "companyId")));

        // New Location
        schemas.put("newLocationPayload", new ObjectSchema()
                .addProperty("locationId", new io.swagger.v3.oas.models.media.IntegerSchema()
                        .description("Global ID of the location"))
                .addProperty("locationName", new StringSchema()
                        .description("Name of the location"))
                .addProperty("newLocation", new Schema<>()
                        .$ref("#/components/schemas/LocationShowDTO")
                        .description("Serialized location object (sent when endpoint serialization is enabled)"))
                .addProperty("occurredAt", new StringSchema().format("date-time")
                        .description("Date & time at which the location was created"))
                .addProperty("companyId", new io.swagger.v3.oas.models.media.IntegerSchema()
                        .description("Global ID of the organization"))
                .required(List.of("locationId", "locationName", "occurredAt", "companyId")));

        // New Vendor
        schemas.put("newVendorPayload", new ObjectSchema()
                .addProperty("vendorId", new io.swagger.v3.oas.models.media.IntegerSchema()
                        .description("Global ID of the vendor"))
                .addProperty("vendorName", new StringSchema()
                        .description("Name of the vendor"))
                .addProperty("newVendor", new Schema<>()
                        .$ref("#/components/schemas/Vendor")
                        .description("Serialized vendor object (sent when endpoint serialization is enabled)"))
                .addProperty("occurredAt", new StringSchema().format("date-time")
                        .description("Date & time at which the vendor was created"))
                .addProperty("companyId", new io.swagger.v3.oas.models.media.IntegerSchema()
                        .description("Global ID of the organization"))
                .required(List.of("vendorId", "vendorName", "occurredAt", "companyId")));

        return schemas;
    }

    @Bean
    public GroupedOpenApi webhookApi() {
        return GroupedOpenApi.builder()
                .group("Subscriptions & Webhooks")
                .packagesToScan("com.grash.controller")
                .pathsToMatch("/webhook-endpoints/**")
                .addOpenApiCustomizer(openApi -> openApi.info(
                        new io.swagger.v3.oas.models.info.Info()
                                .title("Atlas CMMS Webhooks API")
                                .version("v1")
                                .description("""
                                        ## Webhooks in Atlas CMMS
                                        
                                        Webhooks are HTTP callbacks that allow different systems to communicate with each other in real-time.
                                        They're like automated messengers that deliver information when something happens, rather than requiring
                                        you to ask for it.
                                        
                                        In the context of Atlas CMMS, webhooks are a way for our system to automatically notify your application
                                        when specific events occur in your account. Instead of your application repeatedly checking our API for
                                        updates (a process known as "polling"), webhooks allow you to receive real-time notifications about important
                                        events like:
                                        
                                        - Work order status changes
                                        - New work orders being created
                                        - Asset status changes
                                        - Part quantity changes
                                        - And more...
                                        
                                        When an event occurs, Atlas CMMS sends an HTTP POST request to the endpoint you specify.
                                        The request contains details about the event. It allows your application to react immediately
                                        to changes in Atlas CMMS.
                                        
                                        See the top-level `webhooks` section for detailed documentation of all webhook event types and their payload schemas.
                                        """)
                ))
                .build();
    }
}
