package com.grash.service;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.RequestPatchDTO;
import com.grash.dto.RequestShowDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.RequestMapper;
import com.grash.model.Company;
import com.grash.model.OwnUser;
import com.grash.model.Request;
import com.grash.model.WorkOrder;
import com.grash.repository.RequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

import com.grash.advancedsearch.FilterField;
import com.grash.model.enums.Priority;
import com.grash.model.CompanySettings;
import com.grash.model.GeneralPreferences;

import javax.persistence.EntityManager;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestServiceTest {

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private RequestMapper requestMapper;

    @Mock
    private CustomSequenceService customSequenceService;

    @Mock
    private WorkOrderService workOrderService;

    @Mock
    private EntityManager em;

    @InjectMocks
    private RequestService requestService;

    private Request request;
    private Company company;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);

        request = new Request();
        request.setId(1L);
        request.setCompany(company);
    }

    @Test
    void create() {
        when(customSequenceService.getNextRequestSequence(company)).thenReturn(1L);
        when(requestRepository.saveAndFlush(any(Request.class))).thenReturn(request);

        Request result = requestService.create(request, company);

        assertNotNull(result);
        assertEquals(request.getId(), result.getId());
        assertEquals("R000001", result.getCustomId());
        verify(requestRepository).saveAndFlush(request);
        verify(em).refresh(request);
    }

    @Test
    void update_whenExists() {
        RequestPatchDTO patchDTO = new RequestPatchDTO();
        when(requestRepository.existsById(1L)).thenReturn(true);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(requestRepository.saveAndFlush(any(Request.class))).thenReturn(request);
        when(requestMapper.updateRequest(any(Request.class), any(RequestPatchDTO.class))).thenReturn(request);

        Request result = requestService.update(1L, patchDTO);

        assertNotNull(result);
        assertEquals(request.getId(), result.getId());
        verify(requestRepository).saveAndFlush(request);
        verify(em).refresh(request);
    }

    @Test
    void update_whenNotExists_shouldThrowException() {
        RequestPatchDTO patchDTO = new RequestPatchDTO();
        when(requestRepository.existsById(1L)).thenReturn(false);

        CustomException exception = assertThrows(CustomException.class, () -> requestService.update(1L, patchDTO));

        assertEquals("Not found", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    }

    @Test
    void getAll() {
        when(requestRepository.findAll()).thenReturn(Collections.singletonList(request));

        assertEquals(1, requestService.getAll().size());
    }

    @Test
    void delete() {
        doNothing().when(requestRepository).deleteById(1L);
        requestService.delete(1L);
        verify(requestRepository).deleteById(1L);
    }

    @Test
    void findById() {
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

        assertTrue(requestService.findById(1L).isPresent());
    }

    @Test
    void findByCompany() {
        when(requestRepository.findByCompany_Id(1L)).thenReturn(Collections.singletonList(request));

        assertEquals(1, requestService.findByCompany(1L).size());
    }

    @Test
    void save() {
        requestService.save(request);
        verify(requestRepository).save(request);
    }

    @Test
    void findByCreatedAtBetweenAndCompany() {
        Date start = new Date();
        Date end = new Date();
        when(requestRepository.findByCreatedAtBetweenAndCompany_Id(start, end, 1L))
                .thenReturn(Collections.singletonList(request));

        assertEquals(1, requestService.findByCreatedAtBetweenAndCompany(start, end, 1L).size());
    }

    @Test
    void findBySearchCriteria() {
        Page<Request> page = new PageImpl<>(Collections.singletonList(request));
        when(requestRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(requestMapper.toShowDto(any(Request.class))).thenReturn(new RequestShowDTO());

        Page<RequestShowDTO> result = requestService.findBySearchCriteria(new SearchCriteria());

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void countPending() {
        when(requestRepository.countPending(1L)).thenReturn(1);

        assertEquals(1, requestService.countPending(1L));
    }

    @Test
    void findByCategoryAndCreatedAtBetween() {
        Date start = new Date();
        Date end = new Date();
        when(requestRepository.findByCategory_IdAndCreatedAtBetween(1L, start, end))
                .thenReturn(Collections.singletonList(request));

        assertEquals(1, requestService.findByCategoryAndCreatedAtBetween(1L, start, end).size());
    }

    @Test
    void createWorkOrderFromRequest_withAutoAssign() {
        OwnUser creator = new OwnUser();
        Company company = new Company();
        GeneralPreferences generalPreferences = new GeneralPreferences();
        generalPreferences.setAutoAssignRequests(true);
        CompanySettings companySettings = new CompanySettings();
        companySettings.setGeneralPreferences(generalPreferences);
        company.setCompanySettings(companySettings);
        creator.setCompany(company);

        WorkOrder workOrder = new WorkOrder();
        workOrder.setPrimaryUser(new OwnUser());

        when(workOrderService.getWorkOrderFromWorkOrderBase(request)).thenReturn(workOrder);
        when(workOrderService.create(any(WorkOrder.class), any(Company.class))).thenReturn(workOrder);

        WorkOrder result = requestService.createWorkOrderFromRequest(request, creator);

        assertNotNull(result);
        verify(requestRepository).save(request);
    }

    @Test
    void createWorkOrderFromRequest_withoutAutoAssign() {
        OwnUser creator = new OwnUser();
        Company company = new Company();
        GeneralPreferences generalPreferences = new GeneralPreferences();
        generalPreferences.setAutoAssignRequests(false);
        CompanySettings companySettings = new CompanySettings();
        companySettings.setGeneralPreferences(generalPreferences);
        company.setCompanySettings(companySettings);
        creator.setCompany(company);

        WorkOrder workOrder = new WorkOrder();

        when(workOrderService.getWorkOrderFromWorkOrderBase(request)).thenReturn(workOrder);
        when(workOrderService.create(any(WorkOrder.class), any(Company.class))).thenReturn(workOrder);

        WorkOrder result = requestService.createWorkOrderFromRequest(request, creator);

        assertNotNull(result);
        verify(requestRepository).save(request);
    }

    @Test
    void createWorkOrderFromRequest_withNullPrimaryUser() {
        OwnUser creator = new OwnUser();
        Company company = new Company();
        GeneralPreferences generalPreferences = new GeneralPreferences();
        generalPreferences.setAutoAssignRequests(true);
        CompanySettings companySettings = new CompanySettings();
        companySettings.setGeneralPreferences(generalPreferences);
        company.setCompanySettings(companySettings);
        creator.setCompany(company);

        WorkOrder workOrder = new WorkOrder();

        when(workOrderService.getWorkOrderFromWorkOrderBase(request)).thenReturn(workOrder);
        when(workOrderService.create(any(WorkOrder.class), any(Company.class))).thenReturn(workOrder);

        WorkOrder result = requestService.createWorkOrderFromRequest(request, creator);

        assertNotNull(result);
        assertEquals(creator, result.getPrimaryUser());
        verify(requestRepository).save(request);
    }

    @Test
    void findBySearchCriteria_withPriority() {
        SearchCriteria searchCriteria = new SearchCriteria();
        FilterField filterField = new FilterField();
        filterField.setField("priority");
        filterField.setValues(Collections.singletonList(Priority.HIGH.toString()));
        searchCriteria.getFilterFields().add(filterField);

        Page<Request> page = new PageImpl<>(Collections.singletonList(request));
        when(requestRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(requestMapper.toShowDto(any(Request.class))).thenReturn(new RequestShowDTO());

        Page<RequestShowDTO> result = requestService.findBySearchCriteria(searchCriteria);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void findBySearchCriteria_withStatusCancelled() {
        SearchCriteria searchCriteria = new SearchCriteria();
        FilterField filterField = new FilterField();
        filterField.setField("status");
        filterField.setValues(Collections.singletonList("CANCELLED"));
        searchCriteria.getFilterFields().add(filterField);

        Page<Request> page = new PageImpl<>(Collections.singletonList(request));
        when(requestRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(requestMapper.toShowDto(any(Request.class))).thenReturn(new RequestShowDTO());

        Page<RequestShowDTO> result = requestService.findBySearchCriteria(searchCriteria);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void findBySearchCriteria_withStatusApproved() {
        SearchCriteria searchCriteria = new SearchCriteria();
        FilterField filterField = new FilterField();
        filterField.setField("status");
        filterField.setValues(Collections.singletonList("APPROVED"));
        searchCriteria.getFilterFields().add(filterField);

        Page<Request> page = new PageImpl<>(Collections.singletonList(request));
        when(requestRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(requestMapper.toShowDto(any(Request.class))).thenReturn(new RequestShowDTO());

        Page<RequestShowDTO> result = requestService.findBySearchCriteria(searchCriteria);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void findBySearchCriteria_withStatusPending() {
        SearchCriteria searchCriteria = new SearchCriteria();
        FilterField filterField = new FilterField();
        filterField.setField("status");
        filterField.setValues(Collections.singletonList("PENDING"));
        searchCriteria.getFilterFields().add(filterField);

        Page<Request> page = new PageImpl<>(Collections.singletonList(request));
        when(requestRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(requestMapper.toShowDto(any(Request.class))).thenReturn(new RequestShowDTO());

        Page<RequestShowDTO> result = requestService.findBySearchCriteria(searchCriteria);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void findBySearchCriteria_withStatusUnknown() {
        SearchCriteria searchCriteria = new SearchCriteria();
        FilterField filterField = new FilterField();
        filterField.setField("status");
        filterField.setValues(Collections.singletonList("UNKNOWN"));
        searchCriteria.getFilterFields().add(filterField);

        Page<Request> page = new PageImpl<>(Collections.singletonList(request));
        when(requestRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(requestMapper.toShowDto(any(Request.class))).thenReturn(new RequestShowDTO());

        Page<RequestShowDTO> result = requestService.findBySearchCriteria(searchCriteria);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void isRequestInCompany_withOptionalTrueAndRequestNull() {
        assertTrue(requestService.isRequestInCompany(null, 1L, true));
    }

    @Test
    void isRequestInCompany_withOptionalTrueAndRequestInCompany() {
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        assertTrue(requestService.isRequestInCompany(request, 1L, true));
    }

    @Test
    void isRequestInCompany_withOptionalTrueAndRequestNotInCompany() {
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        assertFalse(requestService.isRequestInCompany(request, 2L, true));
    }

    @Test
    void isRequestInCompany_withOptionalFalseAndRequestInCompany() {
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        assertTrue(requestService.isRequestInCompany(request, 1L, false));
    }

    @Test
    void isRequestInCompany_withOptionalFalseAndRequestNotInCompany() {
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        assertFalse(requestService.isRequestInCompany(request, 2L, false));
    }

    @Test
    void isRequestInCompany_withOptionalFalseAndRequestNotFound() {
        when(requestRepository.findById(1L)).thenReturn(Optional.empty());
        assertFalse(requestService.isRequestInCompany(request, 1L, false));
    }
}
