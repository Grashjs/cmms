package com.grash.service;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.advancedsearch.SpecificationBuilder;
import com.grash.dto.PartPatchDTO;
import com.grash.dto.PartPostDTO;
import com.grash.dto.PartShowDTO;
import com.grash.dto.cutomField.CustomFieldValuePostDTO;
import com.grash.dto.imports.PartImportDTO;
import com.grash.dto.license.LicenseEntitlement;
import com.grash.exception.CustomException;
import com.grash.mapper.PartMapper;
import com.grash.model.*;
import com.grash.model.enums.CustomFieldEntityType;
import com.grash.model.enums.NotificationType;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.webhook.PartField;
import com.grash.model.enums.webhook.WebhookEvent;
import com.grash.repository.PartRepository;
import com.grash.service.CustomFieldValueService;
import com.grash.utils.AuditComparator;
import com.grash.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.grash.utils.Consts.usageBasedLicenseLimits;

@Service
@RequiredArgsConstructor
public class PartService {
    private final PartRepository partRepository;
    private final PartCategoryService partCategoryService;
    private final PartConsumptionService partConsumptionService;
    private final CompanyService companyService;
    private final CustomerService customerService;
    private final VendorService vendorService;
    private final MessageSource messageSource;
    private final LocationService locationService;
    private final PartMapper partMapper;
    private final EntityManager em;
    private final NotificationService notificationService;
    private final UserService userService;
    private final TeamService teamService;
    private final LicenseService licenseService;
    private final WebhookDispatchService webhookDispatchService;
    private final CustomFieldValueService customFieldValueService;


    @Transactional
    public Part create(Part part, User user) {
        checkUsageBasedLimit(user.getCompany());
        Company company = user.getCompany();
        if (part instanceof PartPostDTO partPostDTO) {
            part = partMapper.fromPostDto(partPostDTO);
            if (partPostDTO.getCustomFields() != null && !partPostDTO.getCustomFields().isEmpty()) {
                setPartCustomFields(part, partPostDTO.getCustomFields(), company);
            }
        }
        Part savedPart = partRepository.saveAndFlush(part);
        em.refresh(savedPart);
        Map<String, Object> webhookPayload = new HashMap<>();
        webhookPayload.put("partId", savedPart.getId());
        Object serializedPart = partMapper.toShowDto(savedPart);
        webhookDispatchService.dispatchWebhook(user.getCompany(), WebhookEvent.NEW_PART, webhookPayload,
                "newPart", serializedPart, null, null, null, null, null);
        return savedPart;
    }

    private void setPartCustomFields(Part part, List<CustomFieldValuePostDTO> customFieldValuePostDTOS,
                                     Company company) {
        customFieldValueService.setCustomFields(
                part,
                part.getCustomFieldValues(),
                customFieldValuePostDTOS,
                company,
                CustomFieldEntityType.PART,
                cfv -> cfv.setPart(part)
        );
    }

    @Transactional
    public Part update(Long id, PartPatchDTO part, Company company) {
        if (partRepository.existsById(id)) {
            Part savedPart = partRepository.findById(id).get();
            if (part.getCustomFields() != null && !part.getCustomFields().isEmpty()) {
                setPartCustomFields(savedPart, part.getCustomFields(), company);
            }
            double originalPartQuantity = savedPart.getQuantity();
            Collection<PartField> changedFields = detectPatchDTOChangedFields(savedPart, part);
            Part patchedPart = partRepository.saveAndFlush(partMapper.updatePart(savedPart, part));
            em.refresh(patchedPart);

            Map<String, Object> webhookPayload = new HashMap<>();
            webhookPayload.put("partId", patchedPart.getId());
            Object serializedPart = partMapper.toShowDto(patchedPart);
            webhookDispatchService.dispatchWebhook(patchedPart.getCompany(), WebhookEvent.PART_CHANGE, webhookPayload,
                    "changedPart", serializedPart, null, null, null, null, changedFields);

            if (changedFields.contains(PartField.QUANTITY)) {
                dispatchPartQuantityChangeWebhook(patchedPart, originalPartQuantity, patchedPart.getQuantity(),
                        null, savedPart.getCompany());
            }

            return patchedPart;

        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    private void checkUsageBasedLimit(Company company) {
        Integer threshold = usageBasedLicenseLimits.get(LicenseEntitlement.UNLIMITED_PARTS);
        if (!licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_PARTS)
                && partRepository.hasMoreThan(company.getId(), threshold.longValue() - 1
        ))
            throw new CustomException("You need a license to add a new part. Free Limit reached: " + threshold,
                    HttpStatus.FORBIDDEN);
    }

    public void consumePart(Long id, double quantity, WorkOrder workOrder, Locale locale) {
        Part part = findById(id).get();
        if (part.isNonStock()) return;
        double previousQuantity = part.getQuantity();
        part.setQuantity(part.getQuantity() - quantity);
        if (part.getQuantity() < 0)
            throw new CustomException("There is not enough of this part", HttpStatus.NOT_ACCEPTABLE);
        if (quantity < 0) {
            PartConsumption partConsumption =
                    Collections.max(partConsumptionService.findByWorkOrderAndPart(workOrder.getId(), part.getId()),
                            new AuditComparator());
            partConsumption.setQuantity(partConsumption.getQuantity() + quantity);
            partRepository.save(part);
            partConsumptionService.save(partConsumption);

            // Dispatch webhook for part quantity change (returning parts)
            dispatchPartQuantityChangeWebhook(part, previousQuantity, part.getQuantity(), workOrder,
                    workOrder.getCompany());
        } else {
            String message = messageSource.getMessage("notification_part_low", new Object[]{part.getName()}, locale);
            if (part.getQuantity() < part.getMinQuantity() && licenseService.hasEntitlement(LicenseEntitlement.LOW_STOCK_ALERTS)) {
                Map<Long, User> uniqueUsersMap = new TreeMap<>();
                Stream.concat(
                        userService.findWorkersByCompany(part.getCompany().getId())
                                .stream()
                                .filter(user -> user.getRole().getViewPermissions().contains(PermissionEntity.SETTINGS)),
                        part.getAssignedTo().stream()
                ).forEach(user -> uniqueUsersMap.put(user.getId(), user));

                notificationService.createMultiple(uniqueUsersMap.values().stream().map(user ->
                        new Notification(message, user, NotificationType.PART, part.getId())
                ).collect(Collectors.toList()), true, message);
            }
            partConsumptionService.create(new PartConsumption(part, workOrder, quantity));
            partRepository.save(part);

            // Dispatch webhook for part quantity change (consuming parts)
            dispatchPartQuantityChangeWebhook(part, previousQuantity, part.getQuantity(), workOrder,
                    workOrder.getCompany());
        }
    }

    public Collection<Part> getAll() {
        return partRepository.findAll();
    }


    @Transactional
    public void delete(Part part) {
        Map<String, Object> webhookPayload = new HashMap<>();
        webhookPayload.put("partId", part.getId());
        Object serializedPart = partMapper.toShowDto(part);
        webhookDispatchService.dispatchWebhook(part.getCompany(), WebhookEvent.PART_DELETE, webhookPayload,
                "deletePart", serializedPart, null, null, null, null, null);
        partRepository.deleteById(part.getId());
    }

    public Optional<Part> findById(Long id) {
        return partRepository.findById(id);
    }

    public Collection<Part> findByCompany(Long id) {
        return partRepository.findByCompany_Id(id);
    }

    public List<Part> findByCompanyForExport(Long companyId) {
        return partRepository.findByCompanyForExport(companyId);
    }

    public void notify(Part part, Locale locale) {
        String title = messageSource.getMessage("new_assignment", null, locale);
        String message = messageSource.getMessage("notification_part_assigned", new Object[]{part.getName()}, locale);
        notificationService.createMultiple(part.getUsers().stream().map(user -> new Notification(message, user,
                NotificationType.PART, part.getId())).collect(Collectors.toList()), true, title);
    }

    public void patchNotify(Part oldPart, Part newPart, Locale locale) {
        String title = messageSource.getMessage("new_assignment", null, locale);
        String message = messageSource.getMessage("notification_part_assigned", new Object[]{newPart.getName()},
                locale);
        notificationService.createMultiple(oldPart.getNewUsersToNotify(newPart.getUsers()).stream().map(user ->
                        new Notification(message, user, NotificationType.PART, newPart.getId())).collect(Collectors.toList())
                , true, title);
    }

    public Part save(Part part) {
        return partRepository.save(part);
    }

    public List<Part> saveAll(List<Part> parts) {
        return partRepository.saveAll(parts);
    }

    public boolean isPartInCompany(Part part, long companyId, boolean optional) {
        if (optional) {
            Optional<Part> optionalPart = part == null ? Optional.empty() : findById(part.getId());
            return part == null || (optionalPart.isPresent() && optionalPart.get().getCompany().getId().equals(companyId));
        } else {
            Optional<Part> optionalPart = findById(part.getId());
            return optionalPart.isPresent() && optionalPart.get().getCompany().getId().equals(companyId);
        }
    }

    public Page<PartShowDTO> findBySearchCriteria(SearchCriteria searchCriteria) {
        SpecificationBuilder<Part> builder = new SpecificationBuilder<>();
        searchCriteria.getFilterFields().forEach(builder::with);
        Pageable page = PageRequest.of(searchCriteria.getPageNum(), searchCriteria.getPageSize(),
                searchCriteria.getDirection(), searchCriteria.getSortField());
        return partRepository.findAll(builder.build(), page).map(partMapper::toShowDto);
    }

    public void importPart(Part part, PartImportDTO dto, Company company) {
        checkUsageBasedLimit(company);
        Long companyId = company.getId();
        Long companySettingsId = company.getCompanySettings().getId();
        part.setCompany(company);
        part.setName(dto.getName());
        part.setCost(dto.getCost());
        Optional<PartCategory> optionalPartCategory =
                partCategoryService.findByNameIgnoreCaseAndCompanySettings(dto.getCategory(), companySettingsId);
        optionalPartCategory.ifPresent(part::setCategory);
        part.setNonStock(Helper.getBooleanFromString(dto.getCategory()));
        if (dto.getBarcode() != null && !dto.getBarcode().trim().isEmpty()) {
            Optional<Part> optionalPartWithSameBarCode = findByBarcodeAndCompany(dto.getBarcode(), company.getId());
            if (optionalPartWithSameBarCode.isPresent()) {
                boolean hasError = false;
                if (dto.getId() == null) {//creation
                    hasError = true;
                } else {//update
                    if (!dto.getId().equals(optionalPartWithSameBarCode.get().getId())) {
                        hasError = true;
                    }
                }
                if (hasError)
                    throw new CustomException("Part with same barcode exists: " + dto.getBarcode(),
                            HttpStatus.NOT_ACCEPTABLE);
            }
        }
        part.setBarcode(dto.getBarcode());
        part.setDescription(dto.getDescription());
        part.setQuantity(dto.getQuantity());
        part.setAdditionalInfos(dto.getAdditionalInfos());
        part.setArea(dto.getArea());
        part.setMinQuantity(dto.getMinQuantity());
//        Optional<Location> optionalLocation = locationService.findByNameIgnoreCaseAndCompany(dto.getLocationName(),
//        companyId);
//        optionalLocation.ifPresent(part::setLocation);
        List<User> users = new ArrayList<>();
        dto.getAssignedToEmails().forEach(email -> {
            Optional<User> optionalUser1 = userService.findByEmailAndCompany(email, companyId);
            optionalUser1.ifPresent(users::add);
        });
        part.setAssignedTo(users);
        List<Team> teams = new ArrayList<>();
        dto.getTeamsNames().forEach(teamName -> {
            Optional<Team> optionalTeam = teamService.findByNameIgnoreCaseAndCompany(teamName, companyId);
            optionalTeam.ifPresent(teams::add);
        });
        part.setTeams(teams);
        List<Customer> customers = new ArrayList<>();
        dto.getCustomersNames().forEach(name -> {
            Optional<Customer> optionalCustomer = customerService.findByNameIgnoreCaseAndCompany(name, companyId);
            optionalCustomer.ifPresent(customers::add);
        });
        part.setCustomers(customers);
        List<Vendor> vendors = new ArrayList<>();
        dto.getVendorsNames().forEach(name -> {
            Optional<Vendor> optionalVendor = vendorService.findByNameIgnoreCaseAndCompany(name, companyId);
            optionalVendor.ifPresent(vendors::add);
        });
        part.setVendors(vendors);
    }

    public Optional<Part> findByIdAndCompany(Long id, Long companyId) {
        return partRepository.findByIdAndCompany_Id(id, companyId);
    }

    public List<Part> findByIdsAndCompany(List<Long> ids, Long companyId) {
        return partRepository.findByIdInAndCompany_Id(ids, companyId);
    }

    public Optional<Part> findByNameIgnoreCaseAndCompany(String name, Long companyId) {
        return partRepository.findByNameIgnoreCaseAndCompany_Id(name, companyId);
    }

    public Optional<Part> findByBarcodeAndCompany(String barcode, Long companyId) {
        return partRepository.findByBarcodeAndCompany_Id(barcode, companyId);
    }

    private Collection<PartField> detectPatchDTOChangedFields(Part original, PartPatchDTO patchDTO) {
        Collection<PartField> changedFields = new ArrayList<>();

        if (patchDTO.getName() != null && !Objects.equals(original.getName(), patchDTO.getName())) {
            changedFields.add(PartField.NAME);
        }
        if (patchDTO.getCost() != 0 && original.getCost() != patchDTO.getCost()) {
            changedFields.add(PartField.COST);
        }
        if (patchDTO.getCategory() != null && !Objects.equals(
                original.getCategory() != null ? original.getCategory().getId() : null,
                patchDTO.getCategory().getId())) {
            changedFields.add(PartField.CATEGORY);
        }
        if (patchDTO.isNonStock() != original.isNonStock()) {
            changedFields.add(PartField.NON_STOCK);
        }
        if (patchDTO.getBarcode() != null && !Objects.equals(original.getBarcode(), patchDTO.getBarcode())) {
            changedFields.add(PartField.BARCODE);
        }
        if (patchDTO.getDescription() != null && !Objects.equals(original.getDescription(),
                patchDTO.getDescription())) {
            changedFields.add(PartField.DESCRIPTION);
        }
        if (original.getQuantity() != patchDTO.getQuantity()) {
            changedFields.add(PartField.QUANTITY);
        }
        if (patchDTO.getAdditionalInfos() != null && !Objects.equals(original.getAdditionalInfos(),
                patchDTO.getAdditionalInfos())) {
            changedFields.add(PartField.ADDITIONAL_INFOS);
        }
        if (patchDTO.getArea() != null && !Objects.equals(original.getArea(), patchDTO.getArea())) {
            changedFields.add(PartField.AREA);
        }
        if (patchDTO.getMinQuantity() != 0 && original.getMinQuantity() != patchDTO.getMinQuantity()) {
            changedFields.add(PartField.MIN_QUANTITY);
        }
        if (patchDTO.getAssignedTo() != null && !collectionsMatch(original.getAssignedTo(), patchDTO.getAssignedTo(),
                User::getId)) {
            changedFields.add(PartField.ASSIGNED_TO);
        }
        if (patchDTO.getCustomers() != null && !collectionsMatch(original.getCustomers(), patchDTO.getCustomers(),
                Customer::getId)) {
            changedFields.add(PartField.CUSTOMERS);
        }
        if (patchDTO.getVendors() != null && !collectionsMatch(original.getVendors(), patchDTO.getVendors(),
                Vendor::getId)) {
            changedFields.add(PartField.VENDORS);
        }
        if (patchDTO.getTeams() != null && !collectionsMatch(original.getTeams(), patchDTO.getTeams(), Team::getId)) {
            changedFields.add(PartField.TEAMS);
        }
        if (patchDTO.getUnit() != null && !Objects.equals(original.getUnit(), patchDTO.getUnit())) {
            changedFields.add(PartField.UNIT);
        }

        return changedFields;
    }

    private <T> boolean collectionsMatch(Collection<T> a, Collection<T> b, Function<T, Long> idExtractor) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        if (a.size() != b.size()) return false;
        return a.stream().allMatch(aItem ->
                b.stream().anyMatch(bItem ->
                        idExtractor.apply(aItem).equals(idExtractor.apply(bItem))));
    }

    private void dispatchPartQuantityChangeWebhook(Part part, double previousQuantity, double newQuantity,
                                                   WorkOrder workOrder, Company company) {
        if (previousQuantity == newQuantity) return;
        Map<String, Object> webhookPayload = new HashMap<>();
        webhookPayload.put("partId", part.getId());
        webhookPayload.put("partName", part.getName());
        webhookPayload.put("previousQuantity", previousQuantity);
        webhookPayload.put("newQuantity", newQuantity);
        webhookPayload.put("changedAmount", newQuantity - previousQuantity);
        if (workOrder != null) {
            webhookPayload.put("workOrderId", workOrder.getId());
            webhookPayload.put("workOrderTitle", workOrder.getTitle());
        }
        Collection<PartField> changedFields = Collections.singletonList(PartField.QUANTITY);
        Object serializedPart = partMapper.toShowDto(part);
        webhookDispatchService.dispatchWebhook(company, WebhookEvent.PART_QUANTITY_CHANGED, webhookPayload,
                "changedPart", serializedPart, null, null, null, null, changedFields);
    }
}

