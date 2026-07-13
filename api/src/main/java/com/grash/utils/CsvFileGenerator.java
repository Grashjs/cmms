package com.grash.utils;

import com.grash.model.*;
import com.grash.service.AssetDowntimeService;
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
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CsvFileGenerator {
    private final MessageSource messageSource;
    private final AssetDowntimeService assetDowntimeService;

    public void writeWorkOrdersToCsv(Collection<WorkOrder> workOrders, Writer writer, Locale locale) {
        try {
            CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT);
            List<String> headers = Arrays.asList("ID", "Title", "Status", "Priority", "Description", "Due_Date", "Estimated_Duration", "Requires_Signature", "Category", "Location_Name", "Team_Name", "Primary_User_Email", "Assigned_To_Emails", "Asset_Name", "Completed_By_Email", "Completed_On", "Archived", "Feedback", "Customers", "Created_At");
            printer.printRecord(headers.stream().map(header -> messageSource.getMessage(header, null, locale)).collect(Collectors.toList()));
            for (WorkOrder workOrder : workOrders) {
                printer.printRecord(workOrder.getId(),
                        workOrder.getTitle(),
                        workOrder.getStatus() == null ? null : messageSource.getMessage(workOrder.getStatus().toString(), null, locale),
                        workOrder.getPriority() == null ? null : messageSource.getMessage(workOrder.getPriority().toString(), null, locale),
                        workOrder.getDescription(),
                        workOrder.getDueDate(),
                        workOrder.getEstimatedDuration(),
                        Helper.getStringFromBoolean(workOrder.isRequiredSignature(), messageSource, locale),
                        workOrder.getCategory() == null ? null : workOrder.getCategory().getName(),
                        workOrder.getLocation() == null ? null : workOrder.getLocation().getName(),
                        workOrder.getTeam() == null ? null : workOrder.getTeam().getName(),
                        workOrder.getPrimaryUser() == null ? null : workOrder.getPrimaryUser().getEmail(),
                        Helper.enumerate(workOrder.getAssignedTo().stream().map(OwnUser::getEmail).collect(Collectors.toList())),
                        workOrder.getAsset() == null ? null : workOrder.getAsset().getName(),
                        workOrder.getCompletedBy() == null ? null : workOrder.getCompletedBy().getEmail(),
                        workOrder.getCompletedOn(),
                        Helper.getStringFromBoolean(workOrder.isArchived(), messageSource, locale),
                        workOrder.getFeedback(),
                        Helper.enumerate(workOrder.getCustomers().stream().map(Customer::getName).collect(Collectors.toList())),
                        workOrder.getCreatedAt()
                );
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeAssetsToCsv(Collection<Asset> assets, Writer writer, Locale locale) {
        try {
            CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT);
            List<String> headers = Arrays.asList("ID", "Name",
                    "Description",
                    "Status",
                    "Archived",
                    "Location_Name",
                    "Parent_Asset",
                    "Area",
                    "Barcode",
                    "Category",
                    "Primary_User_Email",
                    "Warranty_Expiration_Date",
                    "Additional_Information",
                    "Serial_Number",
                    "Assigned_To_Emails",
                    "Teams_Names",
                    "Parts",
                    "Vendors",
                    "Customers",
                    "Downtime_Duration");
            printer.printRecord(headers.stream().map(header -> messageSource.getMessage(header, null, locale)).collect(Collectors.toList()));
            for (Asset asset : assets) {
                Collection<AssetDowntime> downtimes = assetDowntimeService.findByAsset(asset.getId());
                long downTimeDuration = downtimes.stream().map(AssetDowntime::getDuration)
                        .reduce(0L, Long::sum);
                
                printer.printRecord(asset.getId(),
                        asset.getName(),
                        asset.getDescription(),
                        messageSource.getMessage(asset.getStatus().toString(), null, locale),
                        Helper.getStringFromBoolean(asset.isArchived(), messageSource, locale),
                        asset.getLocation() == null ? null : asset.getLocation().getName(),
                        asset.getParentAsset() == null ? null : asset.getParentAsset().getName(),
                        asset.getArea(),
                        asset.getBarCode(),
                        asset.getCategory() == null ? null : asset.getCategory().getName(),
                        asset.getPrimaryUser() == null ? null : asset.getPrimaryUser().getEmail(),
                        asset.getWarrantyExpirationDate(),
                        asset.getAdditionalInfos(),
                        asset.getSerialNumber(),
                        Helper.enumerate(asset.getAssignedTo().stream().map(OwnUser::getEmail).collect(Collectors.toList())),
                        Helper.enumerate(asset.getTeams().stream().map(Team::getName).collect(Collectors.toList())),
                        Helper.enumerate(asset.getParts().stream().map(Part::getName).collect(Collectors.toList())),
                        Helper.enumerate(asset.getVendors().stream().map(Vendor::getName).collect(Collectors.toList())),
                        Helper.enumerate(asset.getCustomers().stream().map(Customer::getName).collect(Collectors.toList())),
                        downTimeDuration
                );
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeLocationsToCsv(Collection<Location> locations, Writer writer, Locale locale) {
        try {
            CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT);
            List<String> headers = Arrays.asList("ID", "Name",
                    "Address",
                    "Parent_Location",
                    "Workers",
                    "Teams_Names",
                    "Vendors",
                    "Customers");
            printer.printRecord(headers.stream().map(header -> messageSource.getMessage(header, null, locale)).collect(Collectors.toList()));
            for (Location location : locations) {
                printer.printRecord(location.getId(),
                        location.getName(),
                        location.getAddress(),
                        location.getParentLocation() == null ? null : location.getParentLocation().getName(),
                        Helper.enumerate(location.getWorkers().stream().map(OwnUser::getEmail).collect(Collectors.toList())),
                        Helper.enumerate(location.getTeams().stream().map(Team::getName).collect(Collectors.toList())),
                        Helper.enumerate(location.getVendors().stream().map(Vendor::getName).collect(Collectors.toList())),
                        Helper.enumerate(location.getCustomers().stream().map(Customer::getName).collect(Collectors.toList()))
                );
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writePartsToCsv(Collection<Part> parts, Writer writer, Locale locale) {
        try {
            CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT);
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
                        Helper.enumerate(part.getAssignedTo().stream().map(OwnUser::getEmail).collect(Collectors.toList())),
                        Helper.enumerate(part.getCustomers().stream().map(Customer::getName).collect(Collectors.toList())),
                        Helper.enumerate(part.getVendors().stream().map(Vendor::getName).collect(Collectors.toList())),
                        Helper.enumerate(part.getTeams().stream().map(Team::getName).collect(Collectors.toList()))
                );
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeMetersToCsv(Collection<Meter> meters, Writer writer, Locale locale) {
        try {
            CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT);
            List<String> headers = Arrays.asList("ID", "Name",
                    "Unit",
                    "Update_Frequency",
                    "Category",
                    "Asset_Name",
                    "Location_Name",
                    "Assigned_To_Emails"
            );
            printer.printRecord(headers.stream().map(header -> messageSource.getMessage(header, null, locale)).collect(Collectors.toList()));
            for (Meter meter : meters) {
                printer.printRecord(meter.getId(),
                        meter.getName(),
                        meter.getUnit(),
                        meter.getUpdateFrequency(),
                        meter.getMeterCategory() == null ? null : meter.getMeterCategory().getName(),
                        meter.getAsset() == null ? null : meter.getAsset().getName(),
                        meter.getLocation() == null ? null : meter.getLocation().getName(),
                        Helper.enumerate(meter.getUsers().stream().map(OwnUser::getEmail).collect(Collectors.toList())));
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
