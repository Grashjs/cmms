package com.grash.controller;

import com.grash.exception.CustomException;
import com.grash.factory.MailServiceFactory;
import com.grash.mapper.WorkOrderMapper;
import com.grash.model.*;
import com.grash.model.enums.*;
import com.grash.service.AssetService;
import com.grash.service.LocationService;
import com.grash.service.UserService;
import com.grash.service.WorkOrderService;
import com.grash.util.UserTestUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
class WorkOrderControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @MockBean
    protected UserService userService;

    @MockBean
    protected WorkOrderService workOrderService;

    @MockBean
    protected AssetService assetService;

    @MockBean
    protected LocationService locationService;

    @MockBean
    protected MailServiceFactory mailServiceFactory;

    @MockBean
    protected WorkOrderMapper workOrderMapper;

    @MockBean
    protected MessageSource messageSource;

    private User testUser;
    private WorkOrder testWorkOrder;
    @Autowired
    private UserTestUtils userTestUtils;

    @BeforeEach
    void setUp() {
        testUser = userTestUtils.generateUserAndEnable();
        testWorkOrder = new WorkOrder();
        testWorkOrder.setId(1L);
        testWorkOrder.setTitle("Test Work Order");
        testWorkOrder.setDescription("Test description");
        testWorkOrder.setStatus(Status.OPEN);
        testWorkOrder.setPriority(Priority.NONE);
        testWorkOrder.setCompany(testUser.getCompany());
        testWorkOrder.setCreatedBy(1L);
        testWorkOrder.setFiles(new ArrayList<>());
        testWorkOrder.setAssignedTo(new ArrayList<>());
        testWorkOrder.setCustomers(new ArrayList<>());

        when(userService.whoami(any(HttpServletRequest.class))).thenReturn(testUser);
    }

    @Test
    void getById_shouldReturnWorkOrder() throws Exception {
        when(workOrderService.checkAccessToWorkOrderId(1L, testUser)).thenReturn(testWorkOrder);
        when(workOrderMapper.toShowDto(any(WorkOrder.class))).thenReturn(new com.grash.dto.workOrder.WorkOrderShowDTO());

        mockMvc.perform(get("/work-orders/1")
                        .header("Authorization", "Bearer fake-token"))
                .andExpect(status().isOk());
    }

    @Test
    void getById_shouldReturn404WhenNotFound() throws Exception {
        when(workOrderService.checkAccessToWorkOrderId(999L, testUser))
                .thenThrow(new CustomException("Patient not found", org.springframework.http.HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/work-orders/999")
                        .header("Authorization", "Bearer fake-token"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getByAsset_shouldReturnWorkOrders() throws Exception {
        when(assetService.findById(1L)).thenReturn(Optional.of(new Asset()));
        when(workOrderService.findByAsset(1L)).thenReturn(List.of(testWorkOrder));
        when(workOrderMapper.toShowDto(any(WorkOrder.class))).thenReturn(new com.grash.dto.workOrder.WorkOrderShowDTO());

        mockMvc.perform(get("/work-orders/asset/1")
                        .header("Authorization", "Bearer fake-token"))
                .andExpect(status().isOk());
    }

    @Test
    void getByAsset_shouldReturn404WhenAssetNotFound() throws Exception {
        when(assetService.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/work-orders/asset/999")
                        .header("Authorization", "Bearer fake-token"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getByLocation_shouldReturnWorkOrders() throws Exception {
        when(locationService.findById(1L)).thenReturn(Optional.of(new Location()));
        when(workOrderService.findByLocation(1L)).thenReturn(List.of(testWorkOrder));
        when(workOrderMapper.toShowDto(any(WorkOrder.class))).thenReturn(new com.grash.dto.workOrder.WorkOrderShowDTO());

        mockMvc.perform(get("/work-orders/location/1")
                        .header("Authorization", "Bearer fake-token"))
                .andExpect(status().isOk());
    }

    @Test
    void getByLocation_shouldReturn404WhenLocationNotFound() throws Exception {
        when(locationService.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/work-orders/location/999")
                        .header("Authorization", "Bearer fake-token"))
                .andExpect(status().isNotFound());
    }

    @Test
    void search_shouldReturnResults() throws Exception {
        com.grash.advancedsearch.SearchCriteria criteria = new com.grash.advancedsearch.SearchCriteria();
        when(workOrderService.getSearchCriteria(eq(testUser), any())).thenReturn(criteria);
        when(workOrderService.findBySearchCriteria(any())).thenReturn(
                new org.springframework.data.domain.PageImpl<>(List.of(testWorkOrder)));
        when(workOrderMapper.toShowDto(any(WorkOrder.class))).thenReturn(new com.grash.dto.workOrder.WorkOrderShowDTO());

        mockMvc.perform(post("/work-orders/search")
                        .header("Authorization", "Bearer fake-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"filterFields\":[]}"))
                .andExpect(status().isOk());
    }

    @Test
    void delete_shouldRemoveWorkOrder() throws Exception {
        when(workOrderService.findById(1L)).thenReturn(Optional.of(testWorkOrder));
        doNothing().when(workOrderService).deleteByIdAndUser(anyLong(), any(User.class));
        when(messageSource.getMessage(any(), any(), any())).thenReturn("Work order deleted");
        when(userService.findByCompany(anyLong())).thenReturn(List.of());
        when(mailServiceFactory.getMailService()).thenReturn(mock(com.grash.service.MailService.class));

        mockMvc.perform(delete("/work-orders/1")
                        .header("Authorization", "Bearer fake-token"))
                .andExpect(status().isOk());
    }

    @Test
    void delete_shouldReturn404WhenNotFound() throws Exception {
        when(workOrderService.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/work-orders/999")
                        .header("Authorization", "Bearer fake-token"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUrgentCount_shouldReturnCount() throws Exception {
        when(workOrderService.countUrgent(testUser)).thenReturn(5);

        mockMvc.perform(get("/work-orders/urgent")
                        .header("Authorization", "Bearer fake-token"))
                .andExpect(status().isOk());
    }

    @Test
    void getUrgentCount_shouldDenyAccessForNonClientRoles() throws Exception {
        testUser.getRole().setRoleType(RoleType.ROLE_SUPER_ADMIN);

        mockMvc.perform(get("/work-orders/urgent")
                        .header("Authorization", "Bearer fake-token"))
                .andExpect(status().isForbidden());
    }
}
