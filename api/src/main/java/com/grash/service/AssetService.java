package com.grash.service;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.advancedsearch.SpecificationBuilder;
import com.grash.dto.AssetPatchDTO;
import com.grash.dto.AssetShowDTO;
import com.grash.dto.imports.AssetImportDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.AssetMapper;
import com.grash.model.*;
import com.grash.model.enums.AssetStatus;
import com.grash.model.enums.NotificationType;
import com.grash.repository.AssetRepository;
import com.grash.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.transaction.Transactional;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssetService {
    private final AssetRepository assetRepository;
    private LocationService locationService;
    private final FileService fileService;
    private final AssetCategoryService assetCategoryService;
    private final DeprecationService deprecationService;
    private final UserService userService;
    private final CustomerService customerService;
    private final VendorService vendorService;
    private LaborService laborService;
    private final NotificationService notificationService;
    private final TeamService teamService;
    private final PartService partService;
    private final AssetMapper assetMapper;
    private final EntityManager em;
    private final AssetDowntimeService assetDowntimeService;
    private WorkOrderService workOrderService;
    private final MessageSource messageSource;
    private final CustomSequenceService customSequenceService;

    @Autowired
    public void setDeps(@Lazy LocationService locationService, @Lazy LaborService laborService,
                        @Lazy WorkOrderService workOrderService
    ) {
        this.locationService = locationService;
        this.laborService = laborService;
        this.workOrderService = workOrderService;
    }

    @Transactional
    public Asset create(Asset asset, OwnUser user) {
        // Generate custom ID
        Company company = user.getCompany();
        Long nextSequence = customSequenceService.getNextAssetSequence(company);
        asset.setCustomId("A" + String.format("%06d", nextSequence));

        Asset savedAsset = assetRepository.saveAndFlush(asset);
        em.refresh(savedAsset);
        return savedAsset;
    }

    @Transactional
    public Asset update(Long id, AssetPatchDTO asset) {
        if (assetRepository.existsById(id)) {
            Asset savedAsset = assetRepository.findById(id).get();
            Asset patchedAsset = assetRepository.saveAndFlush(assetMapper.updateAsset(savedAsset, asset));
            em.refresh(patchedAsset);
            return patchedAsset;
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    public Asset save(Asset asset) {
        return assetRepository.save(asset);
    }

    public Collection<Asset> getAll() {
        return assetRepository.findAll();
    }

    public void delete(Long id) {
        assetRepository.deleteById(id);
    }

    public Optional<Asset> findById(Long id) {
        return assetRepository.findById(id);
    }

    public Optional<Asset> findByNfcIdAndCompany(String nfcId, Long companyId) {
        return assetRepository.findByNfcIdAndCompany_Id(nfcId, companyId);
    }

    public List<Asset> findByCompany(Long id) {
        return assetRepository.findByCompany_Id(id);
    }

    public List<Asset> findByCompany(Long id, Sort sort) {
        return assetRepository.findByCompany_Id(id, sort);
    }

    public List<Asset> findByCompanyAndBefore(Long id, Date date) {
        return assetRepository.findByCompany_IdAndCreatedAtBefore(id, date);
    }

    public List<Asset> findAssetChildren(Long id, Sort sort) {
        return assetRepository.findByParentAsset_Id(id, sort);
    }

    public void notify(Asset asset, String title, String message) {
        notificationService.createMultiple(asset.getUsers().stream().map(user -> new Notification(message, user,
                NotificationType.ASSET, asset.getId())).collect(Collectors.toList()), true, title);
    }

    public void patchNotify(Asset oldAsset, Asset newAsset, Locale locale) {
        String title = messageSource.getMessage("new_assignment", null, locale);
        String message = messageSource.getMessage("notification_asset_assigned", new Object[]{newAsset.getName()},
                locale);
        notificationService.createMultiple(oldAsset.getNewUsersToNotify(newAsset.getUsers()).stream().map(user ->
                new Notification(message, user, NotificationType.ASSET, newAsset.getId())).collect(Collectors.toList()), true, title);
    }

    public List<Asset> findByLocation(Long id) {
        return assetRepository.findByLocation_Id(id);
    }

    private void stopAssetDowntime(Asset asset) {
        Collection<AssetDowntime> assetDowntimes = assetDowntimeService.findByAsset(asset.getId());
        Optional<AssetDowntime> optionalRunningDowntime =
                assetDowntimes.stream().filter(downtime -> downtime.getDuration() == 0).findFirst();

        if (optionalRunningDowntime.isPresent()) {
            AssetDowntime runningDowntime = optionalRunningDowntime.get();
            runningDowntime.setDuration(Helper.getDateDiff(runningDowntime.getStartsOn(), new Date(),
                    TimeUnit.SECONDS));
            assetDowntimeService.save(runningDowntime);
        }

        asset.setStatus(AssetStatus.OPERATIONAL);
        save(asset);
    }

    private void recursivelyStopChildrenDowntime(Asset parentAsset) {
        List<Asset> children = findAssetChildren(parentAsset.getId(), null);
        for (Asset child : children) {
            stopAssetDowntime(child);
            recursivelyStopChildrenDowntime(child);
        }
    }

    public void stopDownTime(Long id, Locale locale) {
        Asset savedAsset = findById(id).orElseThrow(() -> new EntityNotFoundException("Asset not found"));
        stopAssetDowntime(savedAsset);
        recursivelyStopChildrenDowntime(savedAsset);
        String message = messageSource.getMessage("notification_asset_operational",
                new Object[]{savedAsset.getName()}, locale);
        notify(savedAsset, message, messageSource.getMessage("asset_status_change", null, locale));
    }

    public void triggerDownTime(Long id, Locale locale, AssetStatus status) {
        Date now = new Date();
        Asset asset = findById(id).get();
        createAssetDowntime(asset, now, asset.getCompany());
        Asset parentAsset = asset.getParentAsset();
        while (parentAsset != null) {
            createAssetDowntime(parentAsset, now, asset.getCompany());
            if (!parentAsset.getStatus().isReallyDown()) {
                parentAsset.setStatus(status);
                save(parentAsset);
            }
            parentAsset = parentAsset.getParentAsset();
        }
        asset.setStatus(status);
        save(asset);
        String message = messageSource.getMessage("notification_asset_down", new Object[]{asset.getName()}, locale);
        notify(asset, message, messageSource.getMessage("asset_status_change", null, locale));

    }

    private void createAssetDowntime(Asset asset, Date startsOn, Company company) {
        AssetDowntime downtime = AssetDowntime.builder()
                .startsOn(startsOn)
                .asset(asset)
                .build();
        downtime.setCompany(company);
        assetDowntimeService.create(downtime);
    }

    public boolean isAssetInCompany(Asset asset, long companyId, boolean optional) {
        if (optional) {
            Optional<Asset> optionalAsset = asset == null ? Optional.empty() : findById(asset.getId());
            return asset == null || (optionalAsset.isPresent() && optionalAsset.get().getCompany().getId().equals(companyId));
        } else {
            Optional<Asset> optionalAsset = findById(asset.getId());
            return optionalAsset.isPresent() && optionalAsset.get().getCompany().getId().equals(companyId);
        }
    }

    public Page<AssetShowDTO> findBySearchCriteria(SearchCriteria searchCriteria) {
        SpecificationBuilder<Asset> builder = new SpecificationBuilder<>();
        searchCriteria.getFilterFields().forEach(builder::with);
        Pageable page = PageRequest.of(searchCriteria.getPageNum(), searchCriteria.getPageSize(),
                searchCriteria.getDirection(), searchCriteria.getSortField());
        return assetRepository.findAll(builder.build(), page).map(asset -> assetMapper.toShowDto(asset, this));
    }

    public Optional<Asset> findByNameIgnoreCaseAndCompany(String assetName, Long companyId) {
        return assetRepository.findByNameIgnoreCaseAndCompany_Id(assetName, companyId);
    }

    public void importAsset(Asset asset, AssetImportDTO dto, Company company) {
        Long companySettingsId = company.getCompanySettings().getId();
        Long companyId = company.getId();
        asset.setArea(dto.getArea());
        if (dto.getBarCode() != null) {
            Optional<Asset> optionalAssetWithSameBarCode = findByBarcodeAndCompany(dto.getBarCode(), company.getId());
            if (optionalAssetWithSameBarCode.isPresent()) {
                boolean hasError = false;
                if (dto.getId() == null) {//creation
                    hasError = true;
                } else {//update
                    if (!dto.getId().equals(optionalAssetWithSameBarCode.get().getId())) {
                        hasError = true;
                    }
                }
                if (hasError)
                    throw new CustomException("Asset with same barcode exists: " + dto.getBarCode(),
                            HttpStatus.NOT_ACCEPTABLE);
            }
        }
        asset.setBarCode(dto.getBarCode());
        asset.setArea(dto.getArea());
        asset.setArchived(Helper.getBooleanFromString(dto.getArchived()));
        asset.setDescription(dto.getDescription());
        asset.setModel(dto.getModel());
        asset.setPower(dto.getPower());
        asset.setManufacturer(dto.getManufacturer());
        Optional<Location> optionalLocation = locationService.findByNameIgnoreCaseAndCompany(dto.getLocationName(),
                companyId);
        optionalLocation.ifPresent(asset::setLocation);
        Optional<Asset> optionalAsset = findByNameIgnoreCaseAndCompany(dto.getParentAssetName(), companyId);
        optionalAsset.ifPresent(asset::setParentAsset);
        Optional<AssetCategory> optionalAssetCategory =
                assetCategoryService.findByNameIgnoreCaseAndCompanySettings(dto.getCategory(), companySettingsId);
        optionalAssetCategory.ifPresent(asset::setCategory);
        asset.setName(dto.getName());
        Optional<OwnUser> optionalPrimaryUser = userService.findByEmailAndCompany(dto.getPrimaryUserEmail(), companyId);
        optionalPrimaryUser.ifPresent(asset::setPrimaryUser);
        asset.setWarrantyExpirationDate(Helper.getDateFromExcelDate(dto.getWarrantyExpirationDate()));
        asset.setAdditionalInfos(dto.getAdditionalInfos());
        asset.setSerialNumber(dto.getSerialNumber());
        List<OwnUser> assignedTo = new ArrayList<>();
        dto.getAssignedToEmails().forEach(email -> {
            Optional<OwnUser> optionalUser1 = userService.findByEmailAndCompany(email, companyId);
            optionalUser1.ifPresent(assignedTo::add);
        });
        asset.setAssignedTo(assignedTo);
        List<Team> teams = new ArrayList<>();
        dto.getTeamsNames().forEach(teamName -> {
            Optional<Team> optionalTeam = teamService.findByNameIgnoreCaseAndCompany(teamName, companyId);
            optionalTeam.ifPresent(teams::add);
        });
        asset.setTeams(teams);
        asset.setStatus(AssetStatus.getAssetStatusFromString(dto.getStatus(), Helper.getLocale(company),
                messageSource));
        asset.setAcquisitionCost(dto.getAcquisitionCost());
        List<Customer> customers = new ArrayList<>();
        dto.getCustomersNames().forEach(name -> {
            Optional<Customer> optionalCustomer = customerService.findByNameIgnoreCaseAndCompany(name, companyId);
            optionalCustomer.ifPresent(customers::add);
        });
        asset.setCustomers(customers);
        List<Vendor> vendors = new ArrayList<>();
        dto.getVendorsNames().forEach(name -> {
            Optional<Vendor> optionalVendor = vendorService.findByNameIgnoreCaseAndCompany(name, companyId);
            optionalVendor.ifPresent(vendors::add);
        });
        asset.setVendors(vendors);
        List<Part> parts = new ArrayList<>();
        dto.getPartsNames().forEach(name -> {
            Optional<Part> optionalPart = partService.findByNameIgnoreCaseAndCompany(name, companyId);
            optionalPart.ifPresent(parts::add);
        });
        asset.setParts(parts);

        assetRepository.save(asset);
    }

    public Optional<Asset> findByIdAndCompany(Long id, Long companyId) {
        return assetRepository.findByIdAndCompany_Id(id, companyId);
    }

    public Optional<Asset> findByBarcodeAndCompany(String data, Long id) {
        return assetRepository.findByBarCodeAndCompany_Id(data, id);
    }


    public static List<AssetImportDTO> orderAssets(List<AssetImportDTO> assets) {
        Map<String, List<AssetImportDTO>> assetMap = new HashMap<>();
        List<AssetImportDTO> topLevelAssets = new ArrayList<>();

        // Group assets by parent name
        for (AssetImportDTO asset : assets) {
            String parentName = asset.getParentAssetName();
            assetMap.computeIfAbsent(parentName, k -> new ArrayList<>()).add(asset);
            if (parentName == null || assets.stream().noneMatch(assetImportDTO -> assetImportDTO.getName().equals(parentName))) {
                topLevelAssets.add(asset);
            }
        }

        // Order assets recursively
        List<AssetImportDTO> orderedAssets = new ArrayList<>();
        orderAssetsRecursive(assetMap, topLevelAssets, orderedAssets);

        return orderedAssets;
    }

    private static void orderAssetsRecursive(Map<String, List<AssetImportDTO>> assetMap, List<AssetImportDTO> assets,
                                             List<AssetImportDTO> orderedAssets) {
        for (AssetImportDTO asset : assets) {
            orderedAssets.add(asset);
            List<AssetImportDTO> children = assetMap.get(asset.getName());
            if (children != null) {
                orderAssetsRecursive(assetMap, children, orderedAssets);
            }
        }
    }

    public Boolean hasChildren(Long assetId) {
        return assetRepository.countByParentAsset_Id(assetId) > 0;
    }

    // Stats
    public long getMTBFLF(Long assetId, Date start, Date end) {
        Asset asset = findById(assetId).get();
        Collection<AssetDowntime> downtimes = assetDowntimeService.findByAssetAndStartsOnBetween(assetId, start, end);
        long downtimesDuration = downtimes.stream().mapToLong(AssetDowntime::getDuration).sum();
        long age = asset.getAge();
        return downtimes.isEmpty() ? 0 : ((age - downtimesDuration) / 60) / downtimes.size();
    }

    public long getMTBF(Long assetId, Date start, Date end) {
        List<AssetDowntime> downtimes = assetDowntimeService.findByAssetAndStartsOnBetween(assetId, start, end);
        downtimes.sort(Comparator.comparing(AssetDowntime::getStartsOn));
        if (downtimes.size() < 2) {
            return 0L;
        }

        long intervalsSum = 0;
        int numberOfIntervals = downtimes.size() - 1;

        for (int i = 0; i < downtimes.size() - 1; i++) {
            AssetDowntime currentDowntime = downtimes.get(i);
            AssetDowntime nextDowntime = downtimes.get(i + 1);

            long interval = Helper.getDateDiff(currentDowntime.getEndsOn(), nextDowntime.getStartsOn(), TimeUnit.DAYS);
            intervalsSum += interval;
        }

        return intervalsSum / numberOfIntervals;
    }

    public long getMTTR(Long assetId, Date start, Date end) {
        Collection<WorkOrder> workOrders = workOrderService.findByAssetAndCreatedAtBetween(assetId, start, end);
        List<Labor> labors = new ArrayList<>();
        for (WorkOrder workOrder : workOrders) {
            labors.addAll(laborService.findByWorkOrder(workOrder.getId()));
        }
        return workOrders.isEmpty() ? 0 : (Labor.getTotalWorkDuration(labors) / 60) / workOrders.size();
    }

    public long getDowntime(Long assetId, Date start, Date end) {
        Collection<AssetDowntime> downtimes = assetDowntimeService.findByAssetAndStartsOnBetween(assetId, start, end);
        return downtimes.stream().mapToLong(AssetDowntime::getDuration).sum();
    }

    public long getUptime(Long assetId, Date start, Date end) {
        Asset asset = findById(assetId).get();
        return asset.getAge() - getDowntime(assetId, start, end);
    }

    public double getTotalCost(Long assetId, Date start, Date end, Boolean includeLaborCost) {
        Collection<WorkOrder> workOrders = workOrderService.findByAssetAndCreatedAtBetween(assetId, start, end);
        return workOrderService.getAllCost(workOrders, includeLaborCost);
    }
}
