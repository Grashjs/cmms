package com.grash.controller;

import com.grash.advancedsearch.FilterField;
import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.MeterMiniDTO;
import com.grash.dto.MeterPatchDTO;
import com.grash.dto.MeterShowDTO;
import com.grash.dto.SuccessResponse;
import com.grash.exception.CustomException;
import com.grash.mapper.MeterMapper;
import com.grash.model.*;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.PlanFeatures;
import com.grash.model.enums.RoleType;
import com.grash.service.AssetService;
import com.grash.service.MeterService;
import com.grash.service.ReadingService;
import com.grash.service.UserService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeterControllerTest {

    @Mock
    private MeterService meterService;
    @Mock
    private MeterMapper meterMapper;
    @Mock
    private UserService userService;
    @Mock
    private AssetService assetService;
    @Mock
    private ReadingService readingService;
    @Mock
    private EntityManager em;
    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private MeterController meterController;

    private OwnUser user;
    private Meter meter;
    private MeterShowDTO meterShowDTO;
    private Company company;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);

        SubscriptionPlan subscriptionPlan = new SubscriptionPlan();
        subscriptionPlan.setFeatures(new HashSet<>(Collections.singletonList(PlanFeatures.METER)));

        Subscription subscription = new Subscription();
        subscription.setSubscriptionPlan(subscriptionPlan);
        company.setSubscription(subscription);

        Role role = new Role();
        role.setRoleType(RoleType.ROLE_CLIENT);
        role.setViewPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.METERS)));
        role.setCreatePermissions(new HashSet<>(Collections.singletonList(PermissionEntity.METERS)));
        role.setEditOtherPermissions(new HashSet<>());
        role.setDeleteOtherPermissions(new HashSet<>());
        role.setViewOtherPermissions(new HashSet<>());

        user = new OwnUser();
        user.setId(1L);
        user.setCompany(company);
        user.setRole(role);

        meter = new Meter();
        meter.setId(1L);
        meter.setCreatedBy(1L);
        meter.setUsers(new ArrayList<>());

        meterShowDTO = new MeterShowDTO();
        meterShowDTO.setId(1L);
    }

    @Nested
    @DisplayName("search method")
    class SearchTests {
        @Test
        @DisplayName("Should return a page of meters for client with view permission")
        void search_withViewPermission_shouldSucceed() {
            SearchCriteria searchCriteria = new SearchCriteria();
            searchCriteria.setFilterFields(new ArrayList<>());
            Page<MeterShowDTO> page = new PageImpl<>(Collections.singletonList(meterShowDTO));

            when(userService.whoami(request)).thenReturn(user);
            when(meterService.findBySearchCriteria(any(SearchCriteria.class))).thenReturn(page);

            ResponseEntity<Page<MeterShowDTO>> response = meterController.search(searchCriteria, request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1, response.getBody().getTotalElements());
        }
    }

    @Nested
    @DisplayName("getMini method")
    class GetMiniTests {
        @Test
        @DisplayName("Should return a collection of mini meters")
        void getMini_shouldSucceed() {
            when(userService.whoami(request)).thenReturn(user);
            when(meterService.findByCompany(company.getId())).thenReturn(Collections.singletonList(meter));
            when(meterMapper.toMiniDto(meter)).thenReturn(new MeterMiniDTO());

            Collection<MeterMiniDTO> result = meterController.getMini(request);

            assertFalse(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("getById method")
    class GetByIdTests {
        @Test
        @DisplayName("Should return meter when found and user has permission")
        void getById_foundAndPermitted_shouldSucceed() {
            when(userService.whoami(request)).thenReturn(user);
            when(meterService.findById(1L)).thenReturn(Optional.of(meter));
            when(meterMapper.toShowDto(meter, readingService)).thenReturn(meterShowDTO);

            MeterShowDTO result = meterController.getById(1L, request);

            assertNotNull(result);
            assertEquals(meter.getId(), result.getId());
        }

        @Test
        @DisplayName("Should throw exception when meter not found")
        void getById_notFound_shouldThrowException() {
            when(userService.whoami(request)).thenReturn(user);
            when(meterService.findById(1L)).thenReturn(Optional.empty());

            assertThrows(CustomException.class, () -> meterController.getById(1L, request));
        }
    }

    @Nested
    @DisplayName("create method")
    class CreateTests {
        @Test
        @DisplayName("Should create meter when user has permission")
        void create_withPermission_shouldSucceed() {
            when(userService.whoami(request)).thenReturn(user);
            when(meterService.create(any(Meter.class))).thenReturn(meter);
            when(meterMapper.toShowDto(meter, readingService)).thenReturn(meterShowDTO);

            MeterShowDTO result = meterController.create(new Meter(), request);

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should throw exception when user has no permission")
        void create_withoutPermission_shouldThrowException() {
            user.getRole().setCreatePermissions(new HashSet<>());
            when(userService.whoami(request)).thenReturn(user);

            assertThrows(CustomException.class, () -> meterController.create(new Meter(), request));
        }
    }

    @Nested
    @DisplayName("patch method")
    class PatchTests {
        @Test
        @DisplayName("Should patch meter when user has permission")
        void patch_withPermission_shouldSucceed() {
            when(userService.whoami(request)).thenReturn(user);
            when(meterService.findById(1L)).thenReturn(Optional.of(meter));
            when(meterService.update(any(Long.class), any(MeterPatchDTO.class))).thenReturn(meter);
            when(meterMapper.toShowDto(meter, readingService)).thenReturn(meterShowDTO);

            MeterShowDTO result = meterController.patch(new MeterPatchDTO(), 1L, request);

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should throw exception when meter not found")
        void patch_notFound_shouldThrowException() {
            when(userService.whoami(request)).thenReturn(user);
            when(meterService.findById(1L)).thenReturn(Optional.empty());

            assertThrows(CustomException.class, () -> meterController.patch(new MeterPatchDTO(), 1L, request));
        }
    }

    @Nested
    @DisplayName("getByAsset method")
    class GetByAssetTests {
        @Test
        @DisplayName("Should return meters when asset found")
        void getByAsset_found_shouldSucceed() {
            Asset asset = new Asset();
            asset.setId(1L);
            when(userService.whoami(request)).thenReturn(user);
            when(assetService.findById(1L)).thenReturn(Optional.of(asset));
            when(meterService.findByAsset(1L)).thenReturn(Collections.singletonList(meter));
            when(meterMapper.toShowDto(meter, readingService)).thenReturn(meterShowDTO);

            Collection<MeterShowDTO> result = meterController.getByAsset(1L, request);

            assertFalse(result.isEmpty());
        }

        @Test
        @DisplayName("Should throw exception when asset not found")
        void getByAsset_notFound_shouldThrowException() {
            when(userService.whoami(request)).thenReturn(user);
            when(assetService.findById(1L)).thenReturn(Optional.empty());

            assertThrows(CustomException.class, () -> meterController.getByAsset(1L, request));
        }
    }

    @Nested
    @DisplayName("delete method")
    class DeleteTests {
        @Test
        @DisplayName("Should delete meter when user has permission")
        void delete_withPermission_shouldSucceed() {
            when(userService.whoami(request)).thenReturn(user);
            when(meterService.findById(1L)).thenReturn(Optional.of(meter));

            ResponseEntity<SuccessResponse> response = meterController.delete(1L, request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(response.getBody().isSuccess());
        }

        @Test
        @DisplayName("Should throw exception when meter not found")
        void delete_notFound_shouldThrowException() {
            when(userService.whoami(request)).thenReturn(user);
            when(meterService.findById(1L)).thenReturn(Optional.empty());

            assertThrows(CustomException.class, () -> meterController.delete(1L, request));
        }
    }
}
