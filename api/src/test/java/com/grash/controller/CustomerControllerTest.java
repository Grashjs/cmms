package com.grash.controller;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.CustomerMiniDTO;
import com.grash.dto.CustomerPatchDTO;
import com.grash.dto.SuccessResponse;
import com.grash.exception.CustomException;
import com.grash.mapper.CustomerMapper;
import com.grash.model.Company;
import com.grash.model.Customer;
import com.grash.model.OwnUser;
import com.grash.model.Role;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.RoleType;
import com.grash.service.CustomerService;
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
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerControllerTest {

    @Mock
    private CustomerService customerService;
    @Mock
    private UserService userService;
    @Mock
    private CustomerMapper customerMapper;
    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private CustomerController customerController;

    private OwnUser user;
    private Customer customer;
    private Company company;
    private Role role;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);

        role = new Role();
        role.setRoleType(RoleType.ROLE_CLIENT);
        role.setViewPermissions(new HashSet<>());
        role.setCreatePermissions(new HashSet<>());
        role.setEditOtherPermissions(new HashSet<>());
        role.setDeleteOtherPermissions(new HashSet<>());

        user = new OwnUser();
        user.setId(1L);
        user.setCompany(company);
        user.setRole(role);

        customer = new Customer();
        customer.setId(1L);
        customer.setCreatedBy(1L);
    }

    @Nested
    @DisplayName("Search Customers Tests")
    class SearchCustomersTests {

        @Mock
        private SearchCriteria searchCriteria;

        @Test
        @DisplayName("Should return customers for client user with permission")
        void shouldReturnCustomersForClientUserWithPermission() {
            role.getViewPermissions().add(PermissionEntity.VENDORS_AND_CUSTOMERS);
            when(userService.whoami(request)).thenReturn(user);
            when(customerService.findBySearchCriteria(any(SearchCriteria.class))).thenReturn(new PageImpl<>(Collections.singletonList(customer)));

            ResponseEntity<Page<Customer>> response = customerController.search(searchCriteria, request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertFalse(response.getBody().isEmpty());
            verify(searchCriteria).filterCompany(user);
        }

        @Test
        @DisplayName("Should throw forbidden for client user without permission")
        void shouldThrowForbiddenForClientUserWithoutPermission() {
            when(userService.whoami(request)).thenReturn(user);

            assertThrows(CustomException.class, () -> customerController.search(searchCriteria, request));
        }

        @Test
        @DisplayName("Should return customers for non-client user")
        void shouldReturnCustomersForNonClientUser() {
            role.setRoleType(RoleType.ROLE_SUPER_ADMIN);
            when(userService.whoami(request)).thenReturn(user);
            when(customerService.findBySearchCriteria(any(SearchCriteria.class))).thenReturn(new PageImpl<>(Collections.singletonList(customer)));

            ResponseEntity<Page<Customer>> response = customerController.search(searchCriteria, request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertFalse(response.getBody().isEmpty());
        }
    }

    @Nested
    @DisplayName("Get Mini Customers Tests")
    class GetMiniCustomersTests {

        @Test
        @DisplayName("Should return mini customer DTOs")
        void shouldReturnMiniCustomerDTOs() {
            when(userService.whoami(request)).thenReturn(user);
            when(customerService.findByCompany(anyLong())).thenReturn(Collections.singletonList(customer));
            when(customerMapper.toMiniDto(any(Customer.class))).thenReturn(new CustomerMiniDTO());

            Collection<CustomerMiniDTO> result = customerController.getMini(request);

            assertFalse(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Get By Id Tests")
    class GetByIdTests {

        @Test
        @DisplayName("Should throw not found when customer does not exist")
        void shouldThrowNotFoundWhenCustomerDoesNotExist() {
            when(userService.whoami(request)).thenReturn(user);
            when(customerService.findById(anyLong())).thenReturn(Optional.empty());

            assertThrows(CustomException.class, () -> customerController.getById(1L, request));
        }

        @Test
        @DisplayName("Should return customer by id with permission")
        void shouldReturnCustomerByIdWithPermission() {
            role.getViewPermissions().add(PermissionEntity.VENDORS_AND_CUSTOMERS);
            when(userService.whoami(request)).thenReturn(user);
            when(customerService.findById(anyLong())).thenReturn(Optional.of(customer));

            Customer result = customerController.getById(1L, request);

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should throw forbidden when customer exists but no permission")
        void shouldThrowForbiddenWhenCustomerExistsButNoPermission() {
            when(userService.whoami(request)).thenReturn(user);
            when(customerService.findById(anyLong())).thenReturn(Optional.of(customer));

            assertThrows(CustomException.class, () -> customerController.getById(1L, request));
        }
    }

    @Nested
    @DisplayName("Create Customer Tests")
    class CreateCustomerTests {

        private Customer customerReq;

        @BeforeEach
        void setup() {
            customerReq = new Customer();
        }

        @Test
        @DisplayName("Should create customer successfully with permission")
        void shouldCreateCustomerSuccessfullyWithPermission() {
            role.getCreatePermissions().add(PermissionEntity.VENDORS_AND_CUSTOMERS);
            when(userService.whoami(request)).thenReturn(user);
            when(customerService.create(any(Customer.class))).thenReturn(customer);

            Customer result = customerController.create(customerReq, request);

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should throw forbidden when no permission to create customer")
        void shouldThrowForbiddenWhenNoPermissionToCreateCustomer() {
            when(userService.whoami(request)).thenReturn(user);

            assertThrows(CustomException.class, () -> customerController.create(customerReq, request));
        }
    }

    @Nested
    @DisplayName("Patch Customer Tests")
    class PatchCustomerTests {

        private CustomerPatchDTO customerPatchDTO;

        @BeforeEach
        void setup() {
            customerPatchDTO = new CustomerPatchDTO();
        }

        @Test
        @DisplayName("Should throw not found when customer does not exist")
        void shouldThrowNotFoundWhenCustomerDoesNotExist() {
            when(userService.whoami(request)).thenReturn(user);
            when(customerService.findById(anyLong())).thenReturn(Optional.empty());

            assertThrows(CustomException.class, () -> customerController.patch(customerPatchDTO, 1L, request));
        }

        @Test
        @DisplayName("Should patch customer successfully with edit other permission")
        void shouldPatchCustomerSuccessfullyWithEditOtherPermission() {
            role.getEditOtherPermissions().add(PermissionEntity.VENDORS_AND_CUSTOMERS);
            when(userService.whoami(request)).thenReturn(user);
            when(customerService.findById(anyLong())).thenReturn(Optional.of(customer));
            when(customerService.update(anyLong(), any(CustomerPatchDTO.class))).thenReturn(customer);

            Customer result = customerController.patch(customerPatchDTO, 1L, request);

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should patch customer successfully when created by current user")
        void shouldPatchCustomerSuccessfullyWhenCreatedByCurrentUser() {
            when(userService.whoami(request)).thenReturn(user);
            when(customerService.findById(anyLong())).thenReturn(Optional.of(customer));
            when(customerService.update(anyLong(), any(CustomerPatchDTO.class))).thenReturn(customer);

            Customer result = customerController.patch(customerPatchDTO, 1L, request);

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should throw forbidden when no edit permission and not created by user")
        void shouldThrowForbiddenWhenNoEditPermissionAndNotCreatedByUser() {
            customer.setCreatedBy(2L); // Different user
            when(userService.whoami(request)).thenReturn(user);
            when(customerService.findById(anyLong())).thenReturn(Optional.of(customer));

            assertThrows(CustomException.class, () -> customerController.patch(customerPatchDTO, 1L, request));
        }
    }

    @Nested
    @DisplayName("Delete Customer Tests")
    class DeleteCustomerTests {

        @Test
        @DisplayName("Should throw not found on delete when customer does not exist")
        void shouldThrowNotFoundOnDeleteWhenCustomerDoesNotExist() {
            when(userService.whoami(request)).thenReturn(user);
            when(customerService.findById(anyLong())).thenReturn(Optional.empty());

            assertThrows(CustomException.class, () -> customerController.delete(1L, request));
        }

        @Test
        @DisplayName("Should delete customer successfully with delete other permission")
        void shouldDeleteCustomerSuccessfullyWithDeleteOtherPermission() {
            role.getDeleteOtherPermissions().add(PermissionEntity.VENDORS_AND_CUSTOMERS);
            when(userService.whoami(request)).thenReturn(user);
            when(customerService.findById(anyLong())).thenReturn(Optional.of(customer));

            ResponseEntity response = customerController.delete(1L, request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(customerService).delete(1L);
        }

        @Test
        @DisplayName("Should delete customer successfully when created by current user")
        void shouldDeleteCustomerSuccessfullyWhenCreatedByCurrentUser() {
            when(userService.whoami(request)).thenReturn(user);
            when(customerService.findById(anyLong())).thenReturn(Optional.of(customer));

            ResponseEntity response = customerController.delete(1L, request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(customerService).delete(1L);
        }

        @Test
        @DisplayName("Should delete customer successfully with delete other permission when not created by current user")
        void shouldDeleteCustomerSuccessfullyWithDeleteOtherPermissionAndNotCreatedByUser() {
            customer.setCreatedBy(2L); // Customer created by a different user
            role.getDeleteOtherPermissions().add(PermissionEntity.VENDORS_AND_CUSTOMERS);
            when(userService.whoami(request)).thenReturn(user);
            when(customerService.findById(anyLong())).thenReturn(Optional.of(customer));

            ResponseEntity response = customerController.delete(1L, request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(customerService).delete(1L);
        }

        @Test
        @DisplayName("Should throw forbidden when no delete permission and not created by user")
        void shouldThrowForbiddenWhenNoDeletePermissionAndNotCreatedByUser() {
            customer.setCreatedBy(2L); // Different user
            when(userService.whoami(request)).thenReturn(user);
            when(customerService.findById(anyLong())).thenReturn(Optional.of(customer));

            assertThrows(CustomException.class, () -> customerController.delete(1L, request));
        }
    }
}
