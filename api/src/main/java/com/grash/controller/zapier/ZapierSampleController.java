package com.grash.controller.zapier;

import com.grash.model.enums.webhook.WebhookEvent;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/zapier/samples")
@Hidden
public class ZapierSampleController {

    @GetMapping("/{eventType}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<Map<String, Object>>> getSamples(
            @Parameter(description = "The webhook event type (e.g., WORK_ORDER_STATUS_CHANGE)")
            @PathVariable String eventType) {

        if (!Arrays.stream(WebhookEvent.values()).map(Enum::name).toList().contains(eventType)) {
            return ResponseEntity.badRequest().body(List.of(Map.of(
                    "error", "Unknown event type: " + eventType,
                    "supportedEvents", WebhookEvent.values()
            )));
        }

        return ResponseEntity.ok(getSampleData(eventType));
    }

    private List<Map<String, Object>> getSampleData(String eventType) {
        return switch (WebhookEvent.valueOf(eventType)) {
            case WORK_ORDER_STATUS_CHANGE -> workOrderStatusChangeSamples();
            case WORK_REQUEST_STATUS_CHANGE -> workRequestStatusChangeSamples();
            case PART_QUANTITY_CHANGED -> partQuantityChangedSamples();
            case PART_CHANGE -> partChangeSamples();
            case PART_DELETE -> partDeleteSamples();
            case ASSET_STATUS_CHANGE -> assetStatusChangeSamples();
            case METER_TRIGGER_STATUS_CHANGE -> meterTriggerStatusChangeSamples();
            case NEW_CATEGORY_ON_WORK_ORDER -> newCategoryOnWorkOrderSamples();
            case NEW_WORK_ORDER -> newWorkOrderSamples();
            case WORK_ORDER_CHANGE -> workOrderChangeSamples();
            case WORK_ORDER_DELETE -> workOrderDeleteSamples();
            case NEW_ASSET -> newAssetSamples();
            case NEW_PART -> newPartSamples();
            case NEW_REQUEST -> newRequestSamples();
            case NEW_LOCATION -> newLocationSamples();
            case NEW_VENDOR -> newVendorSamples();
            default -> List.of(Map.of("error", "No sample data for event: " + eventType));
        };
    }

    private List<Map<String, Object>> workOrderStatusChangeSamples() {
        return List.of(
                Map.of(
                        "workOrderId", 12345,
                        "workOrderTitle", "HVAC System Maintenance",
                        "previousStatus", "OPEN",
                        "newStatus", "IN_PROGRESS",
                        "changedWorkOrder", Map.of(
                                "id", 12345,
                                "title", "HVAC System Maintenance",
                                "status", "IN_PROGRESS",
                                "priority", "HIGH",
                                "description", "Quarterly maintenance check for HVAC unit"
                        ),
                        "occurredAt", "2026-04-07T10:00:00Z",
                        "companyId", 1001
                ),
                Map.of(
                        "workOrderId", 12389,
                        "workOrderTitle", "Elevator Inspection",
                        "previousStatus", "IN_PROGRESS",
                        "newStatus", "COMPLETE",
                        "changedWorkOrder", Map.of(
                                "id", 12389,
                                "title", "Elevator Inspection",
                                "status", "COMPLETE",
                                "priority", "MEDIUM",
                                "description", "Annual elevator safety inspection"
                        ),
                        "occurredAt", "2026-04-07T11:30:00Z",
                        "companyId", 1001
                )
        );
    }

    private List<Map<String, Object>> workRequestStatusChangeSamples() {
        return List.of(
                Map.of(
                        "requestId", 67890,
                        "requestTitle", "Office Lighting Replacement",
                        "previousStatus", "PENDING",
                        "newStatus", "APPROVED",
                        "workOrderId", 12400,
                        "changedRequest", Map.of(
                                "id", 67890,
                                "title", "Office Lighting Replacement",
                                "status", "APPROVED",
                                "description", "Replace fluorescent lights with LED panels on floor 3"
                        ),
                        "occurredAt", "2026-04-07T09:15:00Z",
                        "companyId", 1001
                ),
                Map.of(
                        "requestId", 67912,
                        "requestTitle", "Parking Lot Pothole Repair",
                        "previousStatus", "PENDING",
                        "newStatus", "REJECTED",
                        "workOrderId", null,
                        "changedRequest", Map.of(
                                "id", 67912,
                                "title", "Parking Lot Pothole Repair",
                                "status", "REJECTED",
                                "description", "Fill potholes in section B of parking lot"
                        ),
                        "occurredAt", "2026-04-07T14:00:00Z",
                        "companyId", 1001
                )
        );
    }

    private List<Map<String, Object>> partQuantityChangedSamples() {
        return List.of(
                Map.of(
                        "partId", 111,
                        "partName", "HEPA Air Filter",
                        "previousQuantity", 50.0,
                        "newQuantity", 45.0,
                        "changedAmount", -5.0,
                        "workOrderId", 12345,
                        "changedPart", Map.of(
                                "id", 111,
                                "name", "HEPA Air Filter",
                                "quantity", 45.0,
                                "sku", "FLT-HEPA-001"
                        ),
                        "occurredAt", "2026-04-07T10:30:00Z",
                        "companyId", 1001
                ),
                Map.of(
                        "partId", 225,
                        "partName", "Lubricant Oil (1L)",
                        "previousQuantity", 20.0,
                        "newQuantity", 35.0,
                        "changedAmount", 15.0,
                        "workOrderId", null,
                        "changedPart", Map.of(
                                "id", 225,
                                "name", "Lubricant Oil (1L)",
                                "quantity", 35.0,
                                "sku", "LUB-OIL-1L"
                        ),
                        "occurredAt", "2026-04-06T16:45:00Z",
                        "companyId", 1001
                )
        );
    }

    private List<Map<String, Object>> partChangeSamples() {
        return List.of(
                Map.of(
                        "partId", 111,
                        "changedPart", Map.of(
                                "id", 111,
                                "name", "HEPA Air Filter",
                                "sku", "FLT-HEPA-001",
                                "quantity", 45.0,
                                "cost", 24.99
                        ),
                        "occurredAt", "2026-04-07T12:00:00Z",
                        "companyId", 1001
                ),
                Map.of(
                        "partId", 340,
                        "changedPart", Map.of(
                                "id", 340,
                                "name", "Bearing Assembly 6205",
                                "sku", "BRG-6205",
                                "quantity", 12.0,
                                "cost", 15.50
                        ),
                        "occurredAt", "2026-04-05T08:20:00Z",
                        "companyId", 1001
                )
        );
    }

    private List<Map<String, Object>> partDeleteSamples() {
        return List.of(
                Map.of(
                        "partId", 111,
                        "deletePart", Map.of(
                                "id", 111,
                                "name", "HEPA Air Filter",
                                "sku", "FLT-HEPA-001"
                        ),
                        "occurredAt", "2026-04-07T15:00:00Z",
                        "companyId", 1001
                ),
                Map.of(
                        "partId", 502,
                        "deletePart", Map.of(
                                "id", 502,
                                "name", "Thermostat Unit T-200",
                                "sku", "THM-T200"
                        ),
                        "occurredAt", "2026-04-03T11:10:00Z",
                        "companyId", 1001
                )
        );
    }

    private List<Map<String, Object>> assetStatusChangeSamples() {
        return List.of(
                Map.of(
                        "assetId", 222,
                        "assetName", "Conveyor Belt Line A",
                        "previousStatus", "OPERATIONAL",
                        "newStatus", "DOWN",
                        "changedAsset", Map.of(
                                "id", 222,
                                "name", "Conveyor Belt Line A",
                                "status", "DOWN",
                                "location", "Warehouse 1"
                        ),
                        "occurredAt", "2026-04-07T07:45:00Z",
                        "companyId", 1001
                ),
                Map.of(
                        "assetId", 305,
                        "assetName", "Backup Generator G2",
                        "previousStatus", "UNDER_MAINTENANCE",
                        "newStatus", "OPERATIONAL",
                        "changedAsset", Map.of(
                                "id", 305,
                                "name", "Backup Generator G2",
                                "status", "OPERATIONAL",
                                "location", "Utility Room B"
                        ),
                        "occurredAt", "2026-04-07T13:20:00Z",
                        "companyId", 1001
                )
        );
    }

    private List<Map<String, Object>> meterTriggerStatusChangeSamples() {
        return List.of(
                Map.ofEntries(
                        Map.entry("meterId", 333),
                        Map.entry("meterName", "Motor Temperature Sensor"),
                        Map.entry("meterTriggerId", 444),
                        Map.entry("meterTriggerName", "Overtemperature Alert"),
                        Map.entry("readingValue", 85.5),
                        Map.entry("triggerValue", 80.0),
                        Map.entry("triggerCondition", "MORE_THAN"),
                        Map.entry("workOrderId", 12500),
                        Map.entry("triggeredWorkOrder", Map.of(
                                "id", 12500,
                                "title", "Motor Overheating Investigation",
                                "status", "OPEN"
                        )),
                        Map.entry("occurredAt", "2026-04-07T10:30:00Z"),
                        Map.entry("companyId", 1001)
                ),
                Map.ofEntries(
                        Map.entry("meterId", 510),
                        Map.entry("meterName", "Hydraulic Pressure Gauge"),
                        Map.entry("meterTriggerId", 511),
                        Map.entry("meterTriggerName", "Low Pressure Warning"),
                        Map.entry("readingValue", 42.0),
                        Map.entry("triggerValue", 50.0),
                        Map.entry("triggerCondition", "LESS_THAN"),
                        Map.entry("workOrderId", 12510),
                        Map.entry("triggeredWorkOrder", Map.of(
                                "id", 12510,
                                "title", "Hydraulic System Inspection",
                                "status", "OPEN"
                        )),
                        Map.entry("occurredAt", "2026-04-06T18:00:00Z"),
                        Map.entry("companyId", 1001)
                )
        );
    }

    private List<Map<String, Object>> newCategoryOnWorkOrderSamples() {
        return List.of(
                Map.of(
                        "workOrderId", 12345,
                        "workOrderTitle", "HVAC System Maintenance",
                        "previousCategoryId", null,
                        "newCategoryId", 555,
                        "newCategoryName", "Preventive Maintenance",
                        "changedWorkOrder", Map.of(
                                "id", 12345,
                                "title", "HVAC System Maintenance",
                                "status", "IN_PROGRESS",
                                "category", Map.of("id", 555, "name", "Preventive Maintenance")
                        ),
                        "occurredAt", "2026-04-07T09:00:00Z",
                        "companyId", 1001
                ),
                Map.of(
                        "workOrderId", 12600,
                        "workOrderTitle", "Electrical Panel Upgrade",
                        "previousCategoryId", 100,
                        "newCategoryId", 200,
                        "newCategoryName", "Corrective Maintenance",
                        "changedWorkOrder", Map.of(
                                "id", 12600,
                                "title", "Electrical Panel Upgrade",
                                "status", "OPEN",
                                "category", Map.of("id", 200, "name", "Corrective Maintenance")
                        ),
                        "occurredAt", "2026-04-07T11:00:00Z",
                        "companyId", 1001
                )
        );
    }

    private List<Map<String, Object>> newWorkOrderSamples() {
        return List.of(
                Map.of(
                        "workOrderId", 12700,
                        "workOrderTitle", "Fire Suppression System Test",
                        "newWorkOrder", Map.of(
                                "id", 12700,
                                "title", "Fire Suppression System Test",
                                "status", "OPEN",
                                "priority", "HIGH",
                                "assignedTo", "John Smith"
                        ),
                        "occurredAt", "2026-04-07T08:00:00Z",
                        "companyId", 1001
                ),
                Map.of(
                        "workOrderId", 12701,
                        "workOrderTitle", "Roof Leak Repair - Building C",
                        "newWorkOrder", Map.of(
                                "id", 12701,
                                "title", "Roof Leak Repair - Building C",
                                "status", "OPEN",
                                "priority", "MEDIUM",
                                "assignedTo", "Mike Johnson"
                        ),
                        "occurredAt", "2026-04-07T09:30:00Z",
                        "companyId", 1001
                )
        );
    }

    private List<Map<String, Object>> workOrderChangeSamples() {
        return List.of(
                Map.of(
                        "workOrderId", 12345,
                        "workOrderTitle", "HVAC System Maintenance",
                        "changedWorkOrder", Map.of(
                                "id", 12345,
                                "title", "HVAC System Maintenance",
                                "status", "IN_PROGRESS",
                                "priority", "HIGH",
                                "description", "Quarterly maintenance check with filter replacement"
                        ),
                        "occurredAt", "2026-04-07T10:15:00Z",
                        "companyId", 1001
                ),
                Map.of(
                        "workOrderId", 12800,
                        "workOrderTitle", "Plumbing Inspection - Floor 2",
                        "changedWorkOrder", Map.of(
                                "id", 12800,
                                "title", "Plumbing Inspection - Floor 2",
                                "status", "OPEN",
                                "priority", "LOW",
                                "assignedTo", "Sarah Davis"
                        ),
                        "occurredAt", "2026-04-07T14:30:00Z",
                        "companyId", 1001
                )
        );
    }

    private List<Map<String, Object>> workOrderDeleteSamples() {
        return List.of(
                Map.of(
                        "workOrderId", 12900,
                        "workOrderTitle", "Duplicate Entry - Desk Repair",
                        "deleteWorkOrder", Map.of(
                                "id", 12900,
                                "title", "Duplicate Entry - Desk Repair",
                                "status", "OPEN"
                        ),
                        "occurredAt", "2026-04-07T16:00:00Z",
                        "companyId", 1001
                ),
                Map.of(
                        "workOrderId", 12901,
                        "workOrderTitle", "Cancelled - Paint Touch-up",
                        "deleteWorkOrder", Map.of(
                                "id", 12901,
                                "title", "Cancelled - Paint Touch-up",
                                "status", "CANCELLED"
                        ),
                        "occurredAt", "2026-04-06T12:00:00Z",
                        "companyId", 1001
                )
        );
    }

    private List<Map<String, Object>> newAssetSamples() {
        return List.of(
                Map.of(
                        "assetId", 400,
                        "assetName", "CNC Machine #5",
                        "newAsset", Map.of(
                                "id", 400,
                                "name", "CNC Machine #5",
                                "status", "OPERATIONAL",
                                "location", "Production Floor",
                                "category", "Manufacturing Equipment"
                        ),
                        "occurredAt", "2026-04-07T08:30:00Z",
                        "companyId", 1001
                ),
                Map.of(
                        "assetId", 401,
                        "assetName", "Air Compressor AC-300",
                        "newAsset", Map.of(
                                "id", 401,
                                "name", "Air Compressor AC-300",
                                "status", "OPERATIONAL",
                                "location", "Workshop 2",
                                "category", "Compressed Air System"
                        ),
                        "occurredAt", "2026-04-05T10:00:00Z",
                        "companyId", 1001
                )
        );
    }

    private List<Map<String, Object>> newPartSamples() {
        return List.of(
                Map.of(
                        "partId", 600,
                        "partName", "Safety Valve SV-50",
                        "newPart", Map.of(
                                "id", 600,
                                "name", "Safety Valve SV-50",
                                "sku", "VLV-SV50",
                                "quantity", 10.0,
                                "cost", 75.00
                        ),
                        "occurredAt", "2026-04-07T11:00:00Z",
                        "companyId", 1001
                ),
                Map.of(
                        "partId", 601,
                        "partName", "Drive Belt DB-220",
                        "newPart", Map.of(
                                "id", 601,
                                "name", "Drive Belt DB-220",
                                "sku", "BLT-DB220",
                                "quantity", 25.0,
                                "cost", 18.50
                        ),
                        "occurredAt", "2026-04-06T14:15:00Z",
                        "companyId", 1001
                )
        );
    }

    private List<Map<String, Object>> newRequestSamples() {
        return List.of(
                Map.of(
                        "requestId", 7000,
                        "requestTitle", "Broken Window in Conference Room B",
                        "newRequest", Map.of(
                                "id", 7000,
                                "title", "Broken Window in Conference Room B",
                                "status", "PENDING",
                                "description", "Glass cracked on the east-facing window",
                                "requester", "Alice Martin"
                        ),
                        "occurredAt", "2026-04-07T09:00:00Z",
                        "companyId", 1001
                ),
                Map.of(
                        "requestId", 7001,
                        "requestTitle", "AC Not Cooling in Server Room",
                        "newRequest", Map.of(
                                "id", 7001,
                                "title", "AC Not Cooling in Server Room",
                                "status", "PENDING",
                                "description", "Temperature rising above normal levels",
                                "requester", "IT Department"
                        ),
                        "occurredAt", "2026-04-07T10:45:00Z",
                        "companyId", 1001
                )
        );
    }

    private List<Map<String, Object>> newLocationSamples() {
        return List.of(
                Map.of(
                        "locationId", 777,
                        "locationName", "Building A - Floor 2",
                        "newLocation", Map.of(
                                "id", 777,
                                "name", "Building A - Floor 2",
                                "description", "Second floor office space",
                                "parentLocation", "Building A"
                        ),
                        "occurredAt", "2026-04-07T08:00:00Z",
                        "companyId", 1001
                ),
                Map.of(
                        "locationId", 778,
                        "locationName", "Warehouse - Cold Storage",
                        "newLocation", Map.of(
                                "id", 778,
                                "name", "Warehouse - Cold Storage",
                                "description", "Temperature-controlled storage area",
                                "parentLocation", "Warehouse"
                        ),
                        "occurredAt", "2026-04-04T13:00:00Z",
                        "companyId", 1001
                )
        );
    }

    private List<Map<String, Object>> newVendorSamples() {
        return List.of(
                Map.of(
                        "vendorId", 888,
                        "vendorName", "Industrial Supplies Co.",
                        "newVendor", Map.of(
                                "id", 888,
                                "name", "Industrial Supplies Co.",
                                "email", "sales@industrialsupplies.com",
                                "phone", "+1-555-0198",
                                "address", "123 Industrial Blvd, Manufacturing City"
                        ),
                        "occurredAt", "2026-04-07T10:00:00Z",
                        "companyId", 1001
                ),
                Map.of(
                        "vendorId", 889,
                        "vendorName", "TechParts Ltd.",
                        "newVendor", Map.of(
                                "id", 889,
                                "name", "TechParts Ltd.",
                                "email", "orders@techparts.com",
                                "phone", "+1-555-0245",
                                "address", "456 Tech Park, Innovation District"
                        ),
                        "occurredAt", "2026-04-05T15:30:00Z",
                        "companyId", 1001
                )
        );
    }
}
