package com.grash.service;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.PartPatchDTO;
import com.grash.dto.PartShowDTO;
import com.grash.dto.imports.PartImportDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.PartMapper;
import com.grash.model.*;
import com.grash.model.enums.NotificationType;
import com.grash.repository.PartRepository;
import com.grash.utils.AuditComparator;
import com.grash.utils.Helper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

import javax.persistence.EntityManager;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PartServiceTest {

    @Mock
    private PartRepository partRepository;
    @Mock
    private PartCategoryService partCategoryService;
    @Mock
    private PartConsumptionService partConsumptionService;
    @Mock
    private CompanyService companyService;
    @Mock
    private CustomerService customerService;
    @Mock
    private VendorService vendorService;
    @Mock
    private MessageSource messageSource;
    @Mock
    private LocationService locationService;
    @Mock
    private PartMapper partMapper;
    @Mock
    private EntityManager em;
    @Mock
    private NotificationService notificationService;
    @Mock
    private UserService userService;
    @Mock
    private TeamService teamService;

    @InjectMocks
    private PartService partService;

    private Part part;
    private Company company;
    private OwnUser user;
    private CompanySettings companySettings;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);
        companySettings = new CompanySettings();
        companySettings.setId(1L);
        company.setCompanySettings(companySettings);

        user = new OwnUser();
        user.setId(1L);
        user.setEmail("test@example.com");

        part = new Part();
        part.setId(1L);
        part.setName("Test Part");
        part.setCompany(company);
        part.setQuantity(100.0);
        part.setMinQuantity(10.0);
        part.setAssignedTo(Collections.singletonList(user));
    }

    @Test
    void testCreate() {
        when(partRepository.saveAndFlush(any(Part.class))).thenReturn(part);

        Part result = partService.create(part);

        assertNotNull(result);
        assertEquals(part.getId(), result.getId());
        verify(partRepository, times(1)).saveAndFlush(part);
        verify(em, times(1)).refresh(part);
    }

    @Test
    void testUpdate_whenExists() {
        PartPatchDTO patchDTO = new PartPatchDTO();
        when(partRepository.existsById(1L)).thenReturn(true);
        when(partRepository.findById(1L)).thenReturn(Optional.of(part));
        when(partMapper.updatePart(any(Part.class), any(PartPatchDTO.class))).thenReturn(part);
        when(partRepository.saveAndFlush(any(Part.class))).thenReturn(part);

        Part result = partService.update(1L, patchDTO);

        assertNotNull(result);
        assertEquals(part.getId(), result.getId());
        verify(partRepository, times(1)).saveAndFlush(part);
        verify(em, times(1)).refresh(part);
    }

    @Test
    void testUpdate_whenNotExists_shouldThrowException() {
        PartPatchDTO patchDTO = new PartPatchDTO();
        when(partRepository.existsById(1L)).thenReturn(false);

        CustomException exception = assertThrows(CustomException.class, () -> partService.update(1L, patchDTO));

        assertEquals("Not found", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    }

    @Test
    void testConsumePart_positiveQuantity_belowMinQuantity() {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(1L);
        double quantityToConsume = 95.0; // Will make quantity 5, which is below minQuantity 10

        when(partRepository.findById(1L)).thenReturn(Optional.of(part));

        CustomException exception = assertThrows(CustomException.class, () -> partService.consumePart(1L, quantityToConsume, workOrder, Locale.ENGLISH));

        assertEquals("There is not enough of this part", exception.getMessage());
        assertEquals(HttpStatus.NOT_ACCEPTABLE, exception.getHttpStatus());
        verify(notificationService, never()).createMultiple(anyList(), anyBoolean(), anyString());
        verify(partConsumptionService, never()).create(any(PartConsumption.class));
        verify(partRepository, never()).save(any(Part.class));
    }

    @Test
    void testConsumePart_positiveQuantity_aboveMinQuantity() {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(1L);
        double quantityToConsume = 5.0; // Will make quantity 95, which is above minQuantity 10

        when(partRepository.findById(1L)).thenReturn(Optional.of(part));
        when(partConsumptionService.create(any(PartConsumption.class))).thenReturn(new PartConsumption());
        when(partRepository.save(any(Part.class))).thenReturn(part);

        partService.consumePart(1L, quantityToConsume, workOrder, Locale.ENGLISH);

        assertEquals(95.0, part.getQuantity());
        verify(notificationService, never()).createMultiple(anyList(), anyBoolean(), anyString());
        verify(partConsumptionService, times(1)).create(any(PartConsumption.class));
        verify(partRepository, times(1)).save(part);
    }

    @Test
    void testConsumePart_negativeQuantity() {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(1L);
        double quantityToConsume = -5.0; // Returning 5 units

        PartConsumption existingConsumption = new PartConsumption(part, workOrder, 10.0);
        when(partRepository.findById(1L)).thenReturn(Optional.of(part));
        when(partConsumptionService.findByWorkOrderAndPart(anyLong(), anyLong())).thenReturn(Collections.singletonList(existingConsumption));
        doNothing().when(partConsumptionService).save(any(PartConsumption.class));
        when(partRepository.save(any(Part.class))).thenReturn(part);

        partService.consumePart(1L, quantityToConsume, workOrder, Locale.ENGLISH);

        assertEquals(105.0, part.getQuantity());
        assertEquals(5.0, existingConsumption.getQuantity());
        verify(partConsumptionService, times(1)).findByWorkOrderAndPart(anyLong(), anyLong());
        verify(partConsumptionService, times(1)).save(existingConsumption);
        verify(partRepository, times(1)).save(part);
    }

    @Test
    void testConsumePart_notEnoughQuantity_shouldThrowException() {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(1L);
        double quantityToConsume = 150.0; // More than available 100

        when(partRepository.findById(1L)).thenReturn(Optional.of(part));

        CustomException exception = assertThrows(CustomException.class, () -> partService.consumePart(1L, quantityToConsume, workOrder, Locale.ENGLISH));

        assertEquals("The quantity should not be negative", exception.getMessage());
        assertEquals(HttpStatus.NOT_ACCEPTABLE, exception.getHttpStatus());
        verify(partConsumptionService, never()).create(any(PartConsumption.class));
        verify(partRepository, never()).save(any(Part.class));
    }

    @Test
    void testGetAll() {
        when(partRepository.findAll()).thenReturn(Collections.singletonList(part));

        Collection<Part> result = partService.getAll();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(partRepository, times(1)).findAll();
    }

    @Test
    void testDelete() {
        doNothing().when(partRepository).deleteById(1L);

        partService.delete(1L);

        verify(partRepository, times(1)).deleteById(1L);
    }

    @Test
    void testFindById() {
        when(partRepository.findById(1L)).thenReturn(Optional.of(part));

        Optional<Part> result = partService.findById(1L);

        assertTrue(result.isPresent());
        assertEquals(part.getId(), result.get().getId());
        verify(partRepository, times(1)).findById(1L);
    }

    @Test
    void testFindByCompany() {
        when(partRepository.findByCompany_Id(1L)).thenReturn(Collections.singletonList(part));

        Collection<Part> result = partService.findByCompany(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(partRepository, times(1)).findByCompany_Id(1L);
    }

    @Test
    void testNotify_withUsers() {
        when(messageSource.getMessage(anyString(), any(), any(Locale.class))).thenReturn("Part Assigned");
        doNothing().when(notificationService).createMultiple(anyList(), anyBoolean(), anyString());

        partService.notify(part, Locale.ENGLISH);

        verify(notificationService, times(1)).createMultiple(anyList(), eq(true), eq("Part Assigned"));
    }

    @Test
    void testNotify_withNoUsers() {
        Part mockedPart = mock(Part.class);
        when(mockedPart.getUsers()).thenReturn(new ArrayList<>());
        partService.notify(mockedPart, Locale.ENGLISH);
        verify(notificationService, never()).createMultiple(anyList(), anyBoolean(), anyString());
    }

    @Test
    void testPatchNotify_withNewUsers() {
        Part oldPart = new Part();
        oldPart.setAssignedTo(new ArrayList<>());
        List<OwnUser> newUsers = Collections.singletonList(user);
        part.setAssignedTo(newUsers);

        when(messageSource.getMessage(anyString(), any(), any(Locale.class))).thenReturn("Part Assigned");
        doNothing().when(notificationService).createMultiple(anyList(), anyBoolean(), anyString());

        partService.patchNotify(oldPart, part, Locale.ENGLISH);

        verify(notificationService, times(1)).createMultiple(anyList(), eq(true), eq("Part Assigned"));
    }

    @Test
    void testPatchNotify_withNoNewUsers() {
        Part oldPart = new Part();
        part.setAssignedTo(Collections.singletonList(user));
        oldPart.setAssignedTo(Collections.singletonList(user));

        partService.patchNotify(oldPart, part, Locale.ENGLISH);

        verify(notificationService, never()).createMultiple(anyList(), anyBoolean(), anyString());
    }

    @Test
    void testSave() {
        when(partRepository.save(any(Part.class))).thenReturn(part);

        Part result = partService.save(part);

        assertNotNull(result);
        assertEquals(part.getId(), result.getId());
        verify(partRepository, times(1)).save(part);
    }

    @Test
    void testIsPartInCompany_optionalTrue_partNull() {
        assertTrue(partService.isPartInCompany(null, 1L, true));
    }

    @Test
    void testIsPartInCompany_optionalTrue_partFoundAndMatches() {
        when(partRepository.findById(anyLong())).thenReturn(Optional.of(part));
        assertTrue(partService.isPartInCompany(part, 1L, true));
    }

    @Test
    void testIsPartInCompany_optionalTrue_partFoundButDifferentCompany() {
        Company otherCompany = new Company();
        otherCompany.setId(2L);
        part.setCompany(otherCompany);
        when(partRepository.findById(anyLong())).thenReturn(Optional.of(part));
        assertFalse(partService.isPartInCompany(part, 1L, true));
    }

    @Test
    void testIsPartInCompany_optionalFalse_partFoundAndMatches() {
        when(partRepository.findById(anyLong())).thenReturn(Optional.of(part));
        assertTrue(partService.isPartInCompany(part, 1L, false));
    }

    @Test
    void testIsPartInCompany_optionalFalse_partFoundButDifferentCompany() {
        Company otherCompany = new Company();
        otherCompany.setId(2L);
        part.setCompany(otherCompany);
        when(partRepository.findById(anyLong())).thenReturn(Optional.of(part));
        assertFalse(partService.isPartInCompany(part, 1L, false));
    }

    @Test
    void testIsPartInCompany_optionalFalse_partNotFound() {
        when(partRepository.findById(anyLong())).thenReturn(Optional.empty());
        assertFalse(partService.isPartInCompany(part, 1L, false));
    }

    @Test
    void testFindBySearchCriteria() {
        SearchCriteria searchCriteria = new SearchCriteria();
        searchCriteria.setPageNum(0);
        searchCriteria.setPageSize(10);
        searchCriteria.setSortField("name");
        searchCriteria.setDirection(Sort.Direction.ASC);
        searchCriteria.setFilterFields(Collections.emptyList());

        Pageable pageable = PageRequest.of(0, 10, Sort.Direction.ASC, "name");
        Page<Part> partPage = new PageImpl<>(Collections.singletonList(part), pageable, 1);
        PartShowDTO partShowDTO = new PartShowDTO();
        partShowDTO.setId(part.getId());
        partShowDTO.setName(part.getName());

        when(partRepository.findAll(eq((Specification<Part>) null), any(Pageable.class))).thenReturn(partPage);
        when(partMapper.toShowDto(any(Part.class))).thenReturn(partShowDTO);

        Page<PartShowDTO> result = partService.findBySearchCriteria(searchCriteria);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(partShowDTO.getName(), result.getContent().get(0).getName());
        verify(partRepository, times(1)).findAll(eq((Specification<Part>) null), eq(pageable));
        verify(partMapper, times(1)).toShowDto(any(Part.class));
    }

    @Test
    void testImportPart_success() {
        PartImportDTO dto = new PartImportDTO();
        dto.setName("Imported Part");
        dto.setCost(10.0);
        dto.setCategory("Test Category");
        dto.setNonStock("false");
        dto.setBarcode("BARCODE123");
        dto.setDescription("Description");
        dto.setQuantity(50.0);
        dto.setAdditionalInfos("Info");
        dto.setArea("Area 1");
        dto.setMinQuantity(5.0);
        dto.setAssignedToEmails(Collections.singletonList("test@example.com"));
        dto.setTeamsNames(Collections.singletonList("Team 1"));
        dto.setCustomersNames(Collections.singletonList("Customer 1"));
        dto.setVendorsNames(Collections.singletonList("Vendor 1"));

        PartCategory partCategory = new PartCategory();
        partCategory.setId(1L);

        Location location = new Location();
        location.setId(1L);

        Team team = new Team();
        team.setId(1L);

        Customer customer = new Customer();
        customer.setId(1L);

        Vendor vendor = new Vendor();
        vendor.setId(1L);

        try (MockedStatic<Helper> mockedHelper = Mockito.mockStatic(Helper.class)) {
            mockedHelper.when(() -> Helper.getBooleanFromString(anyString())).thenReturn(false);
            mockedHelper.when(() -> Helper.getLocale(any(Company.class))).thenReturn(Locale.ENGLISH);

            lenient().when(partCategoryService.findByNameIgnoreCaseAndCompanySettings(anyString(), anyLong())).thenReturn(Optional.of(partCategory));
            when(userService.findByEmailAndCompany(anyString(), anyLong())).thenReturn(Optional.of(user));
            when(teamService.findByNameIgnoreCaseAndCompany(anyString(), anyLong())).thenReturn(Optional.of(team));
            when(customerService.findByNameIgnoreCaseAndCompany(anyString(), anyLong())).thenReturn(Optional.of(customer));
            when(vendorService.findByNameIgnoreCaseAndCompany(anyString(), anyLong())).thenReturn(Optional.of(vendor));
            when(partRepository.findByBarcodeAndCompany_Id(anyString(), anyLong())).thenReturn(Optional.empty());
            when(partRepository.save(any(Part.class))).thenReturn(part);

            partService.importPart(part, dto, company);

            verify(partRepository, times(1)).save(part);
            assertEquals("Imported Part", part.getName());
            assertEquals(partCategory, part.getCategory());
            assertFalse(part.isNonStock());
            assertEquals("BARCODE123", part.getBarcode());
            assertEquals(50.0, part.getQuantity());
            assertEquals(user, part.getAssignedTo().get(0));
            assertEquals(team, part.getTeams().get(0));
            assertEquals(customer, part.getCustomers().get(0));
            assertEquals(vendor, part.getVendors().get(0));
        }
    }

    @Test
    void testImportPart_barcodeConflict_creation() {
        PartImportDTO dto = new PartImportDTO();
        dto.setName("Imported Part");
        dto.setBarcode("BARCODE123");

        Part existingPartWithSameBarcode = new Part();
        existingPartWithSameBarcode.setId(2L); // Different ID

        when(partRepository.findByBarcodeAndCompany_Id(anyString(), anyLong())).thenReturn(Optional.of(existingPartWithSameBarcode));

        CustomException exception = assertThrows(CustomException.class, () -> partService.importPart(part, dto, company));

        assertEquals("Part with same barcode exists: BARCODE123", exception.getMessage());
        assertEquals(HttpStatus.NOT_ACCEPTABLE, exception.getHttpStatus());
        verify(partRepository, never()).save(any(Part.class));
    }

    @Test
    void testImportPart_barcodeConflict_update() {
        PartImportDTO dto = new PartImportDTO();
        dto.setId(1L); // Same ID as 'part'
        dto.setName("Imported Part");
        dto.setBarcode("BARCODE123");

        Part existingPartWithSameBarcode = new Part();
        existingPartWithSameBarcode.setId(2L); // Different ID

        when(partRepository.findByBarcodeAndCompany_Id(anyString(), anyLong())).thenReturn(Optional.of(existingPartWithSameBarcode));

        CustomException exception = assertThrows(CustomException.class, () -> partService.importPart(part, dto, company));

        assertEquals("Part with same barcode exists: BARCODE123", exception.getMessage());
        assertEquals(HttpStatus.NOT_ACCEPTABLE, exception.getHttpStatus());
        verify(partRepository, never()).save(any(Part.class));
    }

    @Test
    void testImportPart_barcodeNoConflict_updateSameId() {
        PartImportDTO dto = new PartImportDTO();
        dto.setId(1L); // Same ID as 'part'
        dto.setName("Imported Part");
        dto.setBarcode("BARCODE123");

        when(partRepository.findByBarcodeAndCompany_Id(anyString(), anyLong())).thenReturn(Optional.of(part)); // Returns 'part' itself
        when(partRepository.save(any(Part.class))).thenReturn(part);

        try (MockedStatic<Helper> mockedHelper = Mockito.mockStatic(Helper.class)) {
            mockedHelper.when(() -> Helper.getBooleanFromString(anyString())).thenReturn(false);
            mockedHelper.when(() -> Helper.getLocale(any(Company.class))).thenReturn(Locale.ENGLISH);

            partService.importPart(part, dto, company);

            verify(partRepository, times(1)).save(part);
            assertEquals("Imported Part", part.getName());
        }
    }

    @Test
    void testFindByIdAndCompany() {
        when(partRepository.findByIdAndCompany_Id(1L, 1L)).thenReturn(Optional.of(part));

        Optional<Part> result = partService.findByIdAndCompany(1L, 1L);

        assertTrue(result.isPresent());
        assertEquals(part.getId(), result.get().getId());
        verify(partRepository, times(1)).findByIdAndCompany_Id(1L, 1L);
    }

    @Test
    void testFindByNameIgnoreCaseAndCompany() {
        when(partRepository.findByNameIgnoreCaseAndCompany_Id("Test Part", 1L)).thenReturn(Optional.of(part));

        Optional<Part> result = partService.findByNameIgnoreCaseAndCompany("Test Part", 1L);

        assertTrue(result.isPresent());
        assertEquals(part.getId(), result.get().getId());
        verify(partRepository, times(1)).findByNameIgnoreCaseAndCompany_Id("Test Part", 1L);
    }

    @Test
    void testFindByBarcodeAndCompany() {
        when(partRepository.findByBarcodeAndCompany_Id("BARCODE123", 1L)).thenReturn(Optional.of(part));

        Optional<Part> result = partService.findByBarcodeAndCompany("BARCODE123", 1L);

        assertTrue(result.isPresent());
        assertEquals(part.getId(), result.get().getId());
        verify(partRepository, times(1)).findByBarcodeAndCompany_Id("BARCODE123", 1L);
    }
}
