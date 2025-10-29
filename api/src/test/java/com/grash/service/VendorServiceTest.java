package com.grash.service;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.VendorPatchDTO;
import com.grash.dto.VendorShowDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.VendorMapper;
import com.grash.model.Company;
import com.grash.model.Vendor;
import com.grash.repository.VendorRepository;
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
class VendorServiceTest {

    @Mock
    private VendorRepository vendorRepository;

    @Mock
    private VendorMapper vendorMapper;

    @InjectMocks
    private VendorService vendorService;

    private Vendor vendor;
    private Company company;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);

        vendor = new Vendor();
        vendor.setId(1L);
        vendor.setName("Test Vendor");
        vendor.setCompany(company);
    }

    @Test
    void create() {
        when(vendorRepository.save(any(Vendor.class))).thenReturn(vendor);

        Vendor result = vendorService.create(vendor);

        assertNotNull(result);
        assertEquals(vendor.getId(), result.getId());
        verify(vendorRepository).save(vendor);
    }

    @Test
    void update_whenExists() {
        VendorPatchDTO patchDTO = new VendorPatchDTO();
        when(vendorRepository.existsById(1L)).thenReturn(true);
        when(vendorRepository.findById(1L)).thenReturn(Optional.of(vendor));
        when(vendorRepository.save(any(Vendor.class))).thenReturn(vendor);
        when(vendorMapper.updateVendor(any(Vendor.class), any(VendorPatchDTO.class))).thenReturn(vendor);

        Vendor result = vendorService.update(1L, patchDTO);

        assertNotNull(result);
        assertEquals(vendor.getId(), result.getId());
        verify(vendorRepository).save(vendor);
    }

    @Test
    void update_whenNotExists_shouldThrowException() {
        VendorPatchDTO patchDTO = new VendorPatchDTO();
        when(vendorRepository.existsById(1L)).thenReturn(false);

        CustomException exception = assertThrows(CustomException.class, () -> vendorService.update(1L, patchDTO));

        assertEquals("Not found", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    }

    @Test
    void getAll() {
        when(vendorRepository.findAll()).thenReturn(Collections.singletonList(vendor));

        assertEquals(1, vendorService.getAll().size());
    }

    @Test
    void delete() {
        doNothing().when(vendorRepository).deleteById(1L);
        vendorService.delete(1L);
        verify(vendorRepository).deleteById(1L);
    }

    @Test
    void findById() {
        when(vendorRepository.findById(1L)).thenReturn(Optional.of(vendor));

        assertTrue(vendorService.findById(1L).isPresent());
    }

    @Test
    void findByCompany() {
        when(vendorRepository.findByCompany_Id(1L)).thenReturn(Collections.singletonList(vendor));

        assertEquals(1, vendorService.findByCompany(1L).size());
    }

    @Test
    void findBySearchCriteria() {
        Page<Vendor> page = new PageImpl<>(Collections.singletonList(vendor));
        when(vendorRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        SearchCriteria searchCriteria = new SearchCriteria();
        searchCriteria.setPageNum(0);
        searchCriteria.setPageSize(10);
        searchCriteria.setSortField("id");
        searchCriteria.setDirection(Sort.Direction.ASC);
        searchCriteria.setFilterFields(Collections.emptyList());

        Page<Vendor> result = vendorService.findBySearchCriteria(searchCriteria);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void findByNameIgnoreCaseAndCompany() {
        when(vendorRepository.findByNameIgnoreCaseAndCompany_Id("Test Vendor", 1L)).thenReturn(Optional.of(vendor));

        assertTrue(vendorService.findByNameIgnoreCaseAndCompany("Test Vendor", 1L).isPresent());
    }
}
