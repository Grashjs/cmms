package com.grash.service;

import com.grash.event.CompanyCreatedEvent;
import com.grash.model.*;
import com.grash.model.enums.*;
import com.grash.repository.*;
import com.grash.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
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
    @Autowired
    @Lazy
    private ScheduleService scheduleService;
    @Autowired
    @Lazy
    private WorkOrderService workOrderService;

    @Autowired
    @Lazy
    private RequestService requestService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handleUserCreated(CompanyCreatedEvent event) {
        createDemoData(event.getUser(), event.getUser().getCompany());
    }

    @Transactional
    @Async
    public void createDemoData(OwnUser user, Company company) {
        // Work Order Categories
        WorkOrderCategory woCategory1 = createWorkOrderCategory("Electrical", company, user);
        WorkOrderCategory woCategory2 = createWorkOrderCategory("Mechanical", company, user);
        WorkOrderCategory woCategory3 = createWorkOrderCategory("HVAC", company, user);
        WorkOrderCategory woCategory4 = createWorkOrderCategory("Safety", company, user);
        WorkOrderCategory woCategory5 = createWorkOrderCategory("Plumbing", company, user);

        // Asset Categories
        AssetCategory assetCategory1 = createAssetCategory("HVAC Unit", company, user);
        AssetCategory assetCategory2 = createAssetCategory("Vehicle", company, user);
        AssetCategory assetCategory3 = createAssetCategory("Generator", company, user);
        AssetCategory assetCategory4 = createAssetCategory("Pump", company, user);
        AssetCategory assetCategory5 = createAssetCategory("Production Machine", company, user);

        // Meter Categories
        MeterCategory meterCategory1 = createMeterCategory("Hours", company, user);
        MeterCategory meterCategory2 = createMeterCategory("Mileage", company, user);
        MeterCategory meterCategory3 = createMeterCategory("Cycles", company, user);

        // Time Categories
        TimeCategory timeCategory1 = createTimeCategory("Inspection", company, user);
        TimeCategory timeCategory2 = createTimeCategory("Calibration", company, user);

        // Cost Categories
        CostCategory costCategory1 = createCostCategory("Labor", company, user);
        CostCategory costCategory2 = createCostCategory("Parts", company, user);
        CostCategory costCategory3 = createCostCategory("Subcontractor", company, user);

        // Part Categories
        PartCategory partCategory1 = createPartCategory("Filters", company, user);
        PartCategory partCategory2 = createPartCategory("Belts", company, user);
        PartCategory partCategory3 = createPartCategory("Electrical Components", company, user);

        // Purchase Order Categories
        PurchaseOrderCategory poCategory1 = createPurchaseOrderCategory("Parts Purchase", company, user);
        PurchaseOrderCategory poCategory2 = createPurchaseOrderCategory("Vendor Service", company, user);


        // Locations
        Location location1 = createLocation("Main Building", null, company, user);
        Location location2 = createLocation("Warehouse A", location1, company, user);
        Location location3 = createLocation("Production Floor", location1, company, user);

        // Assets
        Asset asset1 = createAsset("HVAC-001", "Central HVAC Unit", assetCategory1, location2, company, null,
                AssetStatus.DOWN, user);
        Asset asset2 = createAsset("TRUCK-01", "Ford F-150", assetCategory2, location1, company, null,
                AssetStatus.OPERATIONAL, user);
        Asset engine = createAsset(
                "TRUCK-01-ENG",
                "Engine Assembly",
                assetCategory2,
                location1,
                company,
                asset2,
                AssetStatus.OPERATIONAL, user
        );
        Asset transmission = createAsset(
                "TRUCK-01-TRANS",
                "Transmission System",
                assetCategory2,
                location1,
                company,
                asset2,
                AssetStatus.OPERATIONAL
                , user);

        Asset asset3 = createAsset("GEN-001", "Backup Generator", assetCategory3, location3, company, null,
                AssetStatus.EMERGENCY_SHUTDOWN, user);

        // Meters
        Meter meter1 = createMeter("HVAC Hours", meterCategory1, asset1, company, 1, "h", user);
        Meter meter2 = createMeter("Truck Mileage", meterCategory2, asset2, company, 7, "km", user);
        Meter meter3 = createMeter("Generator Hours", meterCategory1, asset3, company, 4, "hours", user);

        // Parts
        Part part1 = createPart("Air Filter", "AF-001", partCategory1, company, 10L, 15.99, user);
        Part part2 = createPart("V-Belt", "VB-001", partCategory2, company, 5L, 25.5, user);
        Part part3 = createPart("Fuse 2A", "F-002A", partCategory3, company, 20L, 2.99, user);

        // Vendors
        Vendor vendor1 = createVendor("Oscar Nilsson", "HVAC Parts Supply", "123-456-7890", "contact@hvacsupply.com",
                company, 45, user);
        Vendor vendor2 = createVendor("Alessandro Rossi", "General Maintenance Inc.", "987-654-3210", "contact" +
                "@genmaint.com", company, 30, user);

        // Customer
        Customer customer1 = createCustomer("Carlos Mendoza", company, "123-557-8901", "carlos-ter.com", "378 " +
                "Middleville Road", 26, "Electrical", user);

        // Preventive Maintenances
        createPreventiveMaintenance("Quarterly HVAC Inspection", "HVAC Inspection", asset1, company, 90,
                RecurrenceType.DAILY,
                RecurrenceBasedOn.SCHEDULED_DATE, new ArrayList<>(), user);
        createPreventiveMaintenance("Weekly Generator Checkup", "Generator Checkup", asset3, company, 1,
                RecurrenceType.WEEKLY,
                RecurrenceBasedOn.SCHEDULED_DATE, Arrays.asList(1, 3, 4), user);

        // Work Orders
        WorkOrder wo1 = createWorkOrder("Fix leaking pipe", "A pipe is leaking in the main building", woCategory5,
                asset1, location1, user, new Date(), Status.IN_PROGRESS, Priority.HIGH, company, user);
        WorkOrder wo2 = createWorkOrder("Replace air filter", "Replace the air filter in HVAC-001", woCategory3,
                asset1, location2, user, new Date(), Status.ON_HOLD, Priority.LOW, company, user);
        WorkOrder wo3 = createWorkOrder("Perform annual inspection", "Annual inspection of the backup generator",
                woCategory4, asset3, location3, user, new Date(), Status.COMPLETE, Priority.LOW,
                company, user);

        // Work Order Details
        addLaborToWorkOrder(wo1, user, timeCategory1, 50, 2, company);
        addPartToWorkOrder(wo1, part1, 1L, company, user);
        addCostToWorkOrder(wo1, costCategory3, "External plumbing service", 150, new Date(), user);

        // Request
        createRequest("Office is too cold", "The temperature in the main office is too cold.", location1, user,
                new Date(), company, user);

    }

    private WorkOrderCategory createWorkOrderCategory(String name, Company company, OwnUser user) {
        WorkOrderCategory category = new WorkOrderCategory();
        category.setName(name);
        category.setCompanySettings(company.getCompanySettings());
        category.setCreatedBy(user.getId());
        category.setDemo(true);
        return workOrderCategoryRepository.save(category);
    }

    private AssetCategory createAssetCategory(String name, Company company, OwnUser user) {
        AssetCategory category = new AssetCategory();
        category.setName(name);
        category.setCompanySettings(company.getCompanySettings());
        category.setCreatedBy(user.getId());
        category.setDemo(true);
        return assetCategoryRepository.save(category);
    }

    private MeterCategory createMeterCategory(String name, Company company, OwnUser user) {
        MeterCategory category = new MeterCategory();
        category.setName(name);
        category.setCompanySettings(company.getCompanySettings());
        category.setCreatedBy(user.getId());
        category.setDemo(true);
        return meterCategoryRepository.save(category);
    }

    private TimeCategory createTimeCategory(String name, Company company, OwnUser user) {
        TimeCategory category = new TimeCategory();
        category.setName(name);
        category.setCompanySettings(company.getCompanySettings());
        category.setCreatedBy(user.getId());
        category.setDemo(true);
        return timeCategoryRepository.save(category);
    }

    private CostCategory createCostCategory(String name, Company company, OwnUser user) {
        CostCategory category = new CostCategory();
        category.setName(name);
        category.setCompanySettings(company.getCompanySettings());
        category.setCreatedBy(user.getId());
        category.setDemo(true);
        return costCategoryRepository.save(category);
    }

    private PartCategory createPartCategory(String name, Company company, OwnUser user) {
        PartCategory category = new PartCategory();
        category.setName(name);
        category.setCreatedBy(user.getId());
        category.setCompanySettings(company.getCompanySettings());
        category.setDemo(true);
        return partCategoryRepository.save(category);
    }

    private PurchaseOrderCategory createPurchaseOrderCategory(String name, Company company, OwnUser user) {
        PurchaseOrderCategory category = new PurchaseOrderCategory();
        category.setName(name);
        category.setCreatedBy(user.getId());
        category.setCompanySettings(company.getCompanySettings());
        category.setDemo(true);
        return purchaseOrderCategoryRepository.save(category);
    }

    // --- Location, Asset, and Meter Creation Methods ---

    private Location createLocation(String name, Location parent, Company company, OwnUser user) {
        Location location = new Location();
        location.setName(name);
        location.setParentLocation(parent);
        location.setCreatedBy(user.getId());
        location.setCompany(company);
        location.setDemo(true);
        return locationRepository.save(location);
    }

    private Asset createAsset(String name, String description, AssetCategory category, Location location,
                              Company company, Asset parentAsset, AssetStatus status, OwnUser user) {
        Asset asset = new Asset();
        asset.setName(name);
        asset.setDescription(description);
        asset.setCategory(category);
        asset.setLocation(location);
        asset.setCompany(company);
        asset.setParentAsset(parentAsset);
        asset.setCreatedBy(user.getId());
        asset.setStatus(status);
        asset.setDemo(true);
        return assetRepository.save(asset);
    }

    private Meter createMeter(String name, MeterCategory category, Asset asset, Company company, int updateFrequency,
                              String unit, OwnUser user) {
        Meter meter = new Meter();
        meter.setName(name);
        meter.setMeterCategory(category);
        meter.setAsset(asset);
        meter.setCompany(company);
        meter.setUpdateFrequency(updateFrequency);
        meter.setUnit(unit);
        meter.setCreatedBy(user.getId());
        meter.setDemo(true);
        return meterRepository.save(meter);
    }

    // --- Part, Vendor, and Customer Creation Methods ---

    private Part createPart(String name, String code, PartCategory category, Company company,
                            Long quantity, double cost, OwnUser user) {
        Part part = new Part();
        part.setName(name);
        part.setBarcode(code);
        part.setCategory(category);
        part.setCompany(company);
        part.setQuantity(quantity);
        part.setCost(cost);
        part.setCreatedBy(user.getId());
        part.setDemo(true);
        return partRepository.save(part);
    }

    private Vendor createVendor(String name, String companyName, String phone, String email, Company company,
                                long hourlyRate, OwnUser user) {
        Vendor vendor = new Vendor();
        vendor.setName(name);
        vendor.setCompanyName(companyName);
        vendor.setPhone(phone);
        vendor.setEmail(email);
        vendor.setCompany(company);
        vendor.setRate(hourlyRate);
        vendor.setCreatedBy(user.getId());
        vendor.setDemo(true);
        return vendorRepository.save(vendor);
    }

    private Customer createCustomer(String name, Company company, String phone, String website, String address,
                                    long hourlyRate, String type, OwnUser user) {
        Customer customer = new Customer();
        customer.setName(name);
        customer.setCompany(company);
        customer.setPhone(phone);
        customer.setWebsite(website);
        customer.setCustomerType(type);
        customer.setAddress(address);
        customer.setRate(hourlyRate);
        customer.setCreatedBy(user.getId());
        customer.setDemo(true);
        return customerRepository.save(customer);
    }

    // --- Maintenance and Order Creation Methods ---

    private PreventiveMaintenance createPreventiveMaintenance(String name, String workOrderTitle, Asset asset,
                                                              Company company,
                                                              int frequency,
                                                              RecurrenceType recurrenceType,
                                                              RecurrenceBasedOn recurrenceBasedOn,
                                                              List<Integer> daysOfWeek, OwnUser user) {
        PreventiveMaintenance pm = new PreventiveMaintenance();
        pm.setName(name);
        pm.setTitle(workOrderTitle);
        pm.setAsset(asset);
        pm.setCompany(company);
        pm.setCreatedBy(user.getId());
        pm.setDemo(true);

        Schedule schedule = new Schedule(pm);
        schedule.setFrequency(frequency);
        schedule.setCreatedBy(user.getId());
        schedule.setRecurrenceType(recurrenceType);
        schedule.setRecurrenceBasedOn(recurrenceBasedOn);
        schedule.setDaysOfWeek(daysOfWeek);
        schedule.setDueDateDelay(1);
        schedule.setEndsOn(Helper.incrementDays(new Date(), 100));
        schedule.setDemo(true);

        pm.setSchedule(schedule);

        pm = preventiveMaintenanceRepository.save(pm);
        scheduleService.scheduleWorkOrder(pm.getSchedule());
        return pm;
    }

    private WorkOrder createWorkOrder(String title, String description, WorkOrderCategory category, Asset asset,
                                      Location location, OwnUser assignedTo, Date creationDate,
                                      Status status, Priority priority, Company company, OwnUser user) {
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
        workOrder.setCreatedBy(user.getId());
        workOrder.setDemo(true);
        workOrder.setCustomId(workOrderService.getWorkOrderNumber(company));
        return workOrderRepository.save(workOrder);
    }

    private Request createRequest(String title, String description, Location location, OwnUser requester,
                                  Date creationDate, Company company, OwnUser user) {
        Request request = new Request();
        request.setTitle(title);
        request.setDescription(description);
        request.setLocation(location);
        request.setCreatedAt(creationDate);
        request.setCompany(company);
        request.setCreatedBy(user.getId());
        request.setDemo(true);
        return requestService.create(request, company);
    }


    private void addLaborToWorkOrder(WorkOrder workOrder, OwnUser user, TimeCategory category,
                                     long hourlyRate, long hours, Company company) {
        Labor labor = new Labor(user, hourlyRate, new Date(), workOrder, false, TimeStatus.STOPPED);
        labor.setTimeCategory(category);
        labor.setDuration(hours * 3600);
        labor.setCompany(company);
        labor.setCreatedBy(user.getId());
        labor.setDemo(true);
        laborRepository.save(labor);
    }

    private void addPartToWorkOrder(WorkOrder workOrder, Part part, double quantity, Company company, OwnUser user) {
        PartQuantity partQuantity = new PartQuantity(part, workOrder, null, quantity);
        partQuantity.setCompany(company);
        partQuantity.setDemo(true);
        partQuantity.setCreatedBy(user.getId());
        partQuantityRepository.save(partQuantity);
    }

    private void addCostToWorkOrder(WorkOrder workOrder, CostCategory category, String description, double cost,
                                    Date date, OwnUser user) {
        AdditionalCost additionalCost = new AdditionalCost();
        additionalCost.setCategory(category);
        additionalCost.setCost(cost);
        additionalCost.setWorkOrder(workOrder);
        additionalCost.setDescription(description);
        additionalCost.setDemo(true);
        additionalCost.setCreatedBy(user.getId());
        additionalCost.setDate(date);
        additionalCostRepository.save(additionalCost);
    }

    @Transactional
    public void deleteDemoData(Long companyId) {
        additionalCostRepository.deleteByWorkOrder_Company_IdAndIsDemoTrue(companyId);
        partQuantityRepository.deleteByCompany_IdAndIsDemoTrue(companyId);
        laborRepository.deleteByCompany_IdAndIsDemoTrue(companyId);
        requestRepository.deleteByCompany_IdAndIsDemoTrue(companyId);
        workOrderRepository.deleteByCompany_IdAndIsDemoTrue(companyId);
        preventiveMaintenanceRepository.deleteByCompany_IdAndIsDemoTrue(companyId);
        scheduleService.deleteByCompanyIdAndIsDemoTrue(companyId);
        customerRepository.deleteByCompany_IdAndIsDemoTrue(companyId);
        vendorRepository.deleteByCompany_IdAndIsDemoTrue(companyId);
        meterRepository.deleteByCompany_IdAndIsDemoTrue(companyId);
        partRepository.deleteByCompany_IdAndIsDemoTrue(companyId);
        assetRepository.deleteByCompany_IdAndIsDemoTrue(companyId);
        locationRepository.deleteByCompany_IdAndIsDemoTrue(companyId);
        purchaseOrderCategoryRepository.deleteByCompanySettings_Company_IdAndIsDemoTrue(companyId);
        partCategoryRepository.deleteByCompanySettings_Company_IdAndIsDemoTrue(companyId);
        costCategoryRepository.deleteByCompanySettings_Company_IdAndIsDemoTrue(companyId);
        timeCategoryRepository.deleteByCompanySettings_Company_IdAndIsDemoTrue(companyId);
        meterCategoryRepository.deleteByCompanySettings_Company_IdAndIsDemoTrue(companyId);
        assetCategoryRepository.deleteByCompanySettings_Company_IdAndIsDemoTrue(companyId);
        workOrderCategoryRepository.deleteByCompanySettings_Company_IdAndIsDemoTrue(companyId);
    }
}