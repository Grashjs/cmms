package com.grash.utils.csv;

import com.grash.model.Asset;
import com.grash.model.AssetDowntime;
import com.grash.model.Customer;
import com.grash.model.Part;
import com.grash.model.Team;
import com.grash.model.User;
import com.grash.model.Vendor;
import com.grash.model.WorkOrder;
import com.grash.service.AssetDowntimeService;
import com.grash.service.UserService;
import com.grash.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The exportable column sets, built per request because the headers are translated.
 * <p>
 * The first block of each registry — up to and including {@code createdAt} for work orders —
 * is the column list the unfiltered export has always produced, in the same order and with
 * the same value expressions. That is deliberate: the legacy {@code /export/*} endpoints now
 * go through this registry, and their output must not change. Columns after that block are
 * additions only a caller that asks for them by name receives.
 * <p>
 * Only work orders and assets are covered. The other five exports still use the hand-written
 * writers in {@code CsvFileGenerator}; adding one is a registry, not a redesign — see
 * docs/reporting.md.
 */
@Component
@RequiredArgsConstructor
public class CsvColumnRegistries {

    private final MessageSource messageSource;
    private final AssetDowntimeService assetDowntimeService;
    private final UserService userService;

    public CsvColumnRegistry<WorkOrder> workOrders(Locale locale) {
        CsvColumnRegistry<WorkOrder> registry = new CsvColumnRegistry<>();
        // --- the legacy default set, order-critical ---
        add(registry, "id", "ID", locale, WorkOrder::getId);
        add(registry, "title", "Title", locale, WorkOrder::getTitle);
        add(registry, "status", "Status", locale, wo -> wo.getStatus() == null ? null
                : messageSource.getMessage(wo.getStatus().toString(), null, locale));
        add(registry, "priority", "Priority", locale, wo -> wo.getPriority() == null ? null
                : messageSource.getMessage(wo.getPriority().toString(), null, locale));
        add(registry, "description", "Description", locale, WorkOrder::getDescription);
        add(registry, "dueDate", "Due_Date", locale, WorkOrder::getDueDate);
        add(registry, "estimatedDuration", "Estimated_Duration", locale, WorkOrder::getEstimatedDuration);
        add(registry, "requiredSignature", "Requires_Signature", locale,
                wo -> Helper.getStringFromBoolean(wo.isRequiredSignature(), messageSource, locale));
        add(registry, "category", "Category", locale,
                wo -> wo.getCategory() == null ? null : wo.getCategory().getName());
        add(registry, "location", "Location_Name", locale,
                wo -> wo.getLocation() == null ? null : wo.getLocation().getName());
        add(registry, "team", "Team_Name", locale, wo -> wo.getTeam() == null ? null : wo.getTeam().getName());
        add(registry, "primaryUser", "Primary_User_Email", locale,
                wo -> wo.getPrimaryUser() == null ? null : wo.getPrimaryUser().getEmail());
        add(registry, "assignedTo", "Assigned_To_Emails", locale,
                wo -> Helper.enumerate(wo.getAssignedTo().stream().map(User::getEmail).collect(Collectors.toList())));
        add(registry, "asset", "Asset_Name", locale, wo -> wo.getAsset() == null ? null : wo.getAsset().getName());
        add(registry, "completedBy", "Completed_By_Email", locale,
                wo -> wo.getCompletedBy() == null ? null : wo.getCompletedBy().getEmail());
        add(registry, "completedOn", "Completed_On", locale, WorkOrder::getCompletedOn);
        add(registry, "archived", "Archived", locale,
                wo -> Helper.getStringFromBoolean(wo.isArchived(), messageSource, locale));
        add(registry, "feedback", "Feedback", locale, WorkOrder::getFeedback);
        add(registry, "customers", "Customers", locale,
                wo -> Helper.enumerate(wo.getCustomers().stream().map(Customer::getName).collect(Collectors.toList())));
        add(registry, "createdAt", "Created_At", locale, WorkOrder::getCreatedAt);
        // --- opt-in additions: selectable by name, never part of the default file ---
        addOptional(registry, "customId", "Custom_ID", locale, WorkOrder::getCustomId);
        addOptional(registry, "updatedAt", "Updated_At", locale, WorkOrder::getUpdatedAt);
        addOptional(registry, "estimatedStartDate", "Estimated_Start_Date", locale,
                WorkOrder::getEstimatedStartDate);
        addOptional(registry, "firstTimeToReact", "First_Time_To_React", locale, WorkOrder::getFirstTimeToReact);
        addOptional(registry, "assetCustomId", "Asset_Custom_ID", locale,
                wo -> wo.getAsset() == null ? null : wo.getAsset().getCustomId());
        // These four exist only because the work-order list shows them as columns. "Export the
        // columns I see" has to be able to export every one of them, or it quietly produces a
        // narrower file than the screen — see CsvColumnRegistry.resolve.
        addOptional(registry, "locationAddress", "Address", locale,
                wo -> wo.getLocation() == null ? null : wo.getLocation().getAddress());
        addOptional(registry, "daysSinceCreated", "Days_Since_Creation", locale, this::daysSinceCreated);
        addOptional(registry, "files", "Files", locale, wo -> wo.getFiles() == null ? 0 : wo.getFiles().size());
        addOptional(registry, "requestedBy", "Requested_By", locale, this::requesterName);
        return registry;
    }

    public CsvColumnRegistry<Asset> assets(Locale locale) {
        CsvColumnRegistry<Asset> registry = new CsvColumnRegistry<>();
        // --- the legacy default set, order-critical ---
        add(registry, "id", "ID", locale, Asset::getId);
        add(registry, "name", "Name", locale, Asset::getName);
        add(registry, "description", "Description", locale, Asset::getDescription);
        add(registry, "status", "Status", locale, asset -> asset.getStatus() == null ? null
                : messageSource.getMessage(asset.getStatus().toString(), null, locale));
        add(registry, "archived", "Archived", locale,
                asset -> Helper.getStringFromBoolean(asset.isArchived(), messageSource, locale));
        add(registry, "location", "Location_Name", locale,
                asset -> asset.getLocation() == null ? null : asset.getLocation().getName());
        add(registry, "parentAsset", "Parent_Asset", locale,
                asset -> asset.getParentAsset() == null ? null : asset.getParentAsset().getName());
        add(registry, "area", "Area", locale, Asset::getArea);
        add(registry, "barCode", "Barcode", locale, Asset::getBarCode);
        add(registry, "category", "Category", locale,
                asset -> asset.getCategory() == null ? null : asset.getCategory().getName());
        add(registry, "primaryUser", "Primary_User_Email", locale,
                asset -> asset.getPrimaryUser() == null ? null : asset.getPrimaryUser().getEmail());
        add(registry, "warrantyExpirationDate", "Warranty_Expiration_Date", locale, Asset::getWarrantyExpirationDate);
        add(registry, "additionalInfos", "Additional_Information", locale, Asset::getAdditionalInfos);
        add(registry, "serialNumber", "Serial_Number", locale, Asset::getSerialNumber);
        add(registry, "assignedTo", "Assigned_To_Emails", locale, asset ->
                Helper.enumerate(asset.getAssignedTo().stream().map(User::getEmail).collect(Collectors.toList())));
        add(registry, "teams", "Teams_Names", locale, asset ->
                Helper.enumerate(asset.getTeams().stream().map(Team::getName).collect(Collectors.toList())));
        add(registry, "parts", "Parts", locale, asset ->
                Helper.enumerate(asset.getParts().stream().map(Part::getName).collect(Collectors.toList())));
        add(registry, "vendors", "Vendors", locale, asset ->
                Helper.enumerate(asset.getVendors().stream().map(Vendor::getName).collect(Collectors.toList())));
        add(registry, "customers", "Customers", locale, asset ->
                Helper.enumerate(asset.getCustomers().stream().map(Customer::getName).collect(Collectors.toList())));
        add(registry, "downtimeDuration", "Downtime_Duration", locale, this::totalDowntimeSeconds);
        // --- opt-in additions: selectable by name, never part of the default file ---
        addOptional(registry, "customId", "Custom_ID", locale, Asset::getCustomId);
        addOptional(registry, "model", "Model", locale, Asset::getModel);
        addOptional(registry, "manufacturer", "Manufacturer", locale, Asset::getManufacturer);
        addOptional(registry, "power", "Power", locale, Asset::getPower);
        addOptional(registry, "inServiceDate", "In_Service_Date", locale, Asset::getInServiceDate);
        addOptional(registry, "acquisitionCost", "Acquisition_Cost", locale, Asset::getAcquisitionCost);
        addOptional(registry, "nfcId", "NFC_ID", locale, Asset::getNfcId);
        addOptional(registry, "createdAt", "Created_At", locale, Asset::getCreatedAt);
        return registry;
    }

    /**
     * Days since creation, matching the list column's {@code dayDiff} helper — which subtracts
     * one, so a work order created today reads -1 on screen. Reproduced rather than corrected:
     * the export exists to put the visible table in a file, and a column that disagrees with
     * the screen by one is worse than one that is consistently odd. Worth fixing in both places
     * together.
     */
    private long daysSinceCreated(WorkOrder workOrder) {
        if (workOrder.getCreatedAt() == null) return 0;
        long days = ChronoUnit.DAYS.between(workOrder.getCreatedAt().toInstant(), Instant.now());
        return Math.abs(days) - 1;
    }

    /**
     * The person who raised the originating request, empty for a work order created directly.
     * One user lookup per row; {@code createdBy} is a plain id, not a relation.
     */
    private String requesterName(WorkOrder workOrder) {
        if (workOrder.getParentRequest() == null || workOrder.getParentRequest().getCreatedBy() == null) {
            return null;
        }
        return userService.findById(workOrder.getParentRequest().getCreatedBy())
                .map(user -> (user.getFirstName() + " " + user.getLastName()).trim())
                .orElse(null);
    }

    /**
     * One query per asset, exactly as the previous inline implementation did. It is an N+1 and
     * it is kept because changing it would change the numbers in a file people already
     * reconcile against; the fix belongs with the wider cost refactor, not here.
     */
    private long totalDowntimeSeconds(Asset asset) {
        Collection<AssetDowntime> downtimes = assetDowntimeService.findByAsset(asset.getId());
        return downtimes.stream().map(AssetDowntime::getDuration).reduce(0L, Long::sum);
    }

    private <T> void add(CsvColumnRegistry<T> registry, String key, String messageKey, Locale locale,
                         Function<T, Object> extractor) {
        registry.add(column(key, messageKey, locale, extractor));
    }

    private <T> void addOptional(CsvColumnRegistry<T> registry, String key, String messageKey, Locale locale,
                                 Function<T, Object> extractor) {
        registry.addOptional(column(key, messageKey, locale, extractor));
    }

    private <T> CsvColumn<T> column(String key, String messageKey, Locale locale, Function<T, Object> extractor) {
        return new CsvColumn<>(key, messageSource.getMessage(messageKey, null, locale), extractor);
    }
}
