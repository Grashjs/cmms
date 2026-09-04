package com.grash.service;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.advancedsearch.SpecificationBuilder;
import com.grash.dto.VendorPatchDTO;
import com.grash.dto.VendorPostDTO;
import com.grash.dto.cutomField.CustomFieldValuePostDTO;
import com.grash.dto.license.LicenseEntitlement;
import com.grash.exception.CustomException;
import com.grash.mapper.VendorMapper;
import com.grash.model.Vendor;
import com.grash.model.Company;
import com.grash.model.User;
import com.grash.model.enums.CustomFieldEntityType;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.RoleType;
import com.grash.model.enums.webhook.WebhookEvent;
import com.grash.repository.VendorRepository;
import com.grash.utils.Sanitizer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VendorService {
    private final VendorRepository vendorRepository;
    private final VendorMapper vendorMapper;
    private final LicenseService licenseService;
    private final WebhookDispatchService webhookDispatchService;
    private final CustomFieldValueService customFieldValueService;

    private void setVendorCustomFields(Vendor vendor, List<CustomFieldValuePostDTO> customFieldValuePostDTOS,
                                       Company company) {
        customFieldValueService.setCustomFields(
                vendor,
                vendor.getCustomFieldValues(),
                customFieldValuePostDTOS,
                company,
                CustomFieldEntityType.VENDOR,
                cfv -> cfv.setVendor(vendor)
        );
    }

    public Vendor update(Long id, VendorPatchDTO vendor, Company company) {
        if (vendorRepository.existsById(id)) {
            Vendor savedVendor = vendorRepository.findById(id).get();
            if (vendor.getCustomFields() != null && !vendor.getCustomFields().isEmpty()) {
                setVendorCustomFields(savedVendor, vendor.getCustomFields(), company);
            }
            Vendor updatedVendor = vendorMapper.updateVendor(savedVendor, vendor);
            Sanitizer.sanitizeVendor(updatedVendor);
            return vendorRepository.save(updatedVendor);
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    public Collection<Vendor> getAll() {
        return vendorRepository.findAll();
    }

    public void delete(Long id) {
        vendorRepository.deleteById(id);
    }

    public Optional<Vendor> findById(Long id) {
        return vendorRepository.findById(id);
    }

    public Collection<Vendor> findByCompany(Long id) {
        return vendorRepository.findByCompany_Id(id);
    }

    public Page<Vendor> findBySearchCriteria(SearchCriteria searchCriteria) {
        SpecificationBuilder<Vendor> builder = new SpecificationBuilder<>();
        searchCriteria.getFilterFields().forEach(builder::with);
        Pageable page = PageRequest.of(searchCriteria.getPageNum(), searchCriteria.getPageSize(),
                searchCriteria.getDirection(), searchCriteria.getSortField());
        return vendorRepository.findAll(builder.build(), page);
    }

    public Optional<Vendor> findByNameIgnoreCaseAndCompany(String name, Long companyId) {
        return vendorRepository.findByNameIgnoreCaseAndCompany_Id(name, companyId);
    }

    public SearchCriteria getSearchCriteria(User user, SearchCriteria searchCriteria) {
        if (user.getRole().getRoleType().equals(RoleType.ROLE_CLIENT)) {
            if (user.getRole().getViewPermissions().contains(PermissionEntity.VENDORS_AND_CUSTOMERS)) {
                searchCriteria.filterCompany(user);
            } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
        }
        return searchCriteria;
    }

    public Vendor getById(Long id, User user) {
        Optional<Vendor> optionalVendor = vendorRepository.findById(id);
        if (optionalVendor.isPresent()) {
            Vendor savedVendor = optionalVendor.get();
            if (savedVendor.canBeViewedBy(user)) {
                return savedVendor;
            } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @Transactional
    public Vendor create(VendorPostDTO vendorReq, User user) {
        if (user.getRole().getCreatePermissions().contains(PermissionEntity.VENDORS_AND_CUSTOMERS)) {
            if (!licenseService.hasEntitlement(LicenseEntitlement.CUSTOMER_VENDOR))
                throw new CustomException("You need a license to create a vendor", HttpStatus.FORBIDDEN);
            Vendor vendor = vendorMapper.fromPostDto(vendorReq);
            if (vendorReq.getCustomFields() != null && !vendorReq.getCustomFields().isEmpty()) {
                setVendorCustomFields(vendor, vendorReq.getCustomFields(), user.getCompany());
            }
            Sanitizer.sanitizeVendor(vendor);
            Vendor savedVendor = vendorRepository.save(vendor);
            Map<String, Object> webhookPayload = new HashMap<>();
            webhookPayload.put("vendorId", savedVendor.getId());
            webhookDispatchService.dispatchWebhook(savedVendor.getCompany(), WebhookEvent.NEW_VENDOR, webhookPayload,
                    "newVendor", savedVendor, null, null, null, null, null);
            return savedVendor;
        } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
    }

    @Transactional
    public Vendor patch(Long id, VendorPatchDTO vendor, User user) {
        Optional<Vendor> optionalVendor = vendorRepository.findById(id);
        if (optionalVendor.isPresent()) {
            Vendor savedVendor = optionalVendor.get();
            if (savedVendor.canBeEditedBy(user)) {
                return update(id, vendor, user.getCompany());
            } else throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        } else throw new CustomException("Vendor not found", HttpStatus.NOT_FOUND);
    }

    @Transactional
    public void deleteByIdAndUser(Long id, User user) {
        Optional<Vendor> optionalVendor = vendorRepository.findById(id);
        if (optionalVendor.isPresent()) {
            Vendor savedVendor = optionalVendor.get();
            if (savedVendor.canBeDeletedBy(user)) {
                delete(id);
            } else throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        } else throw new CustomException("Vendor not found", HttpStatus.NOT_FOUND);
    }

}
