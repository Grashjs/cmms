package com.grash.service;

import com.grash.advancedsearch.FilterField;
import com.grash.advancedsearch.FilterField;
import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.CalendarEvent;
import com.grash.dto.PreventiveMaintenancePatchDTO;
import com.grash.dto.PreventiveMaintenanceShowDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.PreventiveMaintenanceMapper;
import com.grash.model.Company;
import com.grash.model.OwnUser;
import com.grash.model.PreventiveMaintenance;
import com.grash.model.Schedule;
import com.grash.repository.PreventiveMaintenanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

import javax.persistence.EntityManager;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PreventiveMaintenanceServiceTest {

    @Mock
    private PreventiveMaintenanceRepository preventiveMaintenanceRepository;
    @Mock
    private PreventiveMaintenanceMapper preventiveMaintenanceMapper;
    @Mock
    private CustomSequenceService customSequenceService;
    @Mock
    private EntityManager em;

    @InjectMocks
    private PreventiveMaintenanceService preventiveMaintenanceService;

    private PreventiveMaintenance preventiveMaintenance;
    private Company company;
    private OwnUser user;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);

        user = new OwnUser();
        user.setCompany(company);

        preventiveMaintenance = new PreventiveMaintenance();
        preventiveMaintenance.setId(1L);
        preventiveMaintenance.setCompany(company);

        Schedule schedule = new Schedule();
        schedule.setStartsOn(new Date());
        schedule.setFrequency(7);
        preventiveMaintenance.setSchedule(schedule);
    }

    @Nested
    @DisplayName("Write Operations")
    class WriteTests {

        @Test
        @DisplayName("should create a preventive maintenance")
        void create() {
            when(customSequenceService.getNextPreventiveMaintenanceSequence(any(Company.class))).thenReturn(1L);
            when(preventiveMaintenanceRepository.saveAndFlush(any(PreventiveMaintenance.class))).thenReturn(preventiveMaintenance);

            PreventiveMaintenance result = preventiveMaintenanceService.create(preventiveMaintenance, user);

            assertNotNull(result);
            assertEquals("PM000001", result.getCustomId());
            verify(preventiveMaintenanceRepository).saveAndFlush(preventiveMaintenance);
            verify(em).refresh(preventiveMaintenance);
        }

        @Test
        @DisplayName("should update a preventive maintenance")
        void update() {
            PreventiveMaintenancePatchDTO patchDTO = new PreventiveMaintenancePatchDTO();
            when(preventiveMaintenanceRepository.existsById(1L)).thenReturn(true);
            when(preventiveMaintenanceRepository.findById(1L)).thenReturn(Optional.of(preventiveMaintenance));
            when(preventiveMaintenanceMapper.updatePreventiveMaintenance(any(PreventiveMaintenance.class), any(PreventiveMaintenancePatchDTO.class))).thenReturn(preventiveMaintenance);
            when(preventiveMaintenanceRepository.saveAndFlush(any(PreventiveMaintenance.class))).thenReturn(preventiveMaintenance);

            PreventiveMaintenance result = preventiveMaintenanceService.update(1L, patchDTO);

            assertNotNull(result);
            verify(preventiveMaintenanceRepository).saveAndFlush(preventiveMaintenance);
            verify(em).refresh(preventiveMaintenance);
        }

        @Test
        @DisplayName("should throw exception when updating non-existent preventive maintenance")
        void updateNotFound() {
            PreventiveMaintenancePatchDTO patchDTO = new PreventiveMaintenancePatchDTO();
            when(preventiveMaintenanceRepository.existsById(1L)).thenReturn(false);

            CustomException exception = assertThrows(CustomException.class, () -> {
                preventiveMaintenanceService.update(1L, patchDTO);
            });

            assertEquals("Not found", exception.getMessage());
            assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        }

        @Test
        @DisplayName("should delete a preventive maintenance")
        void deletePM() {
            doNothing().when(preventiveMaintenanceRepository).deleteById(1L);
            preventiveMaintenanceService.delete(1L);
            verify(preventiveMaintenanceRepository).deleteById(1L);
        }
    }

    @Nested
    @DisplayName("Read Operations")
    class ReadTests {

        @Test
        @DisplayName("should get all preventive maintenances")
        void getAll() {
            when(preventiveMaintenanceRepository.findAll()).thenReturn(Collections.singletonList(preventiveMaintenance));
            Collection<PreventiveMaintenance> result = preventiveMaintenanceService.getAll();
            assertFalse(result.isEmpty());
            assertEquals(1, result.size());
            verify(preventiveMaintenanceRepository).findAll();
        }

        @Test
        @DisplayName("should find a preventive maintenance by id")
        void findById() {
            when(preventiveMaintenanceRepository.findById(1L)).thenReturn(Optional.of(preventiveMaintenance));
            Optional<PreventiveMaintenance> result = preventiveMaintenanceService.findById(1L);
            assertTrue(result.isPresent());
            assertEquals(preventiveMaintenance.getId(), result.get().getId());
            verify(preventiveMaintenanceRepository).findById(1L);
        }

        @Test
        @DisplayName("should find preventive maintenances by company")
        void findByCompany() {
            when(preventiveMaintenanceRepository.findByCompany_Id(1L)).thenReturn(Collections.singletonList(preventiveMaintenance));
            Collection<PreventiveMaintenance> result = preventiveMaintenanceService.findByCompany(1L);
            assertFalse(result.isEmpty());
            assertEquals(1, result.size());
            verify(preventiveMaintenanceRepository).findByCompany_Id(1L);
        }

        @Test
        @DisplayName("should find by search criteria")
        void findBySearchCriteria() {
            SearchCriteria searchCriteria = new SearchCriteria();
            searchCriteria.setFilterFields(Collections.singletonList(FilterField.builder().field("id").value("1").operation("eq").build()));
            searchCriteria.setPageNum(0);
            searchCriteria.setPageSize(10);
            searchCriteria.setSortField("id");

            Page<PreventiveMaintenance> page = new PageImpl<>(Collections.singletonList(preventiveMaintenance));
            PreventiveMaintenanceShowDTO dto = new PreventiveMaintenanceShowDTO();

            when(preventiveMaintenanceRepository.findAll((Specification<PreventiveMaintenance>) any(), any(Pageable.class))).thenReturn(page);
            when(preventiveMaintenanceMapper.toShowDto(any(PreventiveMaintenance.class))).thenReturn(dto);

            Page<PreventiveMaintenanceShowDTO> result = preventiveMaintenanceService.findBySearchCriteria(searchCriteria);

            assertEquals(1, result.getTotalElements());
            assertEquals(dto, result.getContent().get(0));
            verify(preventiveMaintenanceRepository).findAll((Specification<PreventiveMaintenance>) any(), any(Pageable.class));
            verify(preventiveMaintenanceMapper).toShowDto(preventiveMaintenance);
        }
    }

    @Nested
    @DisplayName("Business Logic")
    class BusinessLogicTests {

        @Test
        @DisplayName("isPreventiveMaintenanceInCompany should return true for null optional PM")
        void isPreventiveMaintenanceInCompany_nullOptional() {
            assertTrue(preventiveMaintenanceService.isPreventiveMaintenanceInCompany(null, 1L, true));
        }

        @Test
        @DisplayName("isPreventiveMaintenanceInCompany should return true for matching company")
        void isPreventiveMaintenanceInCompany_matchingCompany() {
            when(preventiveMaintenanceRepository.findById(1L)).thenReturn(Optional.of(preventiveMaintenance));
            assertTrue(preventiveMaintenanceService.isPreventiveMaintenanceInCompany(preventiveMaintenance, 1L, false));
        }

        @Test
        @DisplayName("isPreventiveMaintenanceInCompany should return false for non-matching company")
        void isPreventiveMaintenanceInCompany_nonMatchingCompany() {
            when(preventiveMaintenanceRepository.findById(1L)).thenReturn(Optional.of(preventiveMaintenance));
            assertFalse(preventiveMaintenanceService.isPreventiveMaintenanceInCompany(preventiveMaintenance, 2L, false));
        }

        @Test
        @DisplayName("should get events")
        void getEvents() {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MONTH, 1);
            Date endDate = cal.getTime();

            when(preventiveMaintenanceRepository.findByCreatedAtBeforeAndCompany_Id(any(), anyLong()))
                    .thenReturn(Collections.singletonList(preventiveMaintenance));

            List<CalendarEvent<PreventiveMaintenance>> events = preventiveMaintenanceService.getEvents(endDate, 1L);

            assertFalse(events.isEmpty());
            // With a frequency of 7 days over one month, we expect 4 or 5 events.
            assertTrue(events.size() >= 4 && events.size() <= 5);
        }

        @Test
        @DisplayName("should not get events for disabled schedule")
        void getEvents_disabledSchedule() {
            preventiveMaintenance.getSchedule().setDisabled(true);
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MONTH, 1);
            Date endDate = cal.getTime();

            when(preventiveMaintenanceRepository.findByCreatedAtBeforeAndCompany_Id(any(), anyLong()))
                    .thenReturn(Collections.singletonList(preventiveMaintenance));

            List<CalendarEvent<PreventiveMaintenance>> events = preventiveMaintenanceService.getEvents(endDate, 1L);

            assertTrue(events.isEmpty());
        }
    }
}
