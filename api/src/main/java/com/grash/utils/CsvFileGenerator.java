package com.grash.utils;

import com.grash.model.*;
import com.grash.service.AdditionalCostService;
import com.grash.service.AssetDowntimeService;
import com.grash.service.LaborService;
import com.grash.service.UserService;
import com.grash.utils.csv.CsvColumn;
import com.grash.utils.csv.CsvColumnRegistries;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CsvFileGenerator {
    private final MessageSource messageSource;
    private final AssetDowntimeService assetDowntimeService;
    private final AdditionalCostService additionalCostService;
    private final LaborService laborService;
    private final UserService userService;
    private final CsvColumnRegistries csvColumnRegistries;

    /**
     * Writes any selection of columns for any entity. The per-entity {@code writeXToCsv}
     * methods below are the unchanged, hand-written writers for the five entities that have
     * no column registry yet; work orders and assets go through here instead — see
     * {@link com.grash.utils.csv.CsvColumnRegistries}.
     * <p>
     * Called once per page of a streamed export, hence {@code includeHeaders}: the header row
     * belongs to the first page only.
     */
    public <T> void writeToCsv(Collection<T> rows, List<CsvColumn<T>> columns, Writer writer,
                               String csvSeparator, boolean includeHeaders) {
        try {
            CSVFormat csvFormat = CSVFormat.DEFAULT.withDelimiter(csvSeparator.charAt(0));
            CSVPrinter printer = new CSVPrinter(writer, csvFormat);
            if (includeHeaders) {
                printer.printRecord(columns.stream().map(CsvColumn::header).collect(Collectors.toList()));
            }
            for (T row : rows) {
                printer.printRecord(columns.stream()
                        .map(column -> column.extractor().apply(row))
                        .collect(Collectors.toList()));
            }
            printer.flush();
        } catch (IOException e) {
            // Same handling as the writers below: the export job logs and reports through the
            // websocket, so a throw here would only replace one error path with two.
            e.printStackTrace();
        }
    }

    /**
     * The unfiltered work-order export. Column list and values now come from the registry so
     * that this file and the filtered export cannot drift apart; the output is unchanged —
     * the registry's default set is this method's former header list in its former order.
     */
    public void writeWorkOrdersToCsv(Collection<WorkOrder> workOrders, Writer writer, Locale locale,
                                     String csvSeparator, boolean includeHeaders) {
        writeToCsv(workOrders, csvColumnRegistries.workOrders(locale).all(), writer, csvSeparator, includeHeaders);
    }

    /**
     * The unfiltered asset export. See {@link #writeWorkOrdersToCsv} — same reasoning, same
     * columns in the same order. One difference in the values: the status column now tolerates
     * a null status instead of throwing, matching how every other enum column in the registry
     * behaves. The field has a default, so no existing row is affected.
     */
    public void writeAssetsToCsv(Collection<Asset> assets, Writer writer, Locale locale, String csvSeparator,
                                 boolean includeHeaders) {
        writeToCsv(assets, csvColumnRegistries.assets(locale).all(), writer, csvSeparator, includeHeaders);
    }

    public void writeLocationsToCsv(Collection<Location> locations, Writer writer, Locale locale, String csvSeparator
            , boolean includeHeaders) {
        try {
            CSVFormat csvFormat = CSVFormat.DEFAULT.withDelimiter(csvSeparator.charAt(0));
            CSVPrinter printer = new CSVPrinter(writer, csvFormat);
            if (includeHeaders) {
                List<String> headers = Arrays.asList("ID", "Name",
                        "Address",
                        "Parent_Location",
                        "Workers",
                        "Teams_Names",
                        "Vendors",
                        "Customers");
                printer.printRecord(headers.stream().map(header -> messageSource.getMessage(header, null, locale)).collect(Collectors.toList()));
            }
            for (Location location : locations) {
                printer.printRecord(location.getId(),
                        location.getName(),
                        location.getAddress(),
                        location.getParentLocation() == null ? null : location.getParentLocation().getName(),
                        Helper.enumerate(location.getWorkers().stream().map(User::getEmail).collect(Collectors.toList())),
                        Helper.enumerate(location.getTeams().stream().map(Team::getName).collect(Collectors.toList())),
                        Helper.enumerate(location.getVendors().stream().map(Vendor::getName).collect(Collectors.toList())),
                        Helper.enumerate(location.getCustomers().stream().map(Customer::getName).collect(Collectors.toList()))
                );
            }
            printer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writePartsToCsv(Collection<Part> parts, Writer writer, Locale locale, String csvSeparator,
                                boolean includeHeaders) {
        try {
            CSVFormat csvFormat = CSVFormat.DEFAULT.withDelimiter(csvSeparator.charAt(0));
            CSVPrinter printer = new CSVPrinter(writer, csvFormat);
            if (includeHeaders) {
                List<String> headers = Arrays.asList("ID", "Name",
                        "Cost",
                        "Category",
                        "Non_Stock",
                        "Barcode",
                        "Description",
                        "Quantity",
                        "Additional_Information",
                        "Area",
                        "Minimum_Quantity",
                        "Assigned_To_Emails",
                        "Customers",
                        "Vendors",
                        "Teams_Names"
                );
                printer.printRecord(headers.stream().map(header -> messageSource.getMessage(header, null, locale)).collect(Collectors.toList()));
            }
            for (Part part : parts) {
                printer.printRecord(part.getId(),
                        part.getName(),
                        part.getCost(),
                        part.getCategory() == null ? null : part.getCategory().getName(),
                        Helper.getStringFromBoolean(part.isNonStock(), messageSource, locale),
                        part.getBarcode(),
                        part.getDescription(),
                        part.getQuantity(),
                        part.getAdditionalInfos(),
                        part.getArea(),
                        part.getMinQuantity(),
                        Helper.enumerate(part.getAssignedTo().stream().map(User::getEmail).collect(Collectors.toList())),
                        Helper.enumerate(part.getCustomers().stream().map(Customer::getName).collect(Collectors.toList())),
                        Helper.enumerate(part.getVendors().stream().map(Vendor::getName).collect(Collectors.toList())),
                        Helper.enumerate(part.getTeams().stream().map(Team::getName).collect(Collectors.toList()))
                );
            }
            printer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeMetersToCsv(Collection<Meter> meters, Writer writer, Locale locale, String csvSeparator,
                                 boolean includeHeaders) {
        try {
            CSVFormat csvFormat = CSVFormat.DEFAULT.withDelimiter(csvSeparator.charAt(0));
            CSVPrinter printer = new CSVPrinter(writer, csvFormat);
            if (includeHeaders) {
                List<String> headers = Arrays.asList("ID", "Name",
                        "Unit",
                        "Update_Frequency",
                        "Category",
                        "Asset_Name",
                        "Location_Name",
                        "Assigned_To_Emails"
                );
                printer.printRecord(headers.stream().map(header -> messageSource.getMessage(header, null, locale)).collect(Collectors.toList()));
            }
            for (Meter meter : meters) {
                printer.printRecord(meter.getId(),
                        meter.getName(),
                        meter.getUnit(),
                        meter.getUpdateFrequency(),
                        meter.getMeterCategory() == null ? null : meter.getMeterCategory().getName(),
                        meter.getAsset() == null ? null : meter.getAsset().getName(),
                        meter.getLocation() == null ? null : meter.getLocation().getName(),
                        Helper.enumerate(meter.getUsers().stream().map(User::getEmail).collect(Collectors.toList())));
            }
            printer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writePreventiveMaintenancesToCsv(Collection<PreventiveMaintenance> preventiveMaintenances,
                                                 Writer writer, Locale locale,
                                                 String csvSeparator, boolean includeHeaders) {
        try {
            CSVFormat csvFormat = CSVFormat.DEFAULT.withDelimiter(csvSeparator.charAt(0));
            CSVPrinter printer = new CSVPrinter(writer, csvFormat);
            if (includeHeaders) {
                List<String> headers = Arrays.asList("ID", "Title", "Starts_On", "Priority", "Description",
                        "Estimated_Duration",
                        "Requires_Signature", "Category", "Location_Name", "Team_Name",
                        "Primary_User_Email", "Asset_Name", "Frequency", "Recurrence_Type");
                printer.printRecord(headers.stream().map(header -> messageSource.getMessage(header, null, locale)).collect(Collectors.toList()));
            }
            for (PreventiveMaintenance pm : preventiveMaintenances) {
                printer.printRecord(pm.getId(),
                        pm.getTitle(),
                        pm.getSchedule() == null ? null : pm.getSchedule().getStartsOn(),
                        pm.getPriority() == null ? null :
                                messageSource.getMessage(pm.getPriority().toString(), null, locale),
                        pm.getDescription(),
                        pm.getEstimatedDuration(),
                        Helper.getStringFromBoolean(pm.isRequiredSignature(), messageSource, locale),
                        pm.getCategory() == null ? null : pm.getCategory().getName(),
                        pm.getLocation() == null ? null : pm.getLocation().getName(),
                        pm.getTeam() == null ? null : pm.getTeam().getName(),
                        pm.getPrimaryUser() == null ? null : pm.getPrimaryUser().getEmail(),
                        pm.getAsset() == null ? null : pm.getAsset().getName(),
                        pm.getSchedule() == null ? null : pm.getSchedule().getFrequency(),
                        pm.getSchedule() == null ? null :
                                messageSource.getMessage(pm.getSchedule().getRecurrenceType().name(), null, locale)
                );
            }
            printer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writePartTransactionsToCsv(Collection<PartTransaction> partTransactions, Writer writer, Locale locale,
                                           String csvSeparator, boolean includeHeaders) {
        try {
            CSVFormat csvFormat = CSVFormat.DEFAULT.withDelimiter(csvSeparator.charAt(0));
            CSVPrinter printer = new CSVPrinter(writer, csvFormat);
            if (includeHeaders) {
                List<String> headers = Arrays.asList("ID", "Part_ID", "Part", "Quantity", "Cost", "Work_Order_ID"
                        , "Work_Order_Title",
                        "Description", "Created_At");
                printer.printRecord(headers.stream().map(header -> messageSource.getMessage(header, null, locale)).collect(Collectors.toList()));
            }
            for (PartTransaction pt : partTransactions) {
                printer.printRecord(pt.getId(),
                        pt.getPart().getId(),
                        pt.getPart().getName(),
                        -pt.getQuantity(),
                        -pt.getCost(),
                        pt.getWorkOrder() == null ? null : pt.getWorkOrder().getId(),
                        pt.getWorkOrder() == null ? null : pt.getWorkOrder().getTitle(),
                        pt.getDescription(),
                        pt.getCreatedAt()
                );
            }
            printer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeCostsAndTimesToCsv(Collection<WorkOrder> workOrders, Writer writer, Locale locale,
                                        String csvSeparator, boolean includeHeaders) {
        try {
            CSVFormat csvFormat = CSVFormat.DEFAULT.withDelimiter(csvSeparator.charAt(0));
            CSVPrinter printer = new CSVPrinter(writer, csvFormat);
            if (includeHeaders) {
                List<String> headers = Arrays.asList("ID", "Work_Order_Title", "Work_Order_ID", "User", "Creator",
                        "Created_At", "Type", "Cost", "Cost_Category", "Time_Category",
                        "Description", "Time_Hours", "Hourly_Rate");
                printer.printRecord(headers.stream().map(header -> messageSource.getMessage(header, null, locale)).collect(Collectors.toList()));
            }
            for (WorkOrder workOrder : workOrders) {
                Collection<AdditionalCost> additionalCosts = additionalCostService.findByWorkOrder(workOrder.getId());
                for (AdditionalCost cost : additionalCosts) {
                    String creatorEmail = null;
                    if (cost.getCreatedBy() != null) {
                        Optional<User> creator = userService.findById(cost.getCreatedBy());
                        if (creator.isPresent()) creatorEmail = creator.get().getEmail();
                    }
                    printer.printRecord(
                            cost.getId(),
                            workOrder.getTitle(),
                            workOrder.getId(),
                            cost.getAssignedTo() == null ? null : cost.getAssignedTo().getEmail(),
                            creatorEmail,
                            cost.getCreatedAt(),
                            "Cost",
                            cost.getCost(),
                            cost.getCategory() == null ? null : cost.getCategory().getName(),
                            null,
                            cost.getDescription(),
                            null,
                            cost.getAssignedTo() == null ? null : cost.getAssignedTo().getRate()
                    );
                }
                Collection<Labor> labors = laborService.findByWorkOrder(workOrder.getId());
                for (Labor labor : labors) {
                    String creatorEmail = null;
                    if (labor.getCreatedBy() != null) {
                        Optional<User> creator = userService.findById(labor.getCreatedBy());
                        if (creator.isPresent()) creatorEmail = creator.get().getEmail();
                    }
                    double hours = labor.getDuration() / 3600.0;
                    printer.printRecord(
                            labor.getId(),
                            workOrder.getTitle(),
                            workOrder.getId(),
                            labor.getAssignedTo() == null ? null : labor.getAssignedTo().getEmail(),
                            creatorEmail,
                            labor.getCreatedAt(),
                            "Time",
                            labor.getHourlyRate() * hours,
                            null,
                            labor.getTimeCategory() == null ? null : labor.getTimeCategory().getName(),
                            null,
                            hours,
                            labor.getHourlyRate()
                    );
                }
            }
            printer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
