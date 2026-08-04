package com.grash.utils.csv;

import com.grash.model.Asset;
import com.grash.model.Customer;
import com.grash.model.Location;
import com.grash.model.User;
import com.grash.model.WorkOrder;
import com.grash.model.WorkOrderCategory;
import com.grash.model.enums.Priority;
import com.grash.model.enums.Status;
import com.grash.utils.CsvFileGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;

import java.io.StringWriter;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the contract of the export column registry.
 * <p>
 * The first test is the important one: the unfiltered export must keep producing exactly the
 * column list it always did. It caught a real regression when the registry was introduced —
 * {@code writeWorkOrdersToCsv} handed out every registered column, so the legacy file silently
 * gained nine, which is why default and opt-in columns are now separate.
 * <p>
 * Deliberately mock-free: it constructs the collaborators directly, so it also runs on JDKs
 * that the project's Byte Buddy is too old to instrument. The nulls are safe because the
 * default work-order column set touches neither {@code AssetDowntimeService} nor
 * {@code UserService} — if that stops being true, this test will NPE rather than mislead.
 */
class CsvColumnRegistriesTest {

    private final MessageSource echo = new MessageSource() {
        @Override
        public String getMessage(String code, Object[] args, String defaultMessage, Locale locale) {
            return code;
        }

        @Override
        public String getMessage(String code, Object[] args, Locale locale) throws NoSuchMessageException {
            return code;
        }

        @Override
        public String getMessage(MessageSourceResolvable resolvable, Locale locale) {
            return resolvable.getCodes()[0];
        }
    };

    @Test
    void workOrderHeaderMatchesLegacyList() {
        CsvColumnRegistries registries = new CsvColumnRegistries(echo, null, null);
        CsvFileGenerator generator = new CsvFileGenerator(echo, null, null, null, null, registries);

        WorkOrder wo = new WorkOrder();
        wo.setId(7L);
        wo.setTitle("Pumpe tauschen");
        wo.setStatus(Status.IN_PROGRESS);
        wo.setPriority(Priority.HIGH);
        wo.setDueDate(new Date(0));
        wo.setEstimatedDuration(2.5);
        WorkOrderCategory category = new WorkOrderCategory();
        category.setName("HVAC");
        wo.setCategory(category);
        Location location = new Location();
        location.setName("Gebaeude B");
        wo.setLocation(location);
        Asset asset = new Asset();
        asset.setName("RLT-01");
        wo.setAsset(asset);
        User assignee = new User();
        assignee.setEmail("a@test.de");
        wo.getAssignedTo().add(assignee);
        Customer customer = new Customer();
        customer.setName("Kunde");
        wo.getCustomers().add(customer);

        StringWriter writer = new StringWriter();
        generator.writeWorkOrdersToCsv(List.of(wo), writer, Locale.ENGLISH, ",", true);
        String[] lines = writer.toString().split("\r?\n");

        assertEquals("ID,Title,Status,Priority,Description,Due_Date,Estimated_Duration,Requires_Signature,Category,"
                        + "Location_Name,Team_Name,Primary_User_Email,Assigned_To_Emails,Asset_Name,"
                        + "Completed_By_Email,Completed_On,Archived,Feedback,Customers,Created_At",
                lines[0], "header row must match the pre-refactor column list exactly");
        assertTrue(lines[1].startsWith("7,Pumpe tauschen,IN_PROGRESS,HIGH,"), "row was: " + lines[1]);
        assertTrue(lines[1].contains("HVAC"));
        assertTrue(lines[1].contains("Gebaeude B"));
        assertTrue(lines[1].contains("RLT-01"));
        assertTrue(lines[1].contains("a@test.de"));
        assertTrue(lines[1].contains("Kunde"));
    }

    @Test
    void selectedColumnsAreWrittenInRequestedOrder() {
        CsvColumnRegistries registries = new CsvColumnRegistries(echo, null, null);
        CsvFileGenerator generator = new CsvFileGenerator(echo, null, null, null, null, registries);

        WorkOrder wo = new WorkOrder();
        wo.setId(9L);
        wo.setTitle("Filter wechseln");
        wo.setCustomId("WO-9");

        StringWriter writer = new StringWriter();
        generator.writeToCsv(List.of(wo), registries.workOrders(Locale.ENGLISH)
                .resolve(List.of("customId", "title", "id")), writer, ",", true);
        String[] lines = writer.toString().split("\r?\n");

        assertEquals("Custom_ID,Title,ID", lines[0]);
        assertEquals("WO-9,Filter wechseln,9", lines[1]);
    }

    /**
     * The asset equivalent of the first test, checked at the header level only: the asset
     * default set includes the downtime column, whose extractor would need a real
     * AssetDowntimeService. Resolving the columns does not call extractors, so the guard against
     * the default set silently widening still works.
     */
    @Test
    void assetDefaultColumnsMatchLegacyList() {
        CsvColumnRegistries registries = new CsvColumnRegistries(echo, null, null);

        List<String> headers = registries.assets(Locale.ENGLISH).all().stream()
                .map(CsvColumn::header)
                .toList();

        assertEquals(List.of("ID", "Name", "Description", "Status", "Archived", "Location_Name", "Parent_Asset",
                        "Area", "Barcode", "Category", "Primary_User_Email", "Warranty_Expiration_Date",
                        "Additional_Information", "Serial_Number", "Assigned_To_Emails", "Teams_Names", "Parts",
                        "Vendors", "Customers", "Downtime_Duration"),
                headers, "asset default column set must match the pre-refactor list exactly");
    }

    @Test
    void unknownColumnIsRejected() {
        CsvColumnRegistries registries = new CsvColumnRegistries(echo, null, null);
        try {
            registries.workOrders(Locale.ENGLISH).resolve(List.of("title", "nope"));
            throw new AssertionError("expected a rejection for the unknown key");
        } catch (com.grash.exception.CustomException e) {
            assertTrue(e.getMessage().contains("nope"), e.getMessage());
        }
    }
}
