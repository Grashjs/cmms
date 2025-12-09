package com.grash.service;

import com.grash.model.*;
import com.grash.model.abstracts.CategoryAbstract;
import com.grash.model.enums.*;
import com.grash.repository.*;
import com.grash.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class DemoDataService {

    private final WorkOrderCategoryRepository workOrderCategoryRepository;
    private final AssetCategoryRepository assetCategoryRepository;
    private final MeterCategoryRepository meterCategoryRepository;
    private final TimeCategoryRepository timeCategoryRepository;
    private final CostCategoryRepository costCategoryRepository;
    private final PartCategoryRepository partCategoryRepository;
    private final PurchaseOrderCategoryRepository purchaseOrderCategoryRepository;
    private final LocationRepository locationRepository;
    private final AssetRepository assetRepository;
    private final MeterRepository meterRepository;
    private final PartRepository partRepository;
    private final VendorRepository vendorRepository;
    private final CustomerRepository customerRepository;
    private final PreventiveMaintenanceRepository preventiveMaintenanceRepository;
    private final WorkOrderRepository workOrderRepository;
    private final RequestRepository requestRepository;
    private final LaborRepository laborRepository;
    private final PartQuantityRepository partQuantityRepository;
    private final AdditionalCostRepository additionalCostRepository;
    private final ScheduleRepository scheduleRepository;


    @Transactional
    @Async
    public void createDemoData(OwnUser user, Company company) {
        // Work Order Categories
        WorkOrderCategory woCategory1 = createWorkOrderCategory("Electrical", company);
        WorkOrderCategory woCategory2 = createWorkOrderCategory("Mechanical", company);
        WorkOrderCategory woCategory3 = createWorkOrderCategory("HVAC", company);
        WorkOrderCategory woCategory4 = createWorkOrderCategory("Safety", company);
        WorkOrderCategory woCategory5 = createWorkOrderCategory("Plumbing", company);

        // Asset Categories
        AssetCategory assetCategory1 = createAssetCategory("HVAC Unit", company);
        AssetCategory assetCategory2 = createAssetCategory("Vehicle", company);
        AssetCategory assetCategory3 = createAssetCategory("Generator", company);
        AssetCategory assetCategory4 = createAssetCategory("Pump", company);
        AssetCategory assetCategory5 = createAssetCategory("Production Machine", company);

        // Meter Categories
        MeterCategory meterCategory1 = createMeterCategory("Hours", company);
        MeterCategory meterCategory2 = createMeterCategory("Mileage", company);
        MeterCategory meterCategory3 = createMeterCategory("Cycles", company);

        // Time Categories
        TimeCategory timeCategory1 = createTimeCategory("Inspection", company);
        TimeCategory timeCategory2 = createTimeCategory("Calibration", company);

        // Cost Categories
        CostCategory costCategory1 = createCostCategory("Labor", company);
        CostCategory costCategory2 = createCostCategory("Parts", company);
        CostCategory costCategory3 = createCostCategory("Subcontractor", company);

        // Part Categories
        PartCategory partCategory1 = createPartCategory("Filters", company);
        PartCategory partCategory2 = createPartCategory("Belts", company);
        PartCategory partCategory3 = createPartCategory("Electrical Components", company);

        // Purchase Order Categories
        PurchaseOrderCategory poCategory1 = createPurchaseOrderCategory("Parts Purchase", company);
        PurchaseOrderCategory poCategory2 = createPurchaseOrderCategory("Vendor Service", company);


        // Locations
        Location location1 = createLocation("Main Building", null, company);
        Location location2 = createLocation("Warehouse A", location1, company);
        Location location3 = createLocation("Production Floor", location1, company);

        // Assets
        Asset asset1 = createAsset("HVAC-001", "Central HVAC Unit", assetCategory1, location2, company, null,
                AssetStatus.DOWN);
        Asset asset2 = createAsset("TRUCK-01", "Ford F-150", assetCategory2, location1, company, null,
                AssetStatus.OPERATIONAL);
        Asset engine = createAsset(
                "TRUCK-01-ENG",
                "Engine Assembly",
                assetCategory2,
                location1,
                company,
                asset2,
                AssetStatus.OPERATIONAL
        );
        Asset transmission = createAsset(
                "TRUCK-01-TRANS",
                "Transmission System",
                assetCategory2,
                location1,
                company,
                asset2,
                AssetStatus.OPERATIONAL
        );

        Asset asset3 = createAsset("GEN-001", "Backup Generator", assetCategory3, location3, company, null,
                AssetStatus.EMERGENCY_SHUTDOWN);

        // Meters
        Meter meter1 = createMeter("HVAC Hours", meterCategory1, asset1, company, 1, "h");
        Meter meter2 = createMeter("Truck Mileage", meterCategory2, asset2, company, 7, "km");
        Meter meter3 = createMeter("Generator Hours", meterCategory1, asset3, company, 4, "hours");

        // Parts
        Part part1 = createPart("Air Filter", "AF-001", partCategory1, company, 10L, 15.99);
        Part part2 = createPart("V-Belt", "VB-001", partCategory2, company, 5L, 25.5);
        Part part3 = createPart("Fuse 2A", "F-002A", partCategory3, company, 20L, 2.99);

        // Vendors
        Vendor vendor1 = createVendor("Oscar Nilsson", "HVAC Parts Supply", "123-456-7890", "contact@hvacsupply.com",
                company, 45);
        Vendor vendor2 = createVendor("Alessandro Rossi", "General Maintenance Inc.", "987-654-3210", "contact" +
                "@genmaint.com", company, 30);

        // Customer
        Customer customer1 = createCustomer("Carlos Mendoza", company, "123-557-8901", "carlos-ter.com", "378 " +
                "Middleville Road", 26, "Electrical");

        // Preventive Maintenances
        createPreventiveMaintenance("Quarterly HVAC Inspection", "HVAC Inspection", asset1, company, 1,
                RecurrenceType.YEARLY,
                RecurrenceBasedOn.SCHEDULED_DATE, new ArrayList<>());
        createPreventiveMaintenance("Weekly Generator Checkup", "Generator Checkup", asset3, company, 1,
                RecurrenceType.WEEKLY,
                RecurrenceBasedOn.SCHEDULED_DATE, Arrays.asList(1, 3, 4));

        // Work Orders
        WorkOrder wo1 = createWorkOrder("Fix leaking pipe", "A pipe is leaking in the main building", woCategory5,
                asset1, location1, user, new Date(), Status.IN_PROGRESS, Priority.HIGH, company);
        WorkOrder wo2 = createWorkOrder("Replace air filter", "Replace the air filter in HVAC-001", woCategory3,
                asset1, location2, user, new Date(), Status.ON_HOLD, Priority.LOW, company);
        WorkOrder wo3 = createWorkOrder("Perform annual inspection", "Annual inspection of the backup generator",
                woCategory4, asset3, location3, user, new Date(), Status.COMPLETE, Priority.LOW,
                company);

        // Work Order Details
        addLaborToWorkOrder(wo1, user, timeCategory1, 50, 2, company);
        addPartToWorkOrder(wo1, part1, 1L, company);
        addCostToWorkOrder(wo1, costCategory3, "External plumbing service", 150, new Date());

        // Request
        createRequest("Office is too cold", "The temperature in the main office is too cold.", location1, user,
                new Date(), company);

    }

    private WorkOrderCategory createWorkOrderCategory(String name, Company company) {
        WorkOrderCategory category = new WorkOrderCategory();
        category.setName(name);
        category.setCompanySettings(company.getCompanySettings());
        category.setDemo(true);
        return workOrderCategoryRepository.save(category);
    }

    private AssetCategory createAssetCategory(String name, Company company) {
        AssetCategory category = new AssetCategory();
        category.setName(name);
        category.setCompanySettings(company.getCompanySettings());
        category.setDemo(true);
        return assetCategoryRepository.save(category);
    }

    private MeterCategory createMeterCategory(String name, Company company) {
        MeterCategory category = new MeterCategory();
        category.setName(name);
        category.setCompanySettings(company.getCompanySettings());
        category.setDemo(true);
        return meterCategoryRepository.save(category);
    }

    private TimeCategory createTimeCategory(String name, Company company) {
        TimeCategory category = new TimeCategory();
        category.setName(name);
        category.setCompanySettings(company.getCompanySettings());
        category.setDemo(true);
        return timeCategoryRepository.save(category);
    }

    private CostCategory createCostCategory(String name, Company company) {
        CostCategory category = new CostCategory();
        category.setName(name);
        category.setCompanySettings(company.getCompanySettings());
        category.setDemo(true);
        return costCategoryRepository.save(category);
    }

    private PartCategory createPartCategory(String name, Company company) {
        PartCategory category = new PartCategory();
        category.setName(name);
        category.setCompanySettings(company.getCompanySettings());
        category.setDemo(true);
        return partCategoryRepository.save(category);
    }

    private PurchaseOrderCategory createPurchaseOrderCategory(String name, Company company) {
        PurchaseOrderCategory category = new PurchaseOrderCategory();
        category.setName(name);
        category.setCompanySettings(company.getCompanySettings());
        category.setDemo(true);
        return purchaseOrderCategoryRepository.save(category);
    }

    // --- Location, Asset, and Meter Creation Methods ---

    private Location createLocation(String name, Location parent, Company company) {
        Location location = new Location();
        location.setName(name);
        location.setParentLocation(parent);
        location.setCompany(company);
        location.setDemo(true);
        return locationRepository.save(location);
    }

    private Asset createAsset(String name, String description, AssetCategory category, Location location,
                              Company company, Asset parentAsset, AssetStatus status) {
        Asset asset = new Asset();
        asset.setName(name);
        asset.setDescription(description);
        asset.setCategory(category);
        asset.setLocation(location);
        asset.setCompany(company);
        asset.setParentAsset(parentAsset);
        asset.setStatus(status);
        asset.setDemo(true);
        return assetRepository.save(asset);
    }

    private Meter createMeter(String name, MeterCategory category, Asset asset, Company company, int updateFrequency,
                              String unit) {
        Meter meter = new Meter();
        meter.setName(name);
        meter.setMeterCategory(category);
        meter.setAsset(asset);
        meter.setCompany(company);
        meter.setUpdateFrequency(updateFrequency);
        meter.setUnit(unit);
        meter.setDemo(true);
        return meterRepository.save(meter);
    }

    // --- Part, Vendor, and Customer Creation Methods ---

    private Part createPart(String name, String code, PartCategory category, Company company,
                            Long quantity, double cost) {
        Part part = new Part();
        part.setName(name);
        part.setBarcode(code);
        part.setCategory(category);
        part.setCompany(company);
        part.setQuantity(quantity);
        part.setCost(cost);
        part.setDemo(true);
        return partRepository.save(part);
    }

    private Vendor createVendor(String name, String companyName, String phone, String email, Company company,
                                long hourlyRate) {
        Vendor vendor = new Vendor();
        vendor.setName(name);
        vendor.setCompanyName(companyName);
        vendor.setPhone(phone);
        vendor.setEmail(email);
        vendor.setCompany(company);
        vendor.setRate(hourlyRate);
        vendor.setDemo(true);
        return vendorRepository.save(vendor);
    }

    private Customer createCustomer(String name, Company company, String phone, String website, String address,
                                    long hourlyRate, String type) {
        Customer customer = new Customer();
        customer.setName(name);
        customer.setCompany(company);
        customer.setPhone(phone);
        customer.setWebsite(website);
        customer.setCustomerType(type);
        customer.setAddress(address);
        customer.setRate(hourlyRate);
        customer.setDemo(true);
        return customerRepository.save(customer);
    }

    // --- Maintenance and Order Creation Methods ---

    private PreventiveMaintenance createPreventiveMaintenance(String name, String workOrderTitle, Asset asset,
                                                              Company company,
                                                              int frequency,
                                                              RecurrenceType recurrenceType,
                                                              RecurrenceBasedOn recurrenceBasedOn,
                                                              List<Integer> daysOfWeek) {
        PreventiveMaintenance pm = new PreventiveMaintenance();
        pm.setName(name);
        pm.setTitle(workOrderTitle);
        pm.setAsset(asset);
        pm.setCompany(company);
        pm.setDemo(true);

        Schedule schedule = new Schedule(pm);
        schedule.setFrequency(frequency);
        schedule.setRecurrenceType(recurrenceType);
        schedule.setRecurrenceBasedOn(recurrenceBasedOn);
        schedule.setDaysOfWeek(daysOfWeek);
        schedule.setDueDateDelay(1);
        schedule.setEndsOn(Helper.incrementDays(new Date(), 7));
        schedule.setDemo(true);

        pm.setSchedule(schedule);

        pm = preventiveMaintenanceRepository.save(pm);

        return pm;
    }

    private WorkOrder createWorkOrder(String title, String description, WorkOrderCategory category, Asset asset,
                                      Location location, OwnUser assignedTo, Date creationDate,
                                      Status status, Priority priority, Company company) {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setTitle(title);
        workOrder.setDescription(description);
        workOrder.setCategory(category);
        workOrder.setAsset(asset);
        workOrder.setLocation(location);
        workOrder.setAssignedTo(Collections.singletonList(assignedTo));
        workOrder.setCreatedAt(creationDate);
        workOrder.setStatus(status);
        workOrder.setPriority(priority);
        workOrder.setCompany(company);
        workOrder.setDemo(true);
        return workOrderRepository.save(workOrder);
    }

    private Request createRequest(String title, String description, Location location, OwnUser requester,
                                  Date creationDate, Company company) {
        Request request = new Request();
        request.setTitle(title);
        request.setDescription(description);
        request.setLocation(location);
        request.setCreatedBy(requester.getId());
        request.setCreatedAt(creationDate);
        request.setCompany(company);
        request.setDemo(true);
        return requestRepository.save(request);
    }


    private void addLaborToWorkOrder(WorkOrder workOrder, OwnUser user, TimeCategory category,
                                     long hourlyRate, long hours, Company company) {
        Labor labor = new Labor(user, hourlyRate, new Date(), workOrder, false, TimeStatus.STOPPED);
        labor.setTimeCategory(category);
        labor.setDuration(hours * 3600);
        labor.setCompany(company);
        labor.setDemo(true);
        laborRepository.save(labor);
    }

    private void addPartToWorkOrder(WorkOrder workOrder, Part part, double quantity, Company company) {
        PartQuantity partQuantity = new PartQuantity(part, workOrder, null, quantity);
        partQuantity.setCompany(company);
        partQuantity.setDemo(true);
        partQuantityRepository.save(partQuantity);
    }

    private void addCostToWorkOrder(WorkOrder workOrder, CostCategory category, String description, double cost,
                                    Date date) {
        AdditionalCost additionalCost = new AdditionalCost();
        additionalCost.setCategory(category);
        additionalCost.setCost(cost);
        additionalCost.setWorkOrder(workOrder);
        additionalCost.setDescription(description);
        additionalCost.setDemo(true);
        additionalCost.setDate(date);
        additionalCostRepository.save(additionalCost);
    }
}