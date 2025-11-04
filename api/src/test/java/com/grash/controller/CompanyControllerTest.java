package com.grash.controller;

import com.grash.dto.CompanyPatchDTO;
import com.grash.dto.CompanyShowDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.CompanyMapper;
import com.grash.model.Company;
import com.grash.model.OwnUser;
import com.grash.model.Role;
import com.grash.model.enums.PermissionEntity;
import com.grash.service.CompanyService;
import com.grash.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyControllerTest {

    @Mock
    private CompanyService companyService;

    @Mock
    private UserService userService;

    @Mock
    private CompanyMapper companyMapper;

    @InjectMocks
    private CompanyController companyController;

    @Mock
    private HttpServletRequest request;

    private OwnUser user;
    private Company company;
    private CompanyShowDTO companyShowDTO;

    @BeforeEach
    void setUp() {
        user = new OwnUser();
        user.setId(1L);
        user.setRole(new Role());

        company = new Company();
        company.setId(1L);

        companyShowDTO = new CompanyShowDTO();
        companyShowDTO.setId(1L);
    }

    @Nested
    @DisplayName("getById tests")
    class GetByIdTests {

        @Test
        @DisplayName("should return company when found")
        void shouldReturnCompanyWhenFound() {
            when(userService.whoami(request)).thenReturn(user);
            when(companyService.findById(1L)).thenReturn(Optional.of(company));
            when(companyMapper.toShowDto(company)).thenReturn(companyShowDTO);

            CompanyShowDTO result = companyController.getById(1L, request);

            assertNotNull(result);
            assertEquals(1L, result.getId());
        }

        @Test
        @DisplayName("should throw CustomException when company not found")
        void shouldThrowCustomExceptionWhenCompanyNotFound() {
            when(userService.whoami(request)).thenReturn(user);
            when(companyService.findById(1L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class, () -> {
                companyController.getById(1L, request);
            });

            assertEquals("Not found", exception.getMessage());
            assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        }
    }

    @Nested
    @DisplayName("patch tests")
    class PatchTests {

        private CompanyPatchDTO companyPatchDTO;

        @BeforeEach
        void setUp() {
            companyPatchDTO = new CompanyPatchDTO();
        }

        @Test
        @DisplayName("should patch company when user has permission")
        void shouldPatchCompanyWhenUserHasPermission() {
            user.getRole().setViewPermissions(Collections.singleton(PermissionEntity.SETTINGS));
            when(userService.whoami(request)).thenReturn(user);
            when(companyService.findById(1L)).thenReturn(Optional.of(company));
            when(companyService.update(1L, companyPatchDTO)).thenReturn(company);
            when(companyMapper.toShowDto(company)).thenReturn(companyShowDTO);

            CompanyShowDTO result = companyController.patch(companyPatchDTO, 1L, request);

            assertNotNull(result);
            assertEquals(1L, result.getId());
        }

        @Test
        @DisplayName("should throw CustomException when company not found")
        void shouldThrowCustomExceptionWhenCompanyNotFound() {
            when(userService.whoami(request)).thenReturn(user);
            when(companyService.findById(1L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class, () -> {
                companyController.patch(companyPatchDTO, 1L, request);
            });

            assertEquals("Company not found", exception.getMessage());
            assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        }

        @Test
        @DisplayName("should throw CustomException when user does not have permission")
        void shouldThrowCustomExceptionWhenUserDoesNotHavePermission() {
            user.getRole().setViewPermissions(Collections.emptySet());
            when(userService.whoami(request)).thenReturn(user);
            when(companyService.findById(1L)).thenReturn(Optional.of(company));

            CustomException exception = assertThrows(CustomException.class, () -> {
                companyController.patch(companyPatchDTO, 1L, request);
            });

            assertEquals("Access denied", exception.getMessage());
            assertEquals(HttpStatus.FORBIDDEN, exception.getHttpStatus());
        }
    }
}