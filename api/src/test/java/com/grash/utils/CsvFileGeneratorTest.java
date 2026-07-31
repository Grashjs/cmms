package com.grash.utils;

import com.grash.model.*;
import com.grash.model.enums.RecurrenceType;
import com.grash.service.AdditionalCostService;
import com.grash.service.AssetDowntimeService;
import com.grash.service.LaborService;
import com.grash.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CsvFileGeneratorTest {

    @InjectMocks
    private CsvFileGenerator csvFileGenerator;

    @Mock
    private MessageSource messageSource;
    @Mock
    private AssetDowntimeService assetDowntimeService;
    @Mock
    private AdditionalCostService additionalCostService;
    @Mock
    private LaborService laborService;
    @Mock
    private UserService userService;

    private static final Locale LOCALE = Locale.ENGLISH;
    private static final String CSV_SEPARATOR = ",";

    @BeforeEach
    void setUp() {
        lenient().when(messageSource.getMessage(anyString(), any(), any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private StringWriter createWriter() {
        return new StringWriter();
    }

    private User buildUser(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        return user;
    }

    private WorkOrder buildWorkOrder(Long id) {
        WorkOrder wo = new WorkOrder();
        wo.setId(id);
        wo.setTitle("WO-" + id);
        wo.setDescription("Desc " + id);
        wo.setStatus(com.grash.model.enums.Status.OPEN);
        wo.setPriority(com.grash.model.enums.Priority.NONE);
        wo.setDueDate(new Date());
        wo.setEstimatedDuration(2.5);
        wo.setRequiredSignature(true);
        wo.setArchived(false);
        wo.setFeedback("Good");
        wo.setCreatedAt(new Date());
        wo.setAssignedTo(new ArrayList<>());
        wo.setCustomers(new ArrayList<>());
        return wo;
    }

    private Asset buildAsset(Long id) {
        Asset asset = new Asset();
        asset.setId(id);
        asset.setName("Asset-" + id);
        asset.setDescription("Asset Desc " + id);
        asset.setArchived(false);
        asset.setAssignedTo(new ArrayList<>());
        asset.setTeams(new ArrayList<>());
        asset.setParts(new ArrayList<>());
        asset.setVendors(new ArrayList<>());
        asset.setCustomers(new ArrayList<>());
        return asset;
    }

    private Location buildLocation(Long id) {
        Location loc = new Location();
        loc.setId(id);
        loc.setName("Loc-" + id);
        loc.setAddress("Addr " + id);
        loc.setWorkers(new ArrayList<>());
        loc.setTeams(new ArrayList<>());
        loc.setVendors(new ArrayList<>());
        loc.setCustomers(new ArrayList<>());
        return loc;
    }

    private Part buildPart(Long id) {
        Part part = new Part();
        part.setId(id);
        part.setName("Part-" + id);
        part.setCost(10.0);
        part.setNonStock(false);
        part.setBarcode("BC-" + id);
        part.setDescription("Part Desc " + id);
        part.setQuantity(5);
        part.setAdditionalInfos("Info");
        part.setArea("A1");
        part.setMinQuantity(1);
        part.setAssignedTo(new ArrayList<>());
        part.setCustomers(new ArrayList<>());
        part.setVendors(new ArrayList<>());
        part.setTeams(new ArrayList<>());
        return part;
    }

    private Meter buildMeter(Long id) {
        Meter meter = new Meter();
        meter.setId(id);
        meter.setName("Meter-" + id);
        meter.setUnit("kWh");
        meter.setUpdateFrequency(3600);
        meter.setUsers(new ArrayList<>());
        return meter;
    }

    private PreventiveMaintenance buildPM(Long id) {
        PreventiveMaintenance pm = new PreventiveMaintenance();
        pm.setId(id);
        pm.setTitle("PM-" + id);
        pm.setDescription("PM Desc " + id);
        pm.setEstimatedDuration(1.0);
        pm.setRequiredSignature(false);
        Schedule schedule = new Schedule();
        schedule.setStartsOn(new Date());
        schedule.setFrequency(7);
        schedule.setRecurrenceType(RecurrenceType.WEEKLY);
        pm.setSchedule(schedule);
        return pm;
    }

    private PartTransaction buildPartTransaction(Long id) {
        PartTransaction pt = new PartTransaction();
        pt.setId(id);
        pt.setQuantity(2);
        pt.setDescription("PT Desc " + id);
        pt.setCreatedAt(new Date());
        Part part = new Part();
        part.setId(id);
        part.setName("PT-Part-" + id);
        part.setCost(10.0);
        pt.setPart(part);
        return pt;
    }

    @Nested
    class WriteWorkOrdersToCsv {

        @Test
        void withHeaders_writesHeaderAndRecords() {
            StringWriter writer = createWriter();
            WorkOrder wo = buildWorkOrder(1L);
            wo.getAssignedTo().add(buildUser(1L, "user@test.com"));
            Customer customer = new Customer();
            customer.setName("Test Customer");
            wo.getCustomers().add(customer);

            csvFileGenerator.writeWorkOrdersToCsv(List.of(wo), writer, LOCALE, CSV_SEPARATOR, true);

            String output = writer.toString();
            assertTrue(output.contains("ID"));
            assertTrue(output.contains(wo.getTitle()));
            assertTrue(output.contains(wo.getId().toString()));
        }

        @Test
        void withoutHeaders_skipsHeaderRow() {
            StringWriter writer = createWriter();
            WorkOrder wo = buildWorkOrder(1L);

            csvFileGenerator.writeWorkOrdersToCsv(List.of(wo), writer, LOCALE, CSV_SEPARATOR, false);

            String output = writer.toString();
            assertFalse(output.startsWith("ID"));
            assertTrue(output.contains(wo.getId().toString()));
        }

        @Test
        void emptyCollection_writesNothing() {
            StringWriter writer = createWriter();

            csvFileGenerator.writeWorkOrdersToCsv(Collections.emptyList(), writer, LOCALE, CSV_SEPARATOR, false);

            assertTrue(writer.toString().isEmpty());
        }

        @Test
        void nullFields_handlesGracefully() {
            StringWriter writer = createWriter();
            WorkOrder wo = buildWorkOrder(1L);
            wo.setStatus(null);
            wo.setPriority(null);
            wo.setCategory(null);
            wo.setLocation(null);
            wo.setTeam(null);
            wo.setPrimaryUser(null);
            wo.setCompletedBy(null);

            csvFileGenerator.writeWorkOrdersToCsv(List.of(wo), writer, LOCALE, CSV_SEPARATOR, false);

            String output = writer.toString();
            assertTrue(output.contains(wo.getId().toString()));
        }
    }

    @Nested
    class WriteAssetsToCsv {

        @Test
        void withHeaders_writesHeaderAndRecords() {
            StringWriter writer = createWriter();
            Asset asset = buildAsset(1L);
            when(assetDowntimeService.findByAsset(asset.getId())).thenReturn(Collections.emptyList());

            csvFileGenerator.writeAssetsToCsv(List.of(asset), writer, LOCALE, CSV_SEPARATOR, true);

            String output = writer.toString();
            assertTrue(output.contains("Name"));
            assertTrue(output.contains(asset.getName()));
        }

        @Test
        void downtimes_summedCorrectly() {
            StringWriter writer = createWriter();
            Asset asset = buildAsset(1L);
            AssetDowntime dt1 = new AssetDowntime();
            dt1.setDuration(100L);
            AssetDowntime dt2 = new AssetDowntime();
            dt2.setDuration(200L);
            when(assetDowntimeService.findByAsset(asset.getId())).thenReturn(List.of(dt1, dt2));

            csvFileGenerator.writeAssetsToCsv(List.of(asset), writer, LOCALE, CSV_SEPARATOR, false);

            String output = writer.toString();
            assertTrue(output.contains("300"));
        }

        @Test
        void emptyCollection_writesNothing() {
            StringWriter writer = createWriter();

            csvFileGenerator.writeAssetsToCsv(Collections.emptyList(), writer, LOCALE, CSV_SEPARATOR, false);

            assertTrue(writer.toString().isEmpty());
        }
    }

    @Nested
    class WriteLocationsToCsv {

        @Test
        void withHeaders_writesHeaderAndRecords() {
            StringWriter writer = createWriter();
            Location loc = buildLocation(1L);

            csvFileGenerator.writeLocationsToCsv(List.of(loc), writer, LOCALE, CSV_SEPARATOR, true);

            String output = writer.toString();
            assertTrue(output.contains("Name"));
            assertTrue(output.contains(loc.getName()));
            assertTrue(output.contains(loc.getAddress()));
        }

        @Test
        void emptyCollection_writesNothing() {
            StringWriter writer = createWriter();
            csvFileGenerator.writeLocationsToCsv(Collections.emptyList(), writer, LOCALE, CSV_SEPARATOR, false);
            assertTrue(writer.toString().isEmpty());
        }
    }

    @Nested
    class WritePartsToCsv {

        @Test
        void withHeaders_writesHeaderAndRecords() {
            StringWriter writer = createWriter();
            Part part = buildPart(1L);

            csvFileGenerator.writePartsToCsv(List.of(part), writer, LOCALE, CSV_SEPARATOR, true);

            String output = writer.toString();
            assertTrue(output.contains("Name"));
            assertTrue(output.contains(part.getName()));
            assertTrue(output.contains(String.valueOf(part.getCost())));
        }

        @Test
        void emptyCollection_writesNothing() {
            StringWriter writer = createWriter();
            csvFileGenerator.writePartsToCsv(Collections.emptyList(), writer, LOCALE, CSV_SEPARATOR, false);
            assertTrue(writer.toString().isEmpty());
        }
    }

    @Nested
    class WriteMetersToCsv {

        @Test
        void withHeaders_writesHeaderAndRecords() {
            StringWriter writer = createWriter();
            Meter meter = buildMeter(1L);

            csvFileGenerator.writeMetersToCsv(List.of(meter), writer, LOCALE, CSV_SEPARATOR, true);

            String output = writer.toString();
            assertTrue(output.contains("Name"));
            assertTrue(output.contains(meter.getName()));
            assertTrue(output.contains(meter.getUnit()));
        }

        @Test
        void emptyCollection_writesNothing() {
            StringWriter writer = createWriter();
            csvFileGenerator.writeMetersToCsv(Collections.emptyList(), writer, LOCALE, CSV_SEPARATOR, false);
            assertTrue(writer.toString().isEmpty());
        }
    }

    @Nested
    class WritePreventiveMaintenancesToCsv {

        @Test
        void withHeaders_writesHeaderAndRecords() {
            StringWriter writer = createWriter();
            PreventiveMaintenance pm = buildPM(1L);

            csvFileGenerator.writePreventiveMaintenancesToCsv(List.of(pm), writer, LOCALE, CSV_SEPARATOR, true);

            String output = writer.toString();
            assertTrue(output.contains("Title"));
            assertTrue(output.contains(pm.getTitle()));
        }

        @Test
        void nullSchedule_handlesGracefully() {
            StringWriter writer = createWriter();
            PreventiveMaintenance pm = buildPM(1L);
            pm.setSchedule(null);

            csvFileGenerator.writePreventiveMaintenancesToCsv(List.of(pm), writer, LOCALE, CSV_SEPARATOR, false);

            String output = writer.toString();
            assertTrue(output.contains(pm.getTitle()));
        }

        @Test
        void emptyCollection_writesNothing() {
            StringWriter writer = createWriter();
            csvFileGenerator.writePreventiveMaintenancesToCsv(Collections.emptyList(), writer, LOCALE, CSV_SEPARATOR,
                    false);
            assertTrue(writer.toString().isEmpty());
        }
    }

    @Nested
    class WritePartTransactionsToCsv {

        @Test
        void withHeaders_writesHeaderAndRecords() {
            StringWriter writer = createWriter();
            PartTransaction pt = buildPartTransaction(1L);

            csvFileGenerator.writePartTransactionsToCsv(List.of(pt), writer, LOCALE, CSV_SEPARATOR, true);

            String output = writer.toString();
            assertTrue(output.contains("Part_ID"));
            assertTrue(output.contains(pt.getPart().getName()));
        }

        @Test
        void emptyCollection_writesNothing() {
            StringWriter writer = createWriter();
            csvFileGenerator.writePartTransactionsToCsv(Collections.emptyList(), writer, LOCALE, CSV_SEPARATOR, false);
            assertTrue(writer.toString().isEmpty());
        }
    }

    @Nested
    class WriteCostsAndTimesToCsv {

        @Test
        void withHeaders_writesHeaderAndRecords() {
            StringWriter writer = createWriter();
            WorkOrder wo = buildWorkOrder(1L);
            AdditionalCost cost = new AdditionalCost();
            cost.setId(1L);
            cost.setCost(100.0);
            cost.setDescription("Cost desc");
            cost.setCreatedAt(new Date());
            when(additionalCostService.findByWorkOrder(wo.getId())).thenReturn(List.of(cost));
            when(laborService.findByWorkOrder(wo.getId())).thenReturn(Collections.emptyList());

            csvFileGenerator.writeCostsAndTimesToCsv(List.of(wo), writer, LOCALE, CSV_SEPARATOR, true);

            String output = writer.toString();
            assertTrue(output.contains("Work_Order_Title"));
            assertTrue(output.contains(wo.getTitle()));
            assertTrue(output.contains(String.valueOf(cost.getCost())));
        }

        @Test
        void includesLaborRecords() {
            StringWriter writer = createWriter();
            WorkOrder wo = buildWorkOrder(1L);
            when(additionalCostService.findByWorkOrder(wo.getId())).thenReturn(Collections.emptyList());
            Labor labor = new Labor();
            labor.setId(1L);
            labor.setDuration(7200);
            labor.setHourlyRate(50L);
            labor.setCreatedAt(new Date());
            when(laborService.findByWorkOrder(wo.getId())).thenReturn(List.of(labor));

            csvFileGenerator.writeCostsAndTimesToCsv(List.of(wo), writer, LOCALE, CSV_SEPARATOR, false);

            String output = writer.toString();
            assertTrue(output.contains(labor.getId().toString()));
            assertTrue(output.contains("Time"));
        }

        @Test
        void emptyCollection_writesNothing() {
            StringWriter writer = createWriter();
            csvFileGenerator.writeCostsAndTimesToCsv(Collections.emptyList(), writer, LOCALE, CSV_SEPARATOR, false);
            assertTrue(writer.toString().isEmpty());
        }
    }

    @Test
    void writerThrowsIOException_doesNotPropagate() {
        Writer failingWriter = new Writer() {
            @Override
            public void write(char[] cbuf, int off, int len) throws IOException {
                throw new IOException("Test IO exception");
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };

        assertDoesNotThrow(() ->
                csvFileGenerator.writeWorkOrdersToCsv(List.of(buildWorkOrder(1L)), failingWriter, LOCALE,
                        CSV_SEPARATOR, false));
    }
}
