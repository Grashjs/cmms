package com.grash.service;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.advancedsearch.SpecificationBuilder;
import com.grash.dto.CustomerPatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.CustomerMapper;
import com.grash.model.Customer;
import com.grash.model.OwnUser;
import com.grash.model.enums.RoleType;
import com.grash.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final CompanyService companyService;
    private PartService partService;
    private LocationService locationService;
    private AssetService assetService;
    private final CustomerMapper customerMapper;

    @Autowired
    public void setDeps(@Lazy PartService partService, @Lazy LocationService locationService,
                        @Lazy AssetService assetService
    ) {
        this.partService = partService;
        this.locationService = locationService;
        this.assetService = assetService;
    }

    public Customer create(Customer Customer) {
        return customerRepository.save(Customer);
    }

    public Customer update(Long id, CustomerPatchDTO customer) {
        if (customerRepository.existsById(id)) {
            Customer savedCustomer = customerRepository.findById(id).get();
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
        return customerRepository.findByNameIgnoreCaseAndCompany_Id(name, companyId);
    }
}
