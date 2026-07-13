package com.grash.service;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.advancedsearch.SpecificationBuilder;
import com.grash.dto.PartPatchDTO;
import com.grash.dto.PartShowDTO;
import com.grash.dto.imports.PartImportDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.PartMapper;
import com.grash.model.*;
import com.grash.model.enums.NotificationType;
import com.grash.repository.PartRepository;
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

import javax.persistence.EntityManager;
import java.util.*;
import java.util.stream.Collectors;

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

    @Transactional
    public Part create(Part Part) {
        Part savedPart = partRepository.saveAndFlush(Part);
        em.refresh(savedPart);
        return savedPart;
    }

    @Transactional
    public Part update(Long id, PartPatchDTO part) {
        if (partRepository.existsById(id)) {
            Part savedPart = partRepository.findById(id).get();
            Part patchedPart = partRepository.saveAndFlush(partMapper.updatePart(savedPart, part));
            em.refresh(patchedPart);
            return patchedPart;

        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    public void consumePart(Long id, double quantity, WorkOrder workOrder, Locale locale) {
        Part part = findById(id).get();
        part.setQuantity(part.getQuantity() - quantity);
        if (quantity < 0) {
            PartConsumption partConsumption =
                    Collections.max(partConsumptionService.findByWorkOrderAndPart(workOrder.getId(), part.getId()),
                            new AuditComparator());
            partConsumption.setQuantity(partConsumption.getQuantity() + quantity);
            partRepository.save(part);
            partConsumptionService.save(partConsumption);
        } else {
            String message = messageSource.getMessage("notification_part_low", new Object[]{part.getName()}, locale);
            if (part.getQuantity() >= quantity) {
                if (part.getQuantity() < part.getMinQuantity()) {
                    notificationService.createMultiple(part.getAssignedTo().stream().map(user ->
                            new Notification(message, user, NotificationType.PART, part.getId())
                    ).collect(Collectors.toList()), true, message);
                }
                partConsumptionService.create(new PartConsumption(part, workOrder, quantity));
                partRepository.save(part);
            } else throw new CustomException("There is not enough of this part", HttpStatus.NOT_ACCEPTABLE);
        }
    }

    public Collection<Part> getAll() {
        return partRepository.findAll();
    }

    public void delete(Long id) {
        partRepository.deleteById(id);
    }

    public Optional<Part> findById(Long id) {
        return partRepository.findById(id);
    }

    public Collection<Part> findByCompany(Long id) {
        return partRepository.findByCompany_Id(id);
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
        Long companyId = company.getId();
        Long companySettingsId = company.getCompanySettings().getId();
        part.setName(dto.getName());
        part.setCost(dto.getCost());
        Optional<PartCategory> optionalPartCategory =
                partCategoryService.findByNameIgnoreCaseAndCompanySettings(dto.getCategory(), companySettingsId);
        optionalPartCategory.ifPresent(part::setCategory);
        part.setNonStock(Helper.getBooleanFromString(dto.getCategory()));
        if (dto.getBarcode() != null) {
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
        List<OwnUser> users = new ArrayList<>();
        dto.getAssignedToEmails().forEach(email -> {
            Optional<OwnUser> optionalUser1 = userService.findByEmailAndCompany(email, companyId);
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
        partRepository.save(part);
    }

    public Optional<Part> findByIdAndCompany(Long id, Long companyId) {
        return partRepository.findByIdAndCompany_Id(id, companyId);
    }

    public Optional<Part> findByNameIgnoreCaseAndCompany(String name, Long companyId) {
        return partRepository.findByNameIgnoreCaseAndCompany_Id(name, companyId);
    }

    public Optional<Part> findByBarcodeAndCompany(String barcode, Long companyId) {
        return partRepository.findByBarcodeAndCompany_Id(barcode, companyId);
    }
}
