
package com.grash.service;

import com.grash.dto.MeterPatchDTO;
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

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
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
}
