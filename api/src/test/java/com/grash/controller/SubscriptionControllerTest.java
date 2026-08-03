package com.grash.controller;

import com.grash.exception.CustomException;
import com.grash.model.Company;
import com.grash.model.Role;
import com.grash.model.Subscription;
import com.grash.model.SubscriptionChangeRequest;
import com.grash.model.SubscriptionPlan;
import com.grash.model.User;
import com.grash.model.enums.RoleType;
import com.grash.service.SubscriptionService;
import com.grash.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collection;

import static com.grash.utils.Helper.setCurrentUser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SubscriptionController.class)
@AutoConfigureMockMvc(addFilters = false)
class SubscriptionControllerTest extends AbstractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SubscriptionService subscriptionService;

    @MockBean
    private UserService userService;

    private Company company;
    private User clientUser;
    private User nonClientUser;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);

        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setId(1L);
        Subscription subscription = new Subscription();
        subscription.setId(1L);
        subscription.setSubscriptionPlan(plan);
        company.setSubscription(subscription);

        Role clientRole = Role.builder()
                .id(1L)
                .roleType(RoleType.ROLE_CLIENT)
                .name("Client Role")
                .build();

        clientUser = new User();
        clientUser.setId(1L);
        clientUser.setFirstName("John");
        clientUser.setLastName("Doe");
        clientUser.setEmail("john@test.com");
        clientUser.setRole(clientRole);
        clientUser.setEnabled(true);
        clientUser.setCompany(company);
        clientUser.setOwnsCompany(true);

        Role nonClientRole = Role.builder()
                .id(2L)
                .roleType(RoleType.ROLE_SUPER_ADMIN)
                .name("Super Admin")
                .build();

        nonClientUser = new User();
        nonClientUser.setId(2L);
        nonClientUser.setFirstName("Admin");
        nonClientUser.setLastName("User");
        nonClientUser.setEmail("admin@test.com");
        nonClientUser.setRole(nonClientRole);
        nonClientUser.setEnabled(true);
    }

    @Nested
    class AuthorizationTests {

        @Test
        void upgrade_requiresRoleClient() throws Exception {
            setCurrentUser(nonClientUser);
            when(userService.whoami(any())).thenReturn(nonClientUser);

            mockMvc.perform(post("/subscriptions/upgrade")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("[1,2]"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void downgrade_requiresRoleClient() throws Exception {
            setCurrentUser(nonClientUser);
            when(userService.whoami(any())).thenReturn(nonClientUser);

            mockMvc.perform(get("/subscriptions/downgrade")
                            .param("usersIds", "1,2"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    class RoutingAndSerialization {

        @Test
        void upgrade_returnsSuccessResponse() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            doNothing().when(subscriptionService).upgrade(any(), any());

            mockMvc.perform(post("/subscriptions/upgrade")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("[1,2]"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Users enabled successfully"));

            verify(subscriptionService).upgrade(eq(Arrays.asList(1L, 2L)), eq(clientUser));
        }

        @Test
        void downgrade_returnsSuccessResponse() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            doNothing().when(subscriptionService).downgrade(any(), any());

            mockMvc.perform(get("/subscriptions/downgrade")
                            .param("usersIds", "1,2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Users disabled successfully"));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Collection<Long>> captor = ArgumentCaptor.forClass(Collection.class);
            verify(subscriptionService).downgrade(captor.capture(), eq(clientUser));
            assertTrue(captor.getValue().containsAll(Arrays.asList(1L, 2L)));
        }
    }

    @Nested
    class ExceptionMapping {

        @Test
        void upgrade_serviceThrowsCustomException_returnsMappedStatus() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            doThrow(new CustomException("The subscription users count doesn't permit this operation",
                    HttpStatus.NOT_ACCEPTABLE)).when(subscriptionService).upgrade(any(), any());

            mockMvc.perform(post("/subscriptions/upgrade")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("[1,2]"))
                    .andExpect(status().isNotAcceptable())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message")
                            .value("The subscription users count doesn't permit this operation"));
        }


        @Test
        void downgrade_serviceThrowsAccessDenied_returns403() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            doThrow(new CustomException("Access Denied", HttpStatus.FORBIDDEN))
                    .when(subscriptionService).downgrade(any(), any());

            mockMvc.perform(get("/subscriptions/downgrade")
                            .param("usersIds", "1,2"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Access Denied"));
        }
    }
}
