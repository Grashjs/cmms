package com.grash.controller;

import com.grash.dto.webhookEndpoint.WebhookDeliveryLogShowDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.WebhookDeliveryLogMapper;
import com.grash.mapper.WebhookEndpointMapper;
import com.grash.model.Company;
import com.grash.model.Role;
import com.grash.model.User;
import com.grash.model.WebhookDeliveryLog;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.RoleType;
import com.grash.service.UserService;
import com.grash.service.WebhookDeliveryLogService;
import com.grash.service.WebhookEndpointService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.grash.utils.Helper.setCurrentUser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WebhookEndpointController.class)
@AutoConfigureMockMvc(addFilters = false)
class WebhookEndpointControllerTest extends AbstractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WebhookEndpointService webhookEndpointService;
    @MockitoBean
    private WebhookEndpointMapper webhookEndpointMapper;
    @MockitoBean
    private WebhookDeliveryLogService webhookDeliveryLogService;
    @MockitoBean
    private WebhookDeliveryLogMapper webhookDeliveryLogMapper;
    @MockitoBean
    private UserService userService;

    private User clientUser;
    private User nonClientUser;

    @BeforeEach
    void setUp() {
        Company company = new Company();
        company.setId(1L);

        clientUser = new User();
        clientUser.setId(1L);
        clientUser.setEmail("john@test.com");
        clientUser.setEnabled(true);
        clientUser.setCompany(company);
        clientUser.setRole(Role.builder().id(1L).roleType(RoleType.ROLE_CLIENT).name("Client")
                .viewPermissions(new HashSet<>(Set.of(PermissionEntity.SETTINGS))).build());

        nonClientUser = new User();
        nonClientUser.setId(2L);
        nonClientUser.setEmail("admin@test.com");
        nonClientUser.setEnabled(true);
        nonClientUser.setRole(Role.builder().id(2L).roleType(RoleType.ROLE_SUPER_ADMIN).name("Super")
                .viewPermissions(new HashSet<>(Set.of(PermissionEntity.SETTINGS))).build());
    }

    private WebhookDeliveryLogShowDTO dto(Long id) {
        WebhookDeliveryLogShowDTO dto = new WebhookDeliveryLogShowDTO();
        dto.setId(id);
        dto.setWebhookEndpointId(5L);
        dto.setEventType("NEW_WORK_ORDER");
        dto.setResponseStatus(200);
        dto.setSuccess(true);
        return dto;
    }

    @Test
    void getLogs_returnsPageAndPassesPageable() throws Exception {
        setCurrentUser(clientUser);
        when(userService.whoami(any())).thenReturn(clientUser);
        WebhookDeliveryLog entity = new WebhookDeliveryLog();
        when(webhookDeliveryLogService.findByEndpoint(eq(5L), any(Pageable.class), eq(clientUser)))
                .thenReturn(new PageImpl<>(List.of(entity), PageRequest.of(0, 20), 1));
        when(webhookDeliveryLogMapper.toShowDto(entity)).thenReturn(dto(9L));

        mockMvc.perform(get("/webhook-endpoints/5/logs?page=0&size=20&sort=createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(9))
                .andExpect(jsonPath("$.content[0].webhookEndpointId").value(5))
                .andExpect(jsonPath("$.totalElements").value(1));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(webhookDeliveryLogService).findByEndpoint(eq(5L), captor.capture(), eq(clientUser));
        assertEquals(20, captor.getValue().getPageSize());
        assertEquals("DESC", captor.getValue().getSort().getOrderFor("createdAt").getDirection().name());
    }

    @Test
    void getLogs_requiresRoleClient() throws Exception {
        setCurrentUser(nonClientUser);
        when(userService.whoami(any())).thenReturn(nonClientUser);

        mockMvc.perform(get("/webhook-endpoints/5/logs")).andExpect(status().isForbidden());
    }

    @Test
    void getLogs_serviceDenies_returns403() throws Exception {
        setCurrentUser(clientUser);
        when(userService.whoami(any())).thenReturn(clientUser);
        when(webhookDeliveryLogService.findByEndpoint(eq(5L), any(Pageable.class), eq(clientUser)))
                .thenThrow(new CustomException("Access denied", HttpStatus.FORBIDDEN));

        mockMvc.perform(get("/webhook-endpoints/5/logs")).andExpect(status().isForbidden());
    }

    @Test
    void getLogs_endpointOfAnotherCompany_returns404() throws Exception {
        setCurrentUser(clientUser);
        when(userService.whoami(any())).thenReturn(clientUser);
        when(webhookDeliveryLogService.findByEndpoint(eq(99L), any(Pageable.class), eq(clientUser)))
                .thenThrow(new CustomException("Webhook endpoint not found", HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/webhook-endpoints/99/logs")).andExpect(status().isNotFound());
    }

    @Test
    void retry_returnsNewLog() throws Exception {
        setCurrentUser(clientUser);
        when(userService.whoami(any())).thenReturn(clientUser);
        WebhookDeliveryLog entity = new WebhookDeliveryLog();
        when(webhookDeliveryLogService.retry(3L, clientUser)).thenReturn(entity);
        when(webhookDeliveryLogMapper.toShowDto(entity)).thenReturn(dto(10L));

        mockMvc.perform(post("/webhook-endpoints/logs/3/retry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void retry_requiresRoleClient() throws Exception {
        setCurrentUser(nonClientUser);
        when(userService.whoami(any())).thenReturn(nonClientUser);

        mockMvc.perform(post("/webhook-endpoints/logs/3/retry")).andExpect(status().isForbidden());
    }

    @Test
    void retry_logOfAnotherCompany_returns404() throws Exception {
        setCurrentUser(clientUser);
        when(userService.whoami(any())).thenReturn(clientUser);
        when(webhookDeliveryLogService.retry(77L, clientUser))
                .thenThrow(new CustomException("Webhook delivery log not found", HttpStatus.NOT_FOUND));

        mockMvc.perform(post("/webhook-endpoints/logs/77/retry")).andExpect(status().isNotFound());
    }
}
