package com.grash.utils;

import com.grash.model.Asset;
import com.grash.model.CustomFieldValue;
import com.grash.model.Location;
import com.grash.model.Meter;
import com.grash.model.Part;
import com.grash.model.PreventiveMaintenance;
import com.grash.model.Request;
import com.grash.model.WorkOrder;
import com.grash.model.abstracts.WorkOrderBase;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

import java.util.Collection;

/**
 * Central HTML-sanitization for user-supplied text stored by the API.
 * All free-text fields that end up rendered in a browser (SPA, emails,
 * PDF exports, print views) must go through {@link #cleanText(String)}
 * before being persisted.
 */
public final class Sanitizer {
    private static final Document.OutputSettings CLEAN_TEXT_OUTPUT_SETTINGS =
            new Document.OutputSettings().prettyPrint(false);

    /**
     * Strips all HTML markup from the input while keeping its readable text
     * (newlines preserved). Null-safe. The result never contains tags, so it is
     * safe to store as-is; rendering layers remain responsible for contextual
     * output-encoding (defense in depth).
     */
    public static String cleanText(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        return Jsoup.clean(
                input,
                "",
                Safelist.none(),
                CLEAN_TEXT_OUTPUT_SETTINGS
        );
    }

    private static void sanitizeWorkOrderBase(WorkOrderBase workOrderBase) {
        workOrderBase.setTitle(cleanText(workOrderBase.getTitle()));
        workOrderBase.setDescription(cleanText(workOrderBase.getDescription()));
        sanitizeCustomFieldValues(workOrderBase.getCustomFieldValues());
    }

    private static void sanitizeCustomFieldValues(Collection<CustomFieldValue> customFieldValues) {
        if (customFieldValues == null || customFieldValues.isEmpty()) {
            return;
        }
        customFieldValues.forEach(customFieldValue -> customFieldValue.setValue(cleanText(customFieldValue.getValue())));
    }

    public static void sanitizeRequest(Request request) {
        sanitizeWorkOrderBase(request);
        request.setContact(cleanText(request.getContact()));
        request.setCancellationReason(cleanText(request.getCancellationReason()));
    }

    public static void sanitizeWorkOrder(WorkOrder workOrder) {
        sanitizeWorkOrderBase(workOrder);
        workOrder.setFeedback(cleanText(workOrder.getFeedback()));
    }

    public static void sanitizePreventiveMaintenance(PreventiveMaintenance preventiveMaintenance) {
        sanitizeWorkOrderBase(preventiveMaintenance);
        preventiveMaintenance.setName(cleanText(preventiveMaintenance.getName()));
    }

    public static void sanitizeAsset(Asset asset) {
        asset.setName(cleanText(asset.getName()));
        asset.setDescription(cleanText(asset.getDescription()));
        asset.setArea(cleanText(asset.getArea()));
        asset.setAdditionalInfos(cleanText(asset.getAdditionalInfos()));
        asset.setModel(cleanText(asset.getModel()));
        asset.setPower(cleanText(asset.getPower()));
        asset.setManufacturer(cleanText(asset.getManufacturer()));
        asset.setSerialNumber(cleanText(asset.getSerialNumber()));
        sanitizeCustomFieldValues(asset.getCustomFieldValues());
    }

    public static void sanitizeLocation(Location location) {
        location.setName(cleanText(location.getName()));
        location.setAddress(cleanText(location.getAddress()));
        sanitizeCustomFieldValues(location.getCustomFieldValues());
    }

    public static void sanitizePart(Part part) {
        part.setName(cleanText(part.getName()));
        part.setDescription(cleanText(part.getDescription()));
        part.setArea(cleanText(part.getArea()));
        part.setAdditionalInfos(cleanText(part.getAdditionalInfos()));
        part.setUnit(cleanText(part.getUnit()));
        sanitizeCustomFieldValues(part.getCustomFieldValues());
    }

    public static void sanitizeMeter(Meter meter) {
        meter.setName(cleanText(meter.getName()));
        meter.setUnit(cleanText(meter.getUnit()));
        sanitizeCustomFieldValues(meter.getCustomFieldValues());
    }
}
