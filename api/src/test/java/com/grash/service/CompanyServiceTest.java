
package com.grash.service;

import com.grash.dto.CompanyPatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.CompanyMapper;
import com.grash.model.Company;
import com.grash.model.Subscription;
import com.grash.model.SubscriptionPlan;
import com.grash.model.enums.PlanFeatures;
import com.grash.repository.CompanyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.persistence.EntityManager;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CompanyMapper companyMapper;

    @Mock
    private EntityManager em;

    @InjectMocks
    private CompanyService companyService;

    private Company company;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);
        company.setName("Test Company");
    }

    @Test
    void testCreateCompany() {
        when(companyRepository.save(any(Company.class))).thenReturn(company);
        Company createdCompany = companyService.create(company);
        assertNotNull(createdCompany);
        assertEquals("Test Company", createdCompany.getName());
        verify(companyRepository, times(1)).save(company);
    }

    @Test
    void testUpdateCompany() {
        when(companyRepository.save(any(Company.class))).thenReturn(company);
        Company updatedCompany = companyService.update(company);
        assertNotNull(updatedCompany);
        assertEquals("Test Company", updatedCompany.getName());
        verify(companyRepository, times(1)).save(company);
    }

    @Test
    void testGetAllCompanies() {
        companyService.getAll();
        verify(companyRepository, times(1)).findAll();
    }

    @Test
    void testDeleteCompany() {
        companyService.delete(1L);
        verify(companyRepository, times(1)).deleteById(1L);
    }

    @Test
    void testFindCompanyById() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        Optional<Company> foundCompany = companyService.findById(1L);
        assertTrue(foundCompany.isPresent());
        assertEquals("Test Company", foundCompany.get().getName());
        verify(companyRepository, times(1)).findById(1L);
    }

    @Test
    void testUpdateCompanyPatch() {
        CompanyPatchDTO patchDTO = new CompanyPatchDTO();
        patchDTO.setName("Updated Company");

        when(companyRepository.existsById(1L)).thenReturn(true);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(companyMapper.updateCompany(any(Company.class), any(CompanyPatchDTO.class))).thenReturn(company);
        when(companyRepository.saveAndFlush(any(Company.class))).thenReturn(company);

        Company updatedCompany = companyService.update(1L, patchDTO);

        assertNotNull(updatedCompany);
        verify(em, times(1)).refresh(any(Company.class));
    }

    @Test
    void testUpdateCompanyPatchNotFound() {
        CompanyPatchDTO patchDTO = new CompanyPatchDTO();
        patchDTO.setName("Updated Company");

        when(companyRepository.existsById(1L)).thenReturn(false);

        assertThrows(CustomException.class, () -> {
            companyService.update(1L, patchDTO);
        });
    }
}
