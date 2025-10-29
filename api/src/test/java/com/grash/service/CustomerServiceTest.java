package com.grash.service;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.CustomerPatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.CustomerMapper;
import com.grash.model.Company;
import com.grash.model.Customer;
import com.grash.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerMapper customerMapper;

    @InjectMocks
    private CustomerService customerService;

    private Customer customer;
    private Company company;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);

        customer = new Customer();
        customer.setId(1L);
        customer.setName("Test Customer");
        customer.setCompany(company);
    }

    @Test
    void create() {
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        Customer result = customerService.create(customer);

        assertNotNull(result);
        assertEquals(customer.getId(), result.getId());
        verify(customerRepository).save(customer);
    }

    @Test
    void update_whenExists() {
        CustomerPatchDTO patchDTO = new CustomerPatchDTO();
        when(customerRepository.existsById(1L)).thenReturn(true);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);
        when(customerMapper.updateCustomer(any(Customer.class), any(CustomerPatchDTO.class))).thenReturn(customer);

        Customer result = customerService.update(1L, patchDTO);

        assertNotNull(result);
        assertEquals(customer.getId(), result.getId());
        verify(customerRepository).save(customer);
    }

    @Test
    void update_whenNotExists_shouldThrowException() {
        CustomerPatchDTO patchDTO = new CustomerPatchDTO();
        when(customerRepository.existsById(1L)).thenReturn(false);

        CustomException exception = assertThrows(CustomException.class, () -> customerService.update(1L, patchDTO));

        assertEquals("Not found", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    }

    @Test
    void getAll() {
        when(customerRepository.findAll()).thenReturn(Collections.singletonList(customer));

        assertEquals(1, customerService.getAll().size());
    }

    @Test
    void delete() {
        doNothing().when(customerRepository).deleteById(1L);
        customerService.delete(1L);
        verify(customerRepository).deleteById(1L);
    }

    @Test
    void findById() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        assertTrue(customerService.findById(1L).isPresent());
    }

    @Test
    void findByCompany() {
        when(customerRepository.findByCompany_Id(1L)).thenReturn(Collections.singletonList(customer));

        assertEquals(1, customerService.findByCompany(1L).size());
    }

    @Test
    void findBySearchCriteria() {
        SearchCriteria searchCriteria = new SearchCriteria();
        searchCriteria.setPageNum(0);
        searchCriteria.setPageSize(10);
        searchCriteria.setSortField("id");
        searchCriteria.setDirection(Sort.Direction.ASC);
        searchCriteria.setFilterFields(Collections.emptyList());

        Page<Customer> result = customerService.findBySearchCriteria(searchCriteria);
    }

    @Test
    void findByNameIgnoreCaseAndCompany() {
        when(customerRepository.findByNameIgnoreCaseAndCompany_Id("Test Customer", 1L))
                .thenReturn(Optional.of(customer));

        assertTrue(customerService.findByNameIgnoreCaseAndCompany("Test Customer", 1L).isPresent());
    }
}
