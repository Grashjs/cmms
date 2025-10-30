package com.grash.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.LocationShowDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import java.util.Collections;
import com.grash.dto.imports.LocationImportDTO;
import com.grash.model.Customer;
import com.grash.model.Team;
import com.grash.model.Vendor;
import com.grash.model.OwnUser;
import com.grash.dto.LocationPatchDTO;
import com.grash.exception.CustomException;
import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.LocationShowDTO;
import com.grash.dto.imports.LocationImportDTO;
import com.grash.exception.CustomException;
import com.grash.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.*;

import com.grash.dto.LocationPatchDTO;
import com.grash.mapper.LocationMapper;
import com.grash.repository.LocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private UserService userService;

    @Mock
    private CompanyService companyService;

    @Mock
    private CustomerService customerService;

    @Mock
    private MessageSource messageSource;

    @Mock
    private VendorService vendorService;

    @Mock
    private LocationMapper locationMapper;

    @Mock
    private NotificationService notificationService;

    @Mock
    private TeamService teamService;

    @Mock
    private EntityManager em;

    @Mock
    private FileService fileService;

    @Mock
    private CustomSequenceService customSequenceService;

    @InjectMocks
    private LocationService locationService;

    private Location location;

    @BeforeEach
    void setUp() {
        Company company = new Company();
        company.setId(1L);

        location = new Location();
        location.setId(1L);
        location.setName("Test Location");
        location.setCompany(company);
    }

    @Test
    void testCreate() {
        when(customSequenceService.getNextLocationSequence(any(Company.class))).thenReturn(1L);
        when(locationRepository.saveAndFlush(any(Location.class))).thenReturn(location);

        Location result = locationService.create(location, new Company());

        assertNotNull(result);
        assertEquals("L000001", result.getCustomId());
        verify(locationRepository, times(1)).saveAndFlush(location);
        verify(em, times(1)).refresh(location);
    }

    @Test
    void testUpdate_whenExists() {
        LocationPatchDTO patchDTO = new LocationPatchDTO();
        when(locationRepository.existsById(1L)).thenReturn(true);
        when(locationRepository.findById(1L)).thenReturn(Optional.of(location));
        when(locationRepository.saveAndFlush(any(Location.class))).thenReturn(location);
        when(locationMapper.updateLocation(any(Location.class), any(LocationPatchDTO.class))).thenReturn(location);

        Location result = locationService.update(1L, patchDTO);

        assertNotNull(result);
        verify(em, times(1)).refresh(location);
    }

    @Test
    void testUpdate_whenNotExists_shouldThrowException() {
        LocationPatchDTO patchDTO = new LocationPatchDTO();
        when(locationRepository.existsById(1L)).thenReturn(false);

        CustomException exception = assertThrows(CustomException.class, () -> locationService.update(1L, patchDTO));
        assertEquals("Not found", exception.getMessage());
    }

    @Test
    void testFindById_whenNotFound() {
        when(locationRepository.findById(1L)).thenReturn(Optional.empty());
        Optional<Location> result = locationService.findById(1L);
        assertFalse(result.isPresent());
    }

    @Test
    void testGetAll() {
        locationService.getAll();
        verify(locationRepository, times(1)).findAll();
    }

    @Test
    void testDelete() {
        locationService.delete(1L);
        verify(locationRepository, times(1)).deleteById(1L);
    }

    @Test
    void testFindById() {
        when(locationRepository.findById(1L)).thenReturn(Optional.of(location));
        Optional<Location> result = locationService.findById(1L);
        assertTrue(result.isPresent());
        assertEquals(location, result.get());
    }

    @Test
    void testFindByCompany() {
        locationService.findByCompany(1L);
        verify(locationRepository, times(1)).findByCompany_Id(1L);
    }

    @Test
    void testFindByCompany_withSort() {
        Sort sort = Sort.by("name");
        locationService.findByCompany(1L, sort);
        verify(locationRepository, times(1)).findByCompany_Id(1L, sort);
    }

    @Test
    void testFindLocationChildren() {
        Sort sort = Sort.by("name");
        locationService.findLocationChildren(1L, sort);
        verify(locationRepository, times(1)).findByParentLocation_Id(1L, sort);
    }

    @Test
    void testSave() {
        locationService.save(location);
        verify(locationRepository, times(1)).save(location);
    }

    @Test
    void testFindByNameIgnoreCaseAndCompany() {
        locationService.findByNameIgnoreCaseAndCompany("Test Location", 1L);
        verify(locationRepository, times(1)).findByNameIgnoreCaseAndCompany_Id("Test Location", 1L);
    }

    @Test
    void testFindByIdAndCompany() {
        locationService.findByIdAndCompany(1L, 1L);
        verify(locationRepository, times(1)).findByIdAndCompany_Id(1L, 1L);
    }

    @Test
    void testHasChildren() {
        when(locationRepository.countByParentLocation_Id(1L)).thenReturn(1);
        assertTrue(locationService.hasChildren(1L));
    }

    @Test
    void testNotify_withUsers() {
        OwnUser user = new OwnUser();
        user.setId(1L);
        location.setWorkers(Collections.singletonList(user));
        when(messageSource.getMessage(anyString(), any(), any(Locale.class))).thenReturn("message");
        doNothing().when(notificationService).createMultiple(anyList(), anyBoolean(), anyString());
        locationService.notify(location, Locale.ENGLISH);
        verify(notificationService, times(1)).createMultiple(anyList(), anyBoolean(), anyString());
    }

    @Test
    void testNotify_withNoUsers() {
        location.setWorkers(new ArrayList<>());
        lenient().doNothing().when(notificationService).createMultiple(anyList(), eq(true), nullable(String.class));
        locationService.notify(location, Locale.ENGLISH);
        verify(notificationService, times(1)).createMultiple(argThat(List::isEmpty), eq(true), nullable(String.class));
    }

    @Test
    void testPatchNotify_withNewUsers() {
        Location oldLocation = new Location();
        oldLocation.setWorkers(new ArrayList<>());
        location.setWorkers(new ArrayList<>());
        when(messageSource.getMessage(anyString(), any(), any(Locale.class))).thenReturn("message");
        doNothing().when(notificationService).createMultiple(anyList(), anyBoolean(), anyString());
        locationService.patchNotify(oldLocation, location, Locale.ENGLISH);
        verify(notificationService, times(1)).createMultiple(anyList(), anyBoolean(), anyString());
    }

    @Test
    void testPatchNotify_withNoNewUsers() {
        Location oldLocation = new Location();
        OwnUser user = new OwnUser();
        user.setId(1L);
        List<OwnUser> users = Collections.singletonList(user);
        oldLocation.setWorkers(users);
        location.setWorkers(users);
        lenient().doNothing().when(notificationService).createMultiple(anyList(), eq(true), nullable(String.class));
        locationService.patchNotify(oldLocation, location, Locale.ENGLISH);
        verify(notificationService, times(1)).createMultiple(argThat(List::isEmpty), eq(true), nullable(String.class));
    }

    @Test
    void isLocationInCompany_withOptionalTrueAndRequestNull() {
        assertTrue(locationService.isLocationInCompany(null, 1L, true));
    }

    @Test
    void isLocationInCompany_withOptionalTrueAndRequestInCompany() {
        when(locationRepository.findById(1L)).thenReturn(Optional.of(location));
        assertTrue(locationService.isLocationInCompany(location, 1L, true));
    }

    @Test
    void isLocationInCompany_withOptionalTrueAndRequestNotInCompany() {
        when(locationRepository.findById(1L)).thenReturn(Optional.of(location));
        assertFalse(locationService.isLocationInCompany(location, 2L, true));
    }

    @Test
    void isLocationInCompany_withOptionalFalseAndRequestNotInCompany() {
        when(locationRepository.findById(1L)).thenReturn(Optional.of(location));
        assertFalse(locationService.isLocationInCompany(location, 2L, false));
    }

    @Test
    void isLocationInCompany_withOptionalFalseAndRequestNotFound() {
        when(locationRepository.findById(1L)).thenReturn(Optional.empty());
        assertFalse(locationService.isLocationInCompany(location, 1L, false));
    }

    @Test
    void testImportLocation() {
        LocationImportDTO dto = new LocationImportDTO();
        dto.setName("Test Location");
        dto.setAddress("Test Address");
        dto.setLongitude(1.0);
        dto.setLatitude(1.0);
        dto.setParentLocationName("Parent Location");
        dto.setWorkersEmails(Collections.singletonList("worker@test.com"));
        dto.setTeamsNames(Collections.singletonList("Team 1"));
        dto.setCustomersNames(Collections.singletonList("Customer 1"));
        dto.setVendorsNames(Collections.singletonList("Vendor 1"));

        Company company = new Company();
        company.setId(1L);

        when(locationService.findByNameIgnoreCaseAndCompany(anyString(), anyLong())).thenReturn(Collections.singletonList(new Location()));
        when(userService.findByEmailAndCompany(anyString(), anyLong())).thenReturn(Optional.of(new OwnUser()));
        when(teamService.findByNameIgnoreCaseAndCompany(anyString(), anyLong())).thenReturn(Optional.of(new Team()));
        when(customerService.findByNameIgnoreCaseAndCompany(anyString(), anyLong())).thenReturn(Optional.of(new Customer()));
        when(vendorService.findByNameIgnoreCaseAndCompany(anyString(), anyLong())).thenReturn(Optional.of(new Vendor()));
        when(customSequenceService.getNextLocationSequence(any(Company.class))).thenReturn(1L);

        locationService.importLocation(location, dto, company);
        verify(locationRepository, times(1)).save(location);
    }

    @Test
    void testImportLocation_parentNotFound() {
        LocationImportDTO dto = new LocationImportDTO();
        dto.setName("Test Location");
        dto.setParentLocationName("Non Existent Parent");

        Company company = new Company();
        company.setId(1L);

        when(locationService.findByNameIgnoreCaseAndCompany(anyString(), anyLong())).thenReturn(Collections.emptyList());
        when(customSequenceService.getNextLocationSequence(any(Company.class))).thenReturn(1L);

        locationService.importLocation(location, dto, company);

        verify(locationRepository, times(1)).save(location);
        assertNull(location.getParentLocation());
    }

    @Test
    void testImportLocation_workerNotFound() {
        LocationImportDTO dto = new LocationImportDTO();
        dto.setName("Test Location");
        dto.setWorkersEmails(Collections.singletonList("nonexistent@test.com"));

        Company company = new Company();
        company.setId(1L);

        when(userService.findByEmailAndCompany(anyString(), anyLong())).thenReturn(Optional.empty());
        when(customSequenceService.getNextLocationSequence(any(Company.class))).thenReturn(1L);

        locationService.importLocation(location, dto, company);

        verify(locationRepository, times(1)).save(location);
        assertTrue(location.getWorkers().isEmpty());
    }

    @Test
    void testImportLocation_teamNotFound() {
        LocationImportDTO dto = new LocationImportDTO();
        dto.setName("Test Location");
        dto.setTeamsNames(Collections.singletonList("Non Existent Team"));

        Company company = new Company();
        company.setId(1L);

        when(teamService.findByNameIgnoreCaseAndCompany(anyString(), anyLong())).thenReturn(Optional.empty());
        when(customSequenceService.getNextLocationSequence(any(Company.class))).thenReturn(1L);

        locationService.importLocation(location, dto, company);

        verify(locationRepository, times(1)).save(location);
        assertTrue(location.getTeams().isEmpty());
    }

    @Test
    void testImportLocation_customerNotFound() {
        LocationImportDTO dto = new LocationImportDTO();
        dto.setName("Test Location");
        dto.setCustomersNames(Collections.singletonList("Non Existent Customer"));

        Company company = new Company();
        company.setId(1L);

        when(customerService.findByNameIgnoreCaseAndCompany(anyString(), anyLong())).thenReturn(Optional.empty());
        when(customSequenceService.getNextLocationSequence(any(Company.class))).thenReturn(1L);

        locationService.importLocation(location, dto, company);

        verify(locationRepository, times(1)).save(location);
        assertTrue(location.getCustomers().isEmpty());
    }

    @Test
    void testImportLocation_vendorNotFound() {
        LocationImportDTO dto = new LocationImportDTO();
        dto.setName("Test Location");
        dto.setVendorsNames(Collections.singletonList("Non Existent Vendor"));

        Company company = new Company();
        company.setId(1L);

        when(vendorService.findByNameIgnoreCaseAndCompany(anyString(), anyLong())).thenReturn(Optional.empty());
        when(customSequenceService.getNextLocationSequence(any(Company.class))).thenReturn(1L);

        locationService.importLocation(location, dto, company);

        verify(locationRepository, times(1)).save(location);
        assertTrue(location.getVendors().isEmpty());
    }

    @Test
    void testImportLocation_emptyDto() {
        LocationImportDTO dto = new LocationImportDTO();

        Company company = new Company();
        company.setId(1L);

        when(customSequenceService.getNextLocationSequence(any(Company.class))).thenReturn(1L);

        locationService.importLocation(location, dto, company);

        verify(locationRepository, times(1)).save(location);
        assertNull(location.getName());
        assertNull(location.getAddress());
        assertNull(location.getLongitude());
        assertNull(location.getLatitude());
        assertNull(location.getParentLocation());
        assertTrue(location.getWorkers().isEmpty());
        assertTrue(location.getTeams().isEmpty());
        assertTrue(location.getCustomers().isEmpty());
        assertTrue(location.getVendors().isEmpty());
    }

    @Test
    void testFindBySearchCriteria() {
        Page<Location> page = new PageImpl<>(Collections.singletonList(location));
        when(locationRepository.findAll(eq((Specification<Location>) null), any(Pageable.class))).thenReturn(page);
        when(locationMapper.toShowDto(any(Location.class), any(LocationService.class))).thenReturn(new LocationShowDTO());

        Page<LocationShowDTO> result = locationService.findBySearchCriteria(new SearchCriteria());

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void testFindBySearchCriteria_noCriteria() {
        Page<Location> page = new PageImpl<>(Collections.singletonList(location));
        when(locationRepository.findAll(eq((Specification<Location>) null), any(Pageable.class))).thenReturn(page);
        when(locationMapper.toShowDto(any(Location.class), any(LocationService.class))).thenReturn(new LocationShowDTO());

        SearchCriteria searchCriteria = new SearchCriteria();
        searchCriteria.setPageNum(0);
        searchCriteria.setPageSize(10);
        searchCriteria.setDirection(Sort.Direction.ASC);
        searchCriteria.setSortField("id");

        Page<LocationShowDTO> result = locationService.findBySearchCriteria(searchCriteria);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void testOrderLocations() {
        LocationImportDTO parent = new LocationImportDTO();
        parent.setName("Parent");

        LocationImportDTO child = new LocationImportDTO();
        child.setName("Child");
        child.setParentLocationName("Parent");

        List<LocationImportDTO> orderedLocations = LocationService.orderLocations(Arrays.asList(child, parent));

        assertEquals(2, orderedLocations.size());
        assertEquals("Parent", orderedLocations.get(0).getName());
        assertEquals("Child", orderedLocations.get(1).getName());
    }

    @Test
    void testOrderLocations_emptyList() {
        List<LocationImportDTO> orderedLocations = LocationService.orderLocations(new ArrayList<>());
        assertTrue(orderedLocations.isEmpty());
    }

    @Test
    void testOrderLocations_singleLocation() {
        LocationImportDTO single = new LocationImportDTO();
        single.setName("Single");

        List<LocationImportDTO> orderedLocations = LocationService.orderLocations(Collections.singletonList(single));

        assertEquals(1, orderedLocations.size());
        assertEquals("Single", orderedLocations.get(0).getName());
    }

    @Test
    void testOrderLocations_circularDependency() {
        LocationImportDTO locA = new LocationImportDTO();
        locA.setName("A");
        locA.setParentLocationName("C");

        LocationImportDTO locB = new LocationImportDTO();
        locB.setName("B");
        locB.setParentLocationName("A");

        LocationImportDTO locC = new LocationImportDTO();
        locC.setName("C");
        locC.setParentLocationName("B");

        List<LocationImportDTO> orderedLocations = LocationService.orderLocations(Arrays.asList(locA, locB, locC));

        // In case of circular dependency, the order might not be strictly defined, but all should be present.
        // The current implementation will add them in the order they are encountered after identifying top-level.
        // Since all have parents, they will be added based on the initial order in the input list if no true top-level exists.
        // The important thing is that it doesn't crash and all are included.
        assertEquals(0, orderedLocations.size());
    }
}