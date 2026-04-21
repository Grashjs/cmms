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
import com.grash.model.enums.CustomFieldEntityType;
import com.grash.model.enums.webhook.WebhookEvent;
import com.grash.repository.VendorRepository;
import com.grash.service.CustomFieldValueService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VendorService {
    private final VendorRepository vendorRepository;
    private final CompanyService companyService;
    private final VendorMapper vendorMapper;
    private final LicenseService licenseService;
    private final WebhookDispatchService webhookDispatchService;
    private final CustomFieldValueService customFieldValueService;

    public Vendor create(Vendor vendor) {
        if (!licenseService.hasEntitlement(LicenseEntitlement.CUSTOMER_VENDOR))
            throw new CustomException("You need a license to create a vendor", HttpStatus.FORBIDDEN);
        Company company = vendor.getCompany();
        if (vendor instanceof VendorPostDTO vendorPostDTO) {
            vendor = vendorMapper.fromPostDto(vendorPostDTO);
            if (vendorPostDTO.getCustomFields() != null && !vendorPostDTO.getCustomFields().isEmpty()) {
                setVendorCustomFields(vendor, vendorPostDTO.getCustomFields(), company);
            }
        }
        Vendor savedVendor = vendorRepository.save(vendor);
        Map<String, Object> webhookPayload = new HashMap<>();
        webhookPayload.put("vendorId", savedVendor.getId());
        webhookDispatchService.dispatchWebhook(savedVendor.getCompany(), WebhookEvent.NEW_VENDOR, webhookPayload,
                "newVendor", savedVendor, null, null, null, null, null);
        return savedVendor;
    }

    private void setVendorCustomFields(Vendor vendor, List<CustomFieldValuePostDTO> customFieldValuePostDTOS, Company company) {
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
            return vendorRepository.save(vendorMapper.updateVendor(savedVendor, vendor));
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

    public boolean isVendorInCompany(Vendor vendor, long companyId, boolean optional) {
        if (optional) {
            Optional<Vendor> optionalVendor = vendor == null ? Optional.empty() : findById(vendor.getId());
            return vendor == null || (optionalVendor.isPresent() && optionalVendor.get().getCompany().getId().equals(companyId));
        } else {
            Optional<Vendor> optionalVendor = findById(vendor.getId());
            return optionalVendor.isPresent() && optionalVendor.get().getCompany().getId().equals(companyId);
        }
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

}
