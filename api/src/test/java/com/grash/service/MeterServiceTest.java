
package com.grash.service;

import java.util.Collections;
import com.grash.model.CompanySettings;
import com.grash.dto.imports.MeterImportDTO;
import com.grash.model.Location;
import com.grash.model.MeterCategory;
import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.MeterShowDTO;
import com.grash.dto.MeterPatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.MeterMapper;
import com.grash.model.Company;
import com.grash.model.Meter;
import com.grash.model.OwnUser;
import com.grash.repository.MeterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeterServiceTest {

    @Mock
    private MeterRepository meterRepository;

    @Mock
    private MeterCategoryService meterCategoryService;

    @Mock
    private FileService fileService;

    @Mock
    private AssetService assetService;

    @Mock
    private CompanyService companyService;

    @Mock
    private MessageSource messageSource;

    @Mock
    private LocationService locationService;

    @Mock
    private UserService userService;

    @Mock
    private EntityManager em;

    @Mock
    private MeterMapper meterMapper;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ReadingService readingService;

    @InjectMocks
    private MeterService meterService;

    private Meter meter;
    private Company company;
    private OwnUser user;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);

        user = new OwnUser();
        user.setId(1L);
        user.setCompany(company);

        meter = new Meter();
        meter.setId(1L);
        meter.setName("Test Meter");
        meter.setCompany(company);
        List<OwnUser> users = new ArrayList<>();
        users.add(user);
        meter.setUsers(users);
    }

    @Test
    void testCreateMeter() {
        when(meterRepository.saveAndFlush(any(Meter.class))).thenReturn(meter);
        Meter createdMeter = meterService.create(meter);
        assertNotNull(createdMeter);
        assertEquals("Test Meter", createdMeter.getName());
        verify(meterRepository, times(1)).saveAndFlush(meter);
        verify(em, times(1)).refresh(any(Meter.class));
    }

    @Test
    void testUpdateMeter() {
        MeterPatchDTO patchDTO = new MeterPatchDTO();
        patchDTO.setName("Updated Meter");

        when(meterRepository.existsById(1L)).thenReturn(true);
        when(meterRepository.findById(1L)).thenReturn(Optional.of(meter));
        when(meterMapper.updateMeter(any(Meter.class), any(MeterPatchDTO.class))).thenReturn(meter);
        when(meterRepository.saveAndFlush(any(Meter.class))).thenReturn(meter);

        Meter updatedMeter = meterService.update(1L, patchDTO);

        assertNotNull(updatedMeter);
        verify(em, times(1)).refresh(any(Meter.class));
    }

    @Test
    void testUpdateMeter_whenNotExists_shouldThrowException() {
        MeterPatchDTO patchDTO = new MeterPatchDTO();
        patchDTO.setName("Updated Meter");

        when(meterRepository.existsById(1L)).thenReturn(false);

        assertThrows(CustomException.class, () -> meterService.update(1L, patchDTO));
    }

    @Test
    void testNotify_withUsers() {
        when(messageSource.getMessage(anyString(), any(), any(Locale.class))).thenReturn("message");
        meterService.notify(meter, Locale.ENGLISH);
        verify(notificationService, times(1)).createMultiple(anyList(), anyBoolean(), anyString());
    }

    @Test
    void testNotify_withNoUsers() {
        meter.setUsers(new ArrayList<>());
        meterService.notify(meter, Locale.ENGLISH);
        verify(notificationService, never()).createMultiple(anyList(), anyBoolean(), anyString());
    }

    @Test
    void testPatchNotify_withNewUsers() {
        Meter oldMeter = new Meter();
        oldMeter.setUsers(new ArrayList<>());

        when(messageSource.getMessage(anyString(), any(), any(Locale.class))).thenReturn("message");

        meterService.patchNotify(oldMeter, meter, Locale.ENGLISH);
        verify(notificationService, times(1)).createMultiple(anyList(), anyBoolean(), anyString());
    }

    @Test
    void testPatchNotify_withNoNewUsers() {
        Meter oldMeter = new Meter();
        oldMeter.setUsers(meter.getUsers());

        meterService.patchNotify(oldMeter, meter, Locale.ENGLISH);
        verify(notificationService, never()).createMultiple(anyList(), anyBoolean(), anyString());
    }

    @Test
    void testGetAllMeters() {
        meterService.getAll();
        verify(meterRepository, times(1)).findAll();
    }

    @Test
    void testDeleteMeter() {
        meterService.delete(1L);
        verify(meterRepository, times(1)).deleteById(1L);
    }

    @Test
    void testFindMeterById() {
        when(meterRepository.findById(1L)).thenReturn(Optional.of(meter));
        Optional<Meter> foundMeter = meterService.findById(1L);
        assertTrue(foundMeter.isPresent());
        assertEquals("Test Meter", foundMeter.get().getName());
        verify(meterRepository, times(1)).findById(1L);
    }

    @Test
    void testFindByCompany() {
        meterService.findByCompany(1L);
        verify(meterRepository, times(1)).findByCompany_Id(1L);
    }

    @Test
    void testFindByAsset() {
        meterService.findByAsset(1L);
        verify(meterRepository, times(1)).findByAsset_Id(1L);
    }

    @Test
    void testIsMeterInCompany() {
        when(meterRepository.findById(1L)).thenReturn(Optional.of(meter));
        assertTrue(meterService.isMeterInCompany(meter, 1L, false));
    }

    @Test
    void isMeterInCompany_withOptionalTrueAndRequestNull() {
        assertTrue(meterService.isMeterInCompany(null, 1L, true));
    }

    @Test
    void isMeterInCompany_withOptionalTrueAndRequestInCompany() {
        when(meterRepository.findById(1L)).thenReturn(Optional.of(meter));
        assertTrue(meterService.isMeterInCompany(meter, 1L, true));
    }

    @Test
    void isMeterInCompany_withOptionalTrueAndRequestNotInCompany() {
        when(meterRepository.findById(1L)).thenReturn(Optional.of(meter));
        assertFalse(meterService.isMeterInCompany(meter, 2L, true));
    }

    @Test
    void isMeterInCompany_withOptionalFalseAndRequestNotInCompany() {
        when(meterRepository.findById(1L)).thenReturn(Optional.of(meter));
        assertFalse(meterService.isMeterInCompany(meter, 2L, false));
    }

    @Test
    void isMeterInCompany_withOptionalFalseAndRequestNotFound() {
        when(meterRepository.findById(1L)).thenReturn(Optional.empty());
        assertFalse(meterService.isMeterInCompany(meter, 1L, false));
    }



    @Test
    void testImportMeter() {
        MeterImportDTO dto = new MeterImportDTO();
        dto.setName("Test Meter");
        dto.setUnit("Test Unit");
        dto.setUpdateFrequency(1);
        dto.setLocationName("Test Location");
        dto.setAssetName("Test Asset");
        dto.setMeterCategory("Test Category");
        dto.setUsersEmails(Collections.singletonList("test@test.com"));

        Company company = new Company();
        company.setId(1L);
        CompanySettings companySettings = new CompanySettings();
        companySettings.setId(1L);
        company.setCompanySettings(companySettings);

        when(locationService.findByNameIgnoreCaseAndCompany(anyString(), anyLong())).thenReturn(Collections.singletonList(new Location()));
        when(assetService.findByNameIgnoreCaseAndCompany(anyString(), anyLong())).thenReturn(Collections.singletonList(new com.grash.model.Asset()));
        when(meterCategoryService.findByNameIgnoreCaseAndCompanySettings(anyString(), anyLong())).thenReturn(Optional.of(new MeterCategory()));
        when(userService.findByEmailAndCompany(anyString(), anyLong())).thenReturn(Optional.of(new OwnUser()));

        meterService.importMeter(meter, dto, company);

        verify(meterRepository, times(1)).save(meter);
    }

    @Test
    void testFindByIdAndCompany() {
        when(meterRepository.findByIdAndCompany_Id(1L, 1L)).thenReturn(Optional.of(meter));
        Optional<Meter> foundMeter = meterService.findByIdAndCompany(1L, 1L);
        assertTrue(foundMeter.isPresent());
        assertEquals("Test Meter", foundMeter.get().getName());
        verify(meterRepository, times(1)).findByIdAndCompany_Id(1L, 1L);
    }
}
