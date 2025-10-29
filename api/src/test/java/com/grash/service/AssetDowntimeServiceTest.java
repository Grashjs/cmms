package com.grash.service;

import com.grash.dto.AssetDowntimePatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.AssetDowntimeMapper;
import com.grash.model.Asset;
import com.grash.model.AssetDowntime;
import com.grash.model.Company;
import com.grash.repository.AssetDowntimeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssetDowntimeServiceTest {

    @Mock
    private AssetDowntimeRepository assetDowntimeRepository;

    @Mock
    private AssetDowntimeMapper assetDowntimeMapper;

    @InjectMocks
    private AssetDowntimeService assetDowntimeService;

    private AssetDowntime assetDowntime;
    private Asset asset;

    @BeforeEach
    void setUp() {
        Company company = new Company();
        company.setId(1L);

        asset = new Asset();
        asset.setId(1L);
        asset.setCompany(company);

        assetDowntime = new AssetDowntime();
        assetDowntime.setId(1L);
        assetDowntime.setAsset(asset);
        assetDowntime.setStartsOn(new Date(System.currentTimeMillis() - 100000));
        assetDowntime.setDuration(100000);
    }

    @Test
    void create_whenNoOverlapping() {
        when(assetDowntimeRepository.findByAsset_Id(1L)).thenReturn(Collections.emptyList());
        when(assetDowntimeRepository.save(any(AssetDowntime.class))).thenReturn(assetDowntime);

        AssetDowntime result = assetDowntimeService.create(assetDowntime);

        assertNotNull(result);
        assertEquals(assetDowntime.getId(), result.getId());
        verify(assetDowntimeRepository).save(assetDowntime);
    }

    @Test
    void create_whenOverlapping_shouldThrowException() {
        AssetDowntime existingDowntime = new AssetDowntime();
        existingDowntime.setStartsOn(new Date(System.currentTimeMillis() - 50000));
        existingDowntime.setDuration(100000);

        when(assetDowntimeRepository.findByAsset_Id(1L)).thenReturn(Collections.singletonList(existingDowntime));

        CustomException exception = assertThrows(CustomException.class, () -> assetDowntimeService.create(assetDowntime));

        assertEquals("The downtimes can't interweave", exception.getMessage());
        assertEquals(HttpStatus.NOT_ACCEPTABLE, exception.getHttpStatus());
    }

    @Test
    void save() {
        when(assetDowntimeRepository.save(any(AssetDowntime.class))).thenReturn(assetDowntime);

        AssetDowntime result = assetDowntimeService.save(assetDowntime);

        assertNotNull(result);
        assertEquals(assetDowntime.getId(), result.getId());
        verify(assetDowntimeRepository).save(assetDowntime);
    }

    @Test
    void update_whenExists() {
        AssetDowntimePatchDTO patchDTO = new AssetDowntimePatchDTO();
        when(assetDowntimeRepository.existsById(1L)).thenReturn(true);
        when(assetDowntimeRepository.findById(1L)).thenReturn(Optional.of(assetDowntime));
        when(assetDowntimeMapper.updateAssetDowntime(any(AssetDowntime.class), any(AssetDowntimePatchDTO.class))).thenReturn(assetDowntime);
        when(assetDowntimeRepository.findByAsset_Id(1L)).thenReturn(Collections.emptyList());
        when(assetDowntimeRepository.save(any(AssetDowntime.class))).thenReturn(assetDowntime);

        AssetDowntime result = assetDowntimeService.update(1L, patchDTO);

        assertNotNull(result);
        assertEquals(assetDowntime.getId(), result.getId());
        verify(assetDowntimeRepository).save(assetDowntime);
    }

    @Test
    void update_whenNotExists_shouldThrowException() {
        AssetDowntimePatchDTO patchDTO = new AssetDowntimePatchDTO();
        when(assetDowntimeRepository.existsById(1L)).thenReturn(false);

        CustomException exception = assertThrows(CustomException.class, () -> assetDowntimeService.update(1L, patchDTO));

        assertEquals("Not found", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    }

    @Test
    void getAll() {
        when(assetDowntimeRepository.findAll()).thenReturn(Collections.singletonList(assetDowntime));

        assertEquals(1, assetDowntimeService.getAll().size());
    }

    @Test
    void delete() {
        doNothing().when(assetDowntimeRepository).deleteById(1L);
        assetDowntimeService.delete(1L);
        verify(assetDowntimeRepository).deleteById(1L);
    }

    @Test
    void findById() {
        when(assetDowntimeRepository.findById(1L)).thenReturn(Optional.of(assetDowntime));

        assertTrue(assetDowntimeService.findById(1L).isPresent());
    }

    @Test
    void findByAsset() {
        when(assetDowntimeRepository.findByAsset_Id(1L)).thenReturn(Collections.singletonList(assetDowntime));

        assertEquals(1, assetDowntimeService.findByAsset(1L).size());
    }

    @Test
    void findByAssetAndStartsOnBetween() {
        Date start = new Date(System.currentTimeMillis() - 200000);
        Date end = new Date();
        when(assetDowntimeRepository.findByAsset_IdAndStartsOnBetween(1L, start, end))
                .thenReturn(Collections.singletonList(assetDowntime));

        assertEquals(1, assetDowntimeService.findByAssetAndStartsOnBetween(1L, start, end).size());
    }

    @Test
    void findByCompany() {
        when(assetDowntimeRepository.findByCompany_Id(1L)).thenReturn(Collections.singletonList(assetDowntime));

        assertEquals(1, assetDowntimeService.findByCompany(1L).size());
    }

    @Test
    void findByStartsOnBetweenAndCompany() {
        Date start = new Date(System.currentTimeMillis() - 200000);
        Date end = new Date();
        when(assetDowntimeRepository.findByStartsOnBetweenAndCompany_Id(start, end, 1L))
                .thenReturn(Collections.singletonList(assetDowntime));

        assertEquals(1, assetDowntimeService.findByStartsOnBetweenAndCompany(start, end, 1L).size());
    }

    @Test
    void findByCompanyAndStartsOnBetween() {
        Date start = new Date(System.currentTimeMillis() - 200000);
        Date end = new Date();
        when(assetDowntimeRepository.findByStartsOnBetweenAndCompany_Id(start, end, 1L))
                .thenReturn(Collections.singletonList(assetDowntime));

        assertEquals(1, assetDowntimeService.findByCompanyAndStartsOnBetween(1L, start, end).size());
    }

    @Test
    void getDowntimesMeantime_withLessThan3Downtimes() {
        assertEquals(0, assetDowntimeService.getDowntimesMeantime(Collections.singletonList(assetDowntime)));
    }

    @Test
    void getDowntimesMeantime_withMoreThan2Downtimes() {
        AssetDowntime assetDowntime2 = new AssetDowntime();
        assetDowntime2.setStartsOn(new Date(System.currentTimeMillis() - 200000));
        AssetDowntime assetDowntime3 = new AssetDowntime();
        assetDowntime3.setStartsOn(new Date());

        assertEquals(0, assetDowntimeService.getDowntimesMeantime(java.util.Arrays.asList(assetDowntime, assetDowntime2, assetDowntime3)));
    }

    @Test
    void update_whenOverlapping_shouldThrowException() {
        AssetDowntimePatchDTO patchDTO = new AssetDowntimePatchDTO();
        AssetDowntime existingDowntime = new AssetDowntime();
        existingDowntime.setStartsOn(new Date(System.currentTimeMillis() - 50000));
        existingDowntime.setDuration(100000);

        when(assetDowntimeRepository.existsById(1L)).thenReturn(true);
        when(assetDowntimeRepository.findById(1L)).thenReturn(Optional.of(assetDowntime));
        when(assetDowntimeMapper.updateAssetDowntime(any(AssetDowntime.class), any(AssetDowntimePatchDTO.class))).thenReturn(assetDowntime);
        when(assetDowntimeRepository.findByAsset_Id(1L)).thenReturn(Collections.singletonList(existingDowntime));

        CustomException exception = assertThrows(CustomException.class, () -> assetDowntimeService.update(1L, patchDTO));

        assertEquals("The downtimes can't interweave", exception.getMessage());
        assertEquals(HttpStatus.NOT_ACCEPTABLE, exception.getHttpStatus());
    }
}
