package com.grash.controller;

import com.grash.dto.*;
import com.grash.exception.CustomException;
import com.grash.mapper.UserMapper;
import com.grash.model.*;
import com.grash.model.enums.RoleType;
import com.grash.repository.SuperAccountRelationRepository;
import com.grash.repository.UserRepository;
import com.grash.security.JwtTokenProvider;
import com.grash.service.CompanyService;
import com.grash.service.LdapService;
import com.grash.service.UserService;
import com.grash.service.VerificationTokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Optional;

import static com.grash.utils.Helper.setCurrentUser;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest extends AbstractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private VerificationTokenService verificationTokenService;

    @MockBean
    private UserMapper userMapper;

    @MockBean
    private SuperAccountRelationRepository superAccountRelationRepository;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CompanyService companyService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private LdapService ldapService;

    private User clientUser;
    private User superAdminUser;
    private UserResponseDTO userResponseDto;

    @BeforeEach
    void setUp() {
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
        clientUser.setPassword("encoded-password");

        Role superAdminRole = Role.builder()
                .id(2L)
                .roleType(RoleType.ROLE_SUPER_ADMIN)
                .name("Super Admin")
                .build();

        superAdminUser = new User();
        superAdminUser.setId(2L);
        superAdminUser.setFirstName("Admin");
        superAdminUser.setLastName("User");
        superAdminUser.setEmail("admin@test.com");
        superAdminUser.setRole(superAdminRole);
        superAdminUser.setEnabled(true);

        userResponseDto = new UserResponseDTO();
        userResponseDto.setId(1);
        userResponseDto.setFirstName("John");
        userResponseDto.setLastName("Doe");
        userResponseDto.setEmail("john@test.com");
    }

    @Nested
    class AuthorizationTests {

        @Test
        void delete_requiresSuperAdmin() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);

            mockMvc.perform(delete("/auth/testuser"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void search_requiresSuperAdmin() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);

            mockMvc.perform(get("/auth/testuser"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void switchAccount_requiresClient() throws Exception {
            setCurrentUser(superAdminUser);
            when(userService.whoami(any())).thenReturn(superAdminUser);

            mockMvc.perform(get("/auth/switch-account?id=1"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    class ExceptionMapping {

        @Test
        void signup_emailAlreadyInUse_returns422() throws Exception {
            when(userService.signup(any()))
                    .thenThrow(new CustomException("Email is already in use", HttpStatus.UNPROCESSABLE_ENTITY));

            mockMvc.perform(post("/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"existing@test.com\",\"password\":\"pass123\"," +
                                    "\"firstName\":\"John\",\"lastName\":\"Doe\",\"phone\":\"12345678\"}"))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        void signin_invalidCredentials_returns403() throws Exception {
            when(userService.signin(anyString(), anyString(), anyString()))
                    .thenThrow(new CustomException("Invalid credentials", HttpStatus.FORBIDDEN));

            mockMvc.perform(post("/auth/signin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"wrong@test.com\",\"password\":\"wrong\",\"type\":\"CLIENT\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void updatePassword_badCredentials_returns406() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

            mockMvc.perform(post("/auth/updatepwd")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"oldPassword\":\"wrong1\",\"newPassword\":\"newpass123\"}"))
                    .andExpect(status().isNotAcceptable())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Bad credentials"));
        }
    }

    @Nested
    class RoutingAndSerialization {

        @Test
        void signin_returnsAuthResponse() throws Exception {
            when(userService.signin(eq("john@test.com"), eq("pass123"), eq("CLIENT"))).thenReturn("jwt-token");

            mockMvc.perform(post("/auth/signin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"john@test.com\",\"password\":\"pass123\",\"type\":\"CLIENT\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("jwt-token"));
        }

        @Test
        void signin_lowercasesEmail() throws Exception {
            when(userService.signin(eq("john@test.com"), eq("pass123"), eq("CLIENT"))).thenReturn("jwt-token");

            mockMvc.perform(post("/auth/signin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"John@Test.Com\",\"password\":\"pass123\",\"type\":\"CLIENT\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("jwt-token"));
        }

        @Test
        void signinLdap_returnsAuthResponse() throws Exception {
            LdapLoginRequest ldapRequest = new LdapLoginRequest("ldapuser", "ldappass");
            when(ldapService.signinLdap(any(LdapLoginRequest.class))).thenReturn("ldap-jwt-token");

            mockMvc.perform(post("/auth/signin-ldap")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"ldapuser\",\"password\":\"ldappass\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("ldap-jwt-token"));
        }

        @SuppressWarnings("unchecked")
        @Test
        void signup_returnsSignupSuccessResponse() throws Exception {
            SignupSuccessResponse<User> serviceResponse = new SignupSuccessResponse<>(true, "Success", clientUser);
            when(userService.signup(any(UserSignupRequest.class))).thenReturn(serviceResponse);
            when(userMapper.toResponseDto(clientUser)).thenReturn(userResponseDto);

            mockMvc.perform(post("/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"new@test.com\",\"password\":\"pass123\",\"firstName\":\"John\"," +
                                    "\"lastName\":\"Doe\",\"phone\":\"12345678\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Success"))
                    .andExpect(jsonPath("$.user.email").value("john@test.com"));
        }

        @Test
        void activateAccount_validToken_redirectsToLogin() throws Exception {
            String token = "valid-token";
            when(verificationTokenService.confirmMail(token)).thenReturn("activated@test.com");

            mockMvc.perform(get("/auth/activate-account?token=" + token))
                    .andExpect(status().isFound())
                    .andExpect(header().string("Location", endsWith("/account/login?email=activated@test.com")));
        }

        @Test
        void activateAccount_invalidToken_redirectsToRegister() throws Exception {
            String token = "invalid-token";
            when(verificationTokenService.confirmMail(token)).thenThrow(new Exception("Invalid token"));

            mockMvc.perform(get("/auth/activate-account?token=" + token))
                    .andExpect(status().isFound())
                    .andExpect(header().string("Location", endsWith("/account/register")));
        }

        @Test
        void resetPasswordConfirm_validToken_redirectsToLogin() throws Exception {
            String token = "valid-reset-token";
            when(verificationTokenService.confirmResetPassword(token)).thenReturn(clientUser);

            mockMvc.perform(get("/auth/reset-pwd-confirm?token=" + token))
                    .andExpect(status().isFound())
                    .andExpect(header().string("Location", endsWith("/account/login?email=john@test.com")));
        }

        @Test
        void resetPasswordConfirm_invalidToken_redirectsToRegister() throws Exception {
            String token = "invalid-reset-token";
            when(verificationTokenService.confirmResetPassword(token)).thenThrow(new Exception("Invalid token"));

            mockMvc.perform(get("/auth/reset-pwd-confirm?token=" + token))
                    .andExpect(status().isFound())
                    .andExpect(header().string("Location", endsWith("/account/register")));
        }

        @Test
        void delete_returnsUsername() throws Exception {
            setCurrentUser(superAdminUser);
            when(userService.whoami(any())).thenReturn(superAdminUser);
            doNothing().when(userService).delete("todelete");

            mockMvc.perform(delete("/auth/todelete"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("todelete"));
        }

        @Test
        void search_returnsUserResponseDTO() throws Exception {
            setCurrentUser(superAdminUser);
            when(userService.whoami(any())).thenReturn(superAdminUser);
            when(userService.findByEmail("test@test.com")).thenReturn(Optional.of(clientUser));
            when(userMapper.toResponseDto(clientUser)).thenReturn(userResponseDto);

            mockMvc.perform(get("/auth/test@test.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("john@test.com"));
        }

        @Test
        void whoami_returnsUserResponseDTO() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any(HttpServletRequest.class), eq(false))).thenReturn(clientUser);
            when(userMapper.toResponseDto(clientUser)).thenReturn(userResponseDto);

            mockMvc.perform(get("/auth/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("john@test.com"));
        }

        @Test
        void refresh_returnsAuthResponse() throws Exception {
            setCurrentUser(clientUser);
            when(userService.refresh(any())).thenReturn("refreshed-token");

            mockMvc.perform(get("/auth/refresh"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("refreshed-token"));
        }

        @Test
        void resetPassword_returnsSuccessResponse() throws Exception {
            when(userService.resetPasswordRequest("test@test.com"))
                    .thenReturn(new SuccessResponse(true, "Password changed successfully"));

            mockMvc.perform(get("/auth/resetpwd?email=test@test.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Password changed successfully"));
        }

        @Test
        void updatePassword_success_returnsOk() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            when(passwordEncoder.matches("oldPass", "encoded-password")).thenReturn(true);
            when(passwordEncoder.encode("newPass")).thenReturn("new-encoded");
            when(userService.save(any())).thenReturn(clientUser);

            mockMvc.perform(post("/auth/updatepwd")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"oldPassword\":\"oldPass\",\"newPassword\":\"newPass\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Password changed successfully"));
        }

        @Test
        void switchAccount_asSuperUser_returnsAuthResponse() throws Exception {
            SuperAccountRelation relation = SuperAccountRelation.builder()
                    .id(1L)
                    .superUser(clientUser)
                    .childUser(superAdminUser)
                    .build();

            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            clientUser.setSuperAccountRelations(Collections.singletonList(relation));
            when(superAccountRelationRepository.findBySuperUser_IdAndChildUser_Id(1L, 2L)).thenReturn(relation);
            when(userService.findById(2L)).thenReturn(Optional.of(superAdminUser));
            when(jwtTokenProvider.createToken(eq("admin@test.com"), anyList())).thenReturn("switched-token");

            mockMvc.perform(get("/auth/switch-account?id=2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("switched-token"));
        }

        @Test
        void switchAccount_asChildUser_returnsAuthResponse() throws Exception {
            SuperAccountRelation relation = SuperAccountRelation.builder()
                    .id(2L)
                    .superUser(superAdminUser)
                    .childUser(clientUser)
                    .build();

            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            clientUser.setSuperAccountRelations(Collections.emptyList());
            clientUser.setParentSuperAccount(relation);
            when(superAccountRelationRepository.findBySuperUser_IdAndChildUser_Id(2L, 1L)).thenReturn(relation);
            when(userService.findById(2L)).thenReturn(Optional.of(superAdminUser));
            when(jwtTokenProvider.createToken(eq("admin@test.com"), anyList())).thenReturn("switched-token");

            mockMvc.perform(get("/auth/switch-account?id=2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("switched-token"));
        }

        @Test
        void switchAccount_noRelation_returns403() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            clientUser.setSuperAccountRelations(Collections.singletonList(new SuperAccountRelation()));

            when(superAccountRelationRepository.findBySuperUser_IdAndChildUser_Id(1L, 99L)).thenReturn(null);

            mockMvc.perform(get("/auth/switch-account?id=99"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void switchAccount_childUserDisabled_returns403() throws Exception {
            SuperAccountRelation relation = SuperAccountRelation.builder()
                    .id(1L)
                    .superUser(clientUser)
                    .childUser(superAdminUser)
                    .build();

            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            clientUser.setSuperAccountRelations(Collections.singletonList(relation));
            when(superAccountRelationRepository.findBySuperUser_IdAndChildUser_Id(1L, 2L)).thenReturn(relation);
            superAdminUser.setEnabled(false);
            when(userService.findById(2L)).thenReturn(Optional.of(superAdminUser));

            mockMvc.perform(get("/auth/switch-account?id=2"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void switchAccount_fallThrough_noSuperRole_returns403() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            clientUser.setSuperAccountRelations(Collections.emptyList());
            clientUser.setParentSuperAccount(null);

            mockMvc.perform(get("/auth/switch-account?id=99"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void switchAccount_childUser_nullRelation_returns403() throws Exception {
            SuperAccountRelation relation = SuperAccountRelation.builder()
                    .id(2L)
                    .superUser(superAdminUser)
                    .childUser(clientUser)
                    .build();

            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            clientUser.setSuperAccountRelations(Collections.emptyList());
            clientUser.setParentSuperAccount(relation);

            when(superAccountRelationRepository.findBySuperUser_IdAndChildUser_Id(99L, 1L)).thenReturn(null);

            mockMvc.perform(get("/auth/switch-account?id=99"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void switchAccount_childUser_superUserDisabled_returns403() throws Exception {
            SuperAccountRelation relation = SuperAccountRelation.builder()
                    .id(2L)
                    .superUser(superAdminUser)
                    .childUser(clientUser)
                    .build();

            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            clientUser.setSuperAccountRelations(Collections.emptyList());
            clientUser.setParentSuperAccount(relation);
            superAdminUser.setEnabled(false);

            when(superAccountRelationRepository.findBySuperUser_IdAndChildUser_Id(2L, 1L)).thenReturn(relation);
            when(userService.findById(2L)).thenReturn(Optional.of(superAdminUser));

            mockMvc.perform(get("/auth/switch-account?id=2"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void deleteAccount_ownsCompany_deletesCompany() throws Exception {
            Company company = new Company();
            company.setId(1L);
            clientUser.setOwnsCompany(true);
            clientUser.setCompany(company);

            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            doNothing().when(companyService).delete(1L);

            mockMvc.perform(delete("/auth"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Account deleted successfully"));

            verify(companyService).delete(1L);
            verify(userRepository, never()).delete((User) any());
        }

        @Test
        void deleteAccount_notOwnsCompany_deletesUser() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            clientUser.setOwnsCompany(false);
            doNothing().when(userRepository).delete(clientUser);

            mockMvc.perform(delete("/auth"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Account deleted successfully"));

            verify(companyService, never()).delete(any());
            verify(userRepository).delete((User) clientUser);
        }
    }
}
