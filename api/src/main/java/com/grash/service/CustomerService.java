package com.grash.service;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.advancedsearch.SpecificationBuilder;
import com.grash.dto.CustomerPatchDTO;
import com.grash.dto.CustomerPostDTO;
import com.grash.dto.cutomField.CustomFieldValuePostDTO;
import com.grash.dto.license.LicenseEntitlement;
import com.grash.exception.CustomException;
import com.grash.mapper.CustomerMapper;
import com.grash.model.Customer;
import com.grash.model.Company;
import com.grash.model.enums.CustomFieldEntityType;
import com.grash.repository.CustomerRepository;
import com.grash.service.CustomFieldValueService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final LicenseService licenseService;
    private final CustomFieldValueService customFieldValueService;


    public Customer create(Customer customer) {
        if (!licenseService.hasEntitlement(LicenseEntitlement.CUSTOMER_VENDOR))
            throw new CustomException("You need a license to create a contractor", HttpStatus.FORBIDDEN);
        Company company = customer.getCompany();
        if (customer instanceof CustomerPostDTO customerPostDTO) {
            customer = customerMapper.fromPostDto(customerPostDTO);
            if (customerPostDTO.getCustomFields() != null && !customerPostDTO.getCustomFields().isEmpty()) {
                setCustomerCustomFields(customer, customerPostDTO.getCustomFields(), company);
            }
        }
        return customerRepository.save(customer);
    }

    private void setCustomerCustomFields(Customer customer, List<CustomFieldValuePostDTO> customFieldValuePostDTOS, Company company) {
        customFieldValueService.setCustomFields(
                customer,
                customer.getCustomFieldValues(),
                customFieldValuePostDTOS,
                company,
                CustomFieldEntityType.CUSTOMER,
                cfv -> cfv.setCustomer(customer)
        );
    }

    public Customer update(Long id, CustomerPatchDTO customer, Company company) {
        if (customerRepository.existsById(id)) {
            Customer savedCustomer = customerRepository.findById(id).get();
            if (customer.getCustomFields() != null && !customer.getCustomFields().isEmpty()) {
                setCustomerCustomFields(savedCustomer, customer.getCustomFields(), company);
            }
            return customerRepository.save(customerMapper.updateCustomer(savedCustomer, customer));
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    public Collection<Customer> getAll() {
        return customerRepository.findAll();
    }

    public void delete(Long id) {
        customerRepository.deleteById(id);
    }

    public Optional<Customer> findById(Long id) {
        return customerRepository.findById(id);
    }

    public Collection<Customer> findByCompany(Long id) {
        return customerRepository.findByCompany_Id(id);
    }

    public Page<Customer> findBySearchCriteria(SearchCriteria searchCriteria) {
        SpecificationBuilder<Customer> builder = new SpecificationBuilder<>();
        searchCriteria.getFilterFields().forEach(builder::with);
        Pageable page = PageRequest.of(searchCriteria.getPageNum(), searchCriteria.getPageSize(),
                searchCriteria.getDirection(), searchCriteria.getSortField());
        return customerRepository.findAll(builder.build(), page);
    }

    public Optional<Customer> findByNameIgnoreCaseAndCompany(String name, Long companyId) {
        return customerRepository.findByNameIgnoreCaseAndCompany_Id(name, companyId).stream().findFirst();
    }
}
