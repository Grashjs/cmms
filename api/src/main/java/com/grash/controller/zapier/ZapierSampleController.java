package com.grash.controller.zapier;

import com.grash.model.enums.webhook.WebhookEvent;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
                Map.ofEntries(
                        Map.entry("workOrderId", 12345),
                        Map.entry("workOrderTitle", "HVAC System Maintenance"),
                        Map.entry("previousStatus", "OPEN"),
                        Map.entry("newStatus", "IN_PROGRESS"),
                        Map.entry("changedWorkOrder", Map.ofEntries(
                                Map.entry("id", 12345),
                                Map.entry("title", "HVAC System Maintenance"),
                                Map.entry("status", "IN_PROGRESS"),
                                Map.entry("priority", "HIGH"),
                                Map.entry("description", "Quarterly maintenance check for HVAC unit"),
                                Map.entry("dueDate", "2026-04-15T17:00:00Z"),
                                Map.entry("estimatedDuration", 4.0),
                                Map.entry("requiredSignature", true),
                                Map.entry("category", Map.of(
                                        "id", 555,
                                        "name", "Preventive Maintenance"
                                )),
                                Map.entry("location", Map.of(
                                        "id", 777,
                                        "name", "Production Floor",
                                        "address", "Building A, Floor 1",
                                        "customId", "LOC-PF-001"
                                )),
                                Map.entry("team", Map.of(
                                        "id", 301,
                                        "name", "Maintenance Team Alpha",
                                        "users", List.of(
                                                Map.ofEntries(
                                                        Map.entry("id", 1001),
                                                        Map.entry("firstName", "John"),
                                                        Map.entry("lastName", "Smith"),
                                                        Map.entry("phone", "+1-555-0101"),
                                                        Map.entry("image", Map.of(
                                                                "id", 5001,
                                                                "name", "john_smith.jpg",
                                                                "url", "https://example.com/files/5001"
                                                        ))
                                                ),
                                                Map.of(
                                                        "id", 1002,
                                                        "firstName", "Jane",
                                                        "lastName", "Doe",
                                                        "phone", "+1-555-0102"
                                                )
                                        )
                                )),
                                Map.entry("primaryUser", Map.of(
                                        "id", 1001,
                                        "firstName", "John",
                                        "lastName", "Smith",
                                        "phone", "+1-555-0101"
                                )),
                                Map.entry("assignedTo", List.of(
                                        Map.of(
                                                "id", 1001,
                                                "firstName", "John",
                                                "lastName", "Smith",
                                                "phone", "+1-555-0101"
                                        ),
                                        Map.of(
                                                "id", 1002,
                                                "firstName", "Jane",
                                                "lastName", "Doe",
                                                "phone", "+1-555-0102"
                                        )
                                )),
                                Map.entry("customers", List.of(
                                        Map.of(
                                                "id", 2001,
                                                "name", "Acme Corporation"
                                        )
                                )),
                                Map.entry("asset", Map.of(
                                        "id", 400,
                                        "name", "HVAC Unit #3",
                                        "customId", "AST-HVAC-003",
                                        "locationId", 777
                                )),
                                Map.entry("files", List.of(
                                        Map.of(
                                                "id", 6001,
                                                "name", "maintenance_checklist.pdf",
                                                "url", "https://example.com/files/6001"
                                        )
                                )),
                                Map.entry("image", Map.of(
                                        "id", 6002,
                                        "name", "hvac_unit.jpg",
                                        "url", "https://example.com/files/6002"
                                ))
                        )),
                        Map.entry("occurredAt", "2026-04-07T10:00:00Z"),
                        Map.entry("companyId", 1001)
                ),
                Map.ofEntries(
                        Map.entry("workOrderId", 12389),
                        Map.entry("workOrderTitle", "Elevator Inspection"),
                        Map.entry("previousStatus", "IN_PROGRESS"),
                        Map.entry("newStatus", "COMPLETE"),
                        Map.entry("changedWorkOrder", Map.ofEntries(
                                Map.entry("id", 12389),
                                Map.entry("title", "Elevator Inspection"),
                                Map.entry("status", "COMPLETE"),
                                Map.entry("priority", "MEDIUM"),
                                Map.entry("description", "Annual elevator safety inspection"),
                                Map.entry("dueDate", "2026-04-20T17:00:00Z"),
                                Map.entry("estimatedDuration", 8.0),
                                Map.entry("requiredSignature", true),
                                Map.entry("category", Map.of(
                                        "id", 556,
                                        "name", "Safety Inspection"
                                )),
                                Map.entry("location", Map.of(
                                        "id", 778,
                                        "name", "Main Lobby",
                                        "address", "Building B, Ground Floor",
                                        "customId", "LOC-ML-002",
                                        "parentId", 770
                                )),
                                Map.entry("team", Map.of(
                                        "id", 302,
                                        "name", "Inspection Team",
                                        "users", List.of(
                                                Map.of(
                                                        "id", 1003,
                                                        "firstName", "Mike",
                                                        "lastName", "Johnson",
                                                        "phone", "+1-555-0103"
                                                )
                                        )
                                )),
                                Map.entry("primaryUser", Map.of(
                                        "id", 1003,
                                        "firstName", "Mike",
                                        "lastName", "Johnson",
                                        "phone", "+1-555-0103"
                                )),
                                Map.entry("assignedTo", List.of(
                                        Map.of(
                                                "id", 1003,
                                                "firstName", "Mike",
                                                "lastName", "Johnson",
                                                "phone", "+1-555-0103"
                                        )
                                )),
                                Map.entry("customers", List.of()),
                                Map.entry("asset", Map.of(
                                        "id", 401,
                                        "name", "Elevator #1",
                                        "customId", "AST-ELEV-001",
                                        "locationId", 778
                                )),
                                Map.entry("files", List.of()),
                                Map.entry("image", "")
                        )),
                        Map.entry("occurredAt", "2026-04-07T11:30:00Z"),
                        Map.entry("companyId", 1001)
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
                                "barcode", "FLT-HEPA-001"
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
                        "changedPart", Map.of(
                                "id", 225,
                                "name", "Lubricant Oil (1L)",
                                "quantity", 35.0,
                                "barcode", "LUB-OIL-1L"
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
                                "barcode", "FLT-HEPA-001",
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
                                "barcode", "BRG-6205",
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
                                "barcode", "FLT-HEPA-001"
                        ),
                        "occurredAt", "2026-04-07T15:00:00Z",
                        "companyId", 1001
                ),
                Map.of(
                        "partId", 502,
                        "deletePart", Map.of(
                                "id", 502,
                                "name", "Thermostat Unit T-200",
                                "barcode", "THM-T200"
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
                                "location", Map.of(
                                        "id", 783,
                                        "name", "Warehouse 1",
                                        "address", "Industrial Park, Section A",
                                        "customId", "LOC-WH-001"
                                )
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
                                "location", Map.of(
                                        "id", 784,
                                        "name", "Utility Room B",
                                        "address", "Building B, Basement",
                                        "customId", "LOC-UB-002",
                                        "parentId", 772
                                )
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
                        Map.entry("triggeredWorkOrder", Map.ofEntries(
                                Map.entry("id", 12500),
                                Map.entry("title", "Motor Overheating Investigation"),
                                Map.entry("status", "OPEN"),
                                Map.entry("priority", "HIGH"),
                                Map.entry("description", "Investigate motor overheating issue"),
                                Map.entry("dueDate", "2026-04-14T17:00:00Z"),
                                Map.entry("estimatedDuration", 3.0),
                                Map.entry("requiredSignature", true),
                                Map.entry("category", Map.of(
                                        "id", 561,
                                        "name", "Emergency Maintenance"
                                )),
                                Map.entry("location", Map.ofEntries(
                                        Map.entry("id", 789),
                                        Map.entry("name", "Motor Room A"),
                                        Map.entry("address", "Building E, Floor 1"),
                                        Map.entry("customId", "LOC-MR-011"),
                                        Map.entry("parentId", "")
                                )),
                                Map.entry("team", Map.ofEntries(
                                        Map.entry("id", 304),
                                        Map.entry("name", "Emergency Response Team"),
                                        Map.entry("users", List.of(
                                                Map.ofEntries(
                                                        Map.entry("id", 1009),
                                                        Map.entry("firstName", "David"),
                                                        Map.entry("lastName", "Lee"),
                                                        Map.entry("phone", "+1-555-0109"),
                                                        Map.entry("image", "")
                                                )
                                        ))
                                )),
                                Map.entry("primaryUser", Map.ofEntries(
                                        Map.entry("id", 1009),
                                        Map.entry("firstName", "David"),
                                        Map.entry("lastName", "Lee"),
                                        Map.entry("phone", "+1-555-0109"),
                                        Map.entry("image", "")
                                )),
                                Map.entry("assignedTo", List.of(
                                        Map.ofEntries(
                                                Map.entry("id", 1009),
                                                Map.entry("firstName", "David"),
                                                Map.entry("lastName", "Lee"),
                                                Map.entry("phone", "+1-555-0109"),
                                                Map.entry("image", "")
                                        )
                                )),
                                Map.entry("customers", List.of()),
                                Map.entry("asset", Map.ofEntries(
                                        Map.entry("id", 404),
                                        Map.entry("name", "Motor M-500"),
                                        Map.entry("customId", "AST-MOT-001"),
                                        Map.entry("parentId", ""),
                                        Map.entry("locationId", 789)
                                )),
                                Map.entry("files", List.of()),
                                Map.entry("image", "")
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
                        Map.entry("triggeredWorkOrder", Map.ofEntries(
                                Map.entry("id", 12510),
                                Map.entry("title", "Hydraulic System Inspection"),
                                Map.entry("status", "OPEN"),
                                Map.entry("priority", "MEDIUM"),
                                Map.entry("description", "Inspect hydraulic system for leaks"),
                                Map.entry("dueDate", "2026-04-18T17:00:00Z"),
                                Map.entry("estimatedDuration", 4.0),
                                Map.entry("requiredSignature", false),
                                Map.entry("category", Map.of(
                                        "id", 562,
                                        "name", "Inspection"
                                )),
                                Map.entry("location", Map.ofEntries(
                                        Map.entry("id", 790),
                                        Map.entry("name", "Hydraulic Station"),
                                        Map.entry("address", "Building F, Section B"),
                                        Map.entry("customId", "LOC-HS-012"),
                                        Map.entry("parentId", "")
                                )),
                                Map.entry("team", ""),
                                Map.entry("primaryUser", Map.ofEntries(
                                        Map.entry("id", 1010),
                                        Map.entry("firstName", "Emily"),
                                        Map.entry("lastName", "Clark"),
                                        Map.entry("phone", "+1-555-0110"),
                                        Map.entry("image", "")
                                )),
                                Map.entry("assignedTo", List.of(
                                        Map.ofEntries(
                                                Map.entry("id", 1010),
                                                Map.entry("firstName", "Emily"),
                                                Map.entry("lastName", "Clark"),
                                                Map.entry("phone", "+1-555-0110"),
                                                Map.entry("image", "")
                                        )
                                )),
                                Map.entry("customers", List.of()),
                                Map.entry("asset", Map.ofEntries(
                                        Map.entry("id", 405),
                                        Map.entry("name", "Hydraulic Pump HP-200"),
                                        Map.entry("customId", "AST-HYD-001"),
                                        Map.entry("parentId", ""),
                                        Map.entry("locationId", 790)
                                )),
                                Map.entry("files", List.of()),
                                Map.entry("image", "")
                        )),
                        Map.entry("occurredAt", "2026-04-06T18:00:00Z"),
                        Map.entry("companyId", 1001)
                )
        );
    }

    private List<Map<String, Object>> newCategoryOnWorkOrderSamples() {
        return List.of(
                Map.ofEntries(
                        Map.entry("workOrderId", 12345),
                        Map.entry("workOrderTitle", "HVAC System Maintenance"),
                        Map.entry("previousCategoryId", ""),
                        Map.entry("newCategoryId", 555),
                        Map.entry("newCategoryName", "Preventive Maintenance"),
                        Map.entry("changedWorkOrder", Map.ofEntries(
                                Map.entry("id", 12345),
                                Map.entry("title", "HVAC System Maintenance"),
                                Map.entry("status", "IN_PROGRESS"),
                                Map.entry("priority", "HIGH"),
                                Map.entry("description", "Quarterly maintenance check for HVAC unit"),
                                Map.entry("category", Map.of(
                                        "id", 555,
                                        "name", "Preventive Maintenance"
                                )),
                                Map.entry("location", Map.of(
                                        "id", 777,
                                        "name", "Production Floor",
                                        "address", "Building A, Floor 1",
                                        "customId", "LOC-PF-001"
                                )),
                                Map.entry("team", Map.of(
                                        "id", 301,
                                        "name", "Maintenance Team Alpha",
                                        "users", List.of()
                                )),
                                Map.entry("primaryUser", Map.of(
                                        "id", 1001,
                                        "firstName", "John",
                                        "lastName", "Smith",
                                        "phone", "+1-555-0101"
                                )),
                                Map.entry("assignedTo", List.of()),
                                Map.entry("customers", List.of()),
                                Map.entry("asset", ""),
                                Map.entry("files", List.of()),
                                Map.entry("image", "")
                        )),
                        Map.entry("occurredAt", "2026-04-07T09:00:00Z"),
                        Map.entry("companyId", 1001)
                ),
                Map.ofEntries(
                        Map.entry("workOrderId", 12600),
                        Map.entry("workOrderTitle", "Electrical Panel Upgrade"),
                        Map.entry("previousCategoryId", 100),
                        Map.entry("newCategoryId", 200),
                        Map.entry("newCategoryName", "Corrective Maintenance"),
                        Map.entry("changedWorkOrder", Map.ofEntries(
                                Map.entry("id", 12600),
                                Map.entry("title", "Electrical Panel Upgrade"),
                                Map.entry("status", "OPEN"),
                                Map.entry("priority", "HIGH"),
                                Map.entry("description", "Upgrade electrical panel to meet new codes"),
                                Map.entry("category", Map.of(
                                        "id", 200,
                                        "name", "Corrective Maintenance"
                                )),
                                Map.entry("location", Map.of(
                                        "id", 782,
                                        "name", "Utility Room A",
                                        "address", "Building D, Basement",
                                        "customId", "LOC-UR-006",
                                        "parentId", 773
                                )),
                                Map.entry("team", ""),
                                Map.entry("primaryUser", Map.of(
                                        "id", 1008,
                                        "firstName", "Tom",
                                        "lastName", "Brown",
                                        "phone", "+1-555-0108"
                                )),
                                Map.entry("assignedTo", List.of(
                                        Map.of(
                                                "id", 1008,
                                                "firstName", "Tom",
                                                "lastName", "Brown",
                                                "phone", "+1-555-0108"
                                        )
                                )),
                                Map.entry("customers", List.of()),
                                Map.entry("asset", Map.of(
                                        "id", 403,
                                        "name", "Electrical Panel EP-100",
                                        "customId", "AST-ELEC-001",
                                        "locationId", 782
                                )),
                                Map.entry("files", List.of()),
                                Map.entry("image", "")
                        )),
                        Map.entry("occurredAt", "2026-04-07T11:00:00Z"),
                        Map.entry("companyId", 1001)
                )
        );
    }

    private List<Map<String, Object>> newWorkOrderSamples() {
        return List.of(
                Map.ofEntries(
                        Map.entry("workOrderId", 12700),
                        Map.entry("workOrderTitle", "Fire Suppression System Test"),
                        Map.entry("newWorkOrder", Map.ofEntries(
                                Map.entry("id", 12700),
                                Map.entry("title", "Fire Suppression System Test"),
                                Map.entry("status", "OPEN"),
                                Map.entry("priority", "HIGH"),
                                Map.entry("description", "Annual fire suppression system testing and inspection"),
                                Map.entry("dueDate", "2026-04-30T17:00:00Z"),
                                Map.entry("estimatedDuration", 6.0),
                                Map.entry("estimatedStartDate", "2026-04-10T08:00:00Z"),
                                Map.entry("requiredSignature", true),
                                Map.entry("category", Map.of(
                                        "id", 557,
                                        "name", "Safety Maintenance"
                                )),
                                Map.entry("location", Map.of(
                                        "id", 779,
                                        "name", "Building A - All Floors",
                                        "address", "123 Main Street",
                                        "customId", "LOC-BA-003",
                                        "parentId", 770
                                )),
                                Map.entry("team", Map.of(
                                        "id", 303,
                                        "name", "Safety Team",
                                        "users", List.of(
                                                Map.of(
                                                        "id", 1004,
                                                        "firstName", "Sarah",
                                                        "lastName", "Davis",
                                                        "phone", "+1-555-0104"
                                                )
                                        )
                                )),
                                Map.entry("primaryUser", Map.of(
                                        "id", 1004,
                                        "firstName", "Sarah",
                                        "lastName", "Davis",
                                        "phone", "+1-555-0104"
                                )),
                                Map.entry("assignedTo", List.of(
                                        Map.of(
                                                "id", 1004,
                                                "firstName", "Sarah",
                                                "lastName", "Davis",
                                                "phone", "+1-555-0104"
                                        ),
                                        Map.of(
                                                "id", 1005,
                                                "firstName", "Robert",
                                                "lastName", "Wilson",
                                                "phone", "+1-555-0105"
                                        )
                                )),
                                Map.entry("customers", List.of(
                                        Map.of(
                                                "id", 2002,
                                                "name", "Building Management Co."
                                        )
                                )),
                                Map.entry("asset", Map.of(
                                        "id", 402,
                                        "name", "Fire Suppression System",
                                        "customId", "AST-FIRE-001",
                                        "locationId", 779
                                )),
                                Map.entry("files", List.of()),
                                Map.entry("image", "")
                        )),
                        Map.entry("occurredAt", "2026-04-07T08:00:00Z"),
                        Map.entry("companyId", 1001)
                ),
                Map.ofEntries(
                        Map.entry("workOrderId", 12701),
                        Map.entry("workOrderTitle", "Roof Leak Repair - Building C"),
                        Map.entry("newWorkOrder", Map.ofEntries(
                                Map.entry("id", 12701),
                                Map.entry("title", "Roof Leak Repair - Building C"),
                                Map.entry("status", "OPEN"),
                                Map.entry("priority", "MEDIUM"),
                                Map.entry("description", "Repair roof leak in the northwest corner"),
                                Map.entry("dueDate", "2026-04-25T17:00:00Z"),
                                Map.entry("estimatedDuration", 3.0),
                                Map.entry("estimatedStartDate", "2026-04-12T09:00:00Z"),
                                Map.entry("requiredSignature", false),
                                Map.entry("category", Map.of(
                                        "id", 558,
                                        "name", "Corrective Maintenance"
                                )),
                                Map.entry("location", Map.of(
                                        "id", 780,
                                        "name", "Building C - Roof",
                                        "address", "456 Oak Avenue",
                                        "customId", "LOC-BC-004",
                                        "parentId", 771
                                )),
                                Map.entry("team", ""),
                                Map.entry("primaryUser", Map.of(
                                        "id", 1006,
                                        "firstName", "Mike",
                                        "lastName", "Johnson",
                                        "phone", "+1-555-0106"
                                )),
                                Map.entry("assignedTo", List.of(
                                        Map.of(
                                                "id", 1006,
                                                "firstName", "Mike",
                                                "lastName", "Johnson",
                                                "phone", "+1-555-0106"
                                        )
                                )),
                                Map.entry("customers", List.of()),
                                Map.entry("asset", ""),
                                Map.entry("files", List.of(
                                        Map.of(
                                                "id", 6003,
                                                "name", "roof_damage_photo.jpg",
                                                "url", "https://example.com/files/6003"
                                        )
                                )),
                                Map.entry("image", "")
                        )),
                        Map.entry("occurredAt", "2026-04-07T09:30:00Z"),
                        Map.entry("companyId", 1001)
                )
        );
    }

    private List<Map<String, Object>> workOrderChangeSamples() {
        return List.of(
                Map.ofEntries(
                        Map.entry("workOrderId", 12345),
                        Map.entry("workOrderTitle", "HVAC System Maintenance"),
                        Map.entry("changedWorkOrder", Map.ofEntries(
                                Map.entry("id", 12345),
                                Map.entry("title", "HVAC System Maintenance"),
                                Map.entry("status", "IN_PROGRESS"),
                                Map.entry("priority", "HIGH"),
                                Map.entry("description", "Quarterly maintenance check with filter replacement"),
                                Map.entry("dueDate", "2026-04-15T17:00:00Z"),
                                Map.entry("estimatedDuration", 4.0),
                                Map.entry("requiredSignature", true),
                                Map.entry("category", Map.of(
                                        "id", 555,
                                        "name", "Preventive Maintenance"
                                )),
                                Map.entry("location", Map.of(
                                        "id", 777,
                                        "name", "Production Floor",
                                        "address", "Building A, Floor 1",
                                        "customId", "LOC-PF-001"
                                )),
                                Map.entry("team", Map.of(
                                        "id", 301,
                                        "name", "Maintenance Team Alpha",
                                        "users", List.of()
                                )),
                                Map.entry("primaryUser", Map.of(
                                        "id", 1001,
                                        "firstName", "John",
                                        "lastName", "Smith",
                                        "phone", "+1-555-0101"
                                )),
                                Map.entry("assignedTo", List.of(
                                        Map.of(
                                                "id", 1001,
                                                "firstName", "John",
                                                "lastName", "Smith",
                                                "phone", "+1-555-0101"
                                        )
                                )),
                                Map.entry("customers", List.of()),
                                Map.entry("asset", Map.of(
                                        "id", 400,
                                        "name", "HVAC Unit #3",
                                        "customId", "AST-HVAC-003",
                                        "locationId", 777
                                )),
                                Map.entry("files", List.of()),
                                Map.entry("image", "")
                        )),
                        Map.entry("occurredAt", "2026-04-07T10:15:00Z"),
                        Map.entry("companyId", 1001)
                ),
                Map.ofEntries(
                        Map.entry("workOrderId", 12800),
                        Map.entry("workOrderTitle", "Plumbing Inspection - Floor 2"),
                        Map.entry("changedWorkOrder", Map.ofEntries(
                                Map.entry("id", 12800),
                                Map.entry("title", "Plumbing Inspection - Floor 2"),
                                Map.entry("status", "OPEN"),
                                Map.entry("priority", "LOW"),
                                Map.entry("description", "Inspect plumbing fixtures and check for leaks"),
                                Map.entry("dueDate", "2026-04-28T17:00:00Z"),
                                Map.entry("estimatedDuration", 2.5),
                                Map.entry("requiredSignature", false),
                                Map.entry("category", Map.of(
                                        "id", 559,
                                        "name", "Inspection"
                                )),
                                Map.entry("location", Map.of(
                                        "id", 781,
                                        "name", "Building B - Floor 2",
                                        "address", "789 Elm Street",
                                        "customId", "LOC-BB-005",
                                        "parentId", 772
                                )),
                                Map.entry("team", ""),
                                Map.entry("primaryUser", Map.of(
                                        "id", 1007,
                                        "firstName", "Sarah",
                                        "lastName", "Davis",
                                        "phone", "+1-555-0107"
                                )),
                                Map.entry("assignedTo", List.of(
                                        Map.of(
                                                "id", 1007,
                                                "firstName", "Sarah",
                                                "lastName", "Davis",
                                                "phone", "+1-555-0107"
                                        )
                                )),
                                Map.entry("customers", List.of()),
                                Map.entry("asset", ""),
                                Map.entry("files", List.of()),
                                Map.entry("image", "")
                        )),
                        Map.entry("occurredAt", "2026-04-07T14:30:00Z"),
                        Map.entry("companyId", 1001)
                )
        );
    }

    private List<Map<String, Object>> workOrderDeleteSamples() {
        return List.of(
                Map.ofEntries(
                        Map.entry("workOrderId", 12900),
                        Map.entry("workOrderTitle", "Duplicate Entry - Desk Repair"),
                        Map.entry("deleteWorkOrder", Map.ofEntries(
                                Map.entry("id", 12900),
                                Map.entry("title", "Duplicate Entry - Desk Repair"),
                                Map.entry("status", "OPEN"),
                                Map.entry("priority", "LOW"),
                                Map.entry("description", "Duplicate work order for desk repair"),
                                Map.entry("category", Map.of(
                                        "id", 560,
                                        "name", "General Maintenance"
                                )),
                                Map.entry("location", Map.of(
                                        "id", 787,
                                        "name", "Office Area - Floor 1",
                                        "address", "Building A",
                                        "customId", "LOC-OA-009"
                                )),
                                Map.entry("team", ""),
                                Map.entry("primaryUser", ""),
                                Map.entry("assignedTo", List.of()),
                                Map.entry("customers", List.of()),
                                Map.entry("asset", ""),
                                Map.entry("files", List.of()),
                                Map.entry("image", "")
                        )),
                        Map.entry("occurredAt", "2026-04-07T16:00:00Z"),
                        Map.entry("companyId", 1001)
                ),
                Map.ofEntries(
                        Map.entry("workOrderId", 12901),
                        Map.entry("workOrderTitle", "Cancelled - Paint Touch-up"),
                        Map.entry("deleteWorkOrder", Map.ofEntries(
                                Map.entry("id", 12901),
                                Map.entry("title", "Cancelled - Paint Touch-up"),
                                Map.entry("status", "CANCELLED"),
                                Map.entry("priority", "NONE"),
                                Map.entry("description", "Paint touch-up work that was cancelled"),
                                Map.entry("category", ""),
                                Map.entry("location", Map.of(
                                        "id", 788,
                                        "name", "Hallway B",
                                        "address", "Building B, Floor 1",
                                        "customId", "LOC-HB-010",
                                        "parentId", 772
                                )),
                                Map.entry("team", ""),
                                Map.entry("primaryUser", ""),
                                Map.entry("assignedTo", List.of()),
                                Map.entry("customers", List.of()),
                                Map.entry("asset", ""),
                                Map.entry("files", List.of()),
                                Map.entry("image", "")
                        )),
                        Map.entry("occurredAt", "2026-04-06T12:00:00Z"),
                        Map.entry("companyId", 1001)
                )
        );
    }

    private List<Map<String, Object>> newAssetSamples() {
        return List.of(
                Map.ofEntries(
                        Map.entry("assetId", 400),
                        Map.entry("assetName", "CNC Machine #5"),
                        Map.entry("newAsset", Map.ofEntries(
                                Map.entry("id", 400),
                                Map.entry("name", "CNC Machine #5"),
                                Map.entry("status", "OPERATIONAL"),
                                Map.entry("location", Map.of(
                                        "id", 785,
                                        "name", "Production Floor",
                                        "address", "Building C, Floor 1",
                                        "customId", "LOC-PF-007"
                                )),
                                Map.entry("category", "Manufacturing Equipment"),
                                Map.entry("customId", "AST-CNC-005"),
                                Map.entry("parentId", ""),
                                Map.entry("locationId", 785)
                        )),
                        Map.entry("occurredAt", "2026-04-07T08:30:00Z"),
                        Map.entry("companyId", 1001)
                ),
                Map.ofEntries(
                        Map.entry("assetId", 401),
                        Map.entry("assetName", "Air Compressor AC-300"),
                        Map.entry("newAsset", Map.ofEntries(
                                Map.entry("id", 401),
                                Map.entry("name", "Air Compressor AC-300"),
                                Map.entry("status", "OPERATIONAL"),
                                Map.entry("location", Map.of(
                                        "id", 786,
                                        "name", "Workshop 2",
                                        "address", "Building D, Section A",
                                        "customId", "LOC-WS-008",
                                        "parentId", 773
                                )),
                                Map.entry("category", "Compressed Air System"),
                                Map.entry("customId", "AST-AC-300"),
                                Map.entry("parentId", ""),
                                Map.entry("locationId", 786)
                        )),
                        Map.entry("occurredAt", "2026-04-05T10:00:00Z"),
                        Map.entry("companyId", 1001)
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
                                "barcode", "VLV-SV50",
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
                                "barcode", "BLT-DB220",
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
                                "address", "123 Main Street, Second Floor",
                                "customId", "LOC-BA-F2-001",
                                "parentId", 770
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
                                "address", "456 Industrial Blvd, Section C",
                                "customId", "LOC-WH-CS-002",
                                "parentId", 771
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
