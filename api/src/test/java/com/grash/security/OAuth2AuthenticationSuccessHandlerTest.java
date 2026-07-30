package com.grash.security;

import com.grash.dto.BrandConfig;
import com.grash.factory.MailServiceFactory;
import com.grash.model.*;
import com.grash.model.enums.PlanFeatures;
import com.grash.model.enums.RoleCode;
import com.grash.model.enums.RoleType;
import com.grash.repository.UserRepository;
import com.grash.service.*;
import com.grash.utils.Utils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationSuccessHandlerTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private OAuth2Properties oAuth2Properties;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SubscriptionService subscriptionService;
    @Mock
    private SubscriptionPlanService subscriptionPlanService;
    @Mock
    private CurrencyService currencyService;
    @Mock
    private MailServiceFactory mailServiceFactory;
    @Mock
    private CompanyService companyService;
    @Mock
    private Utils utils;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private BrandingService brandingService;
    @Mock
    private MailService mailService;
    @Mock
    private RoleService roleService;

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private OAuth2AuthenticationToken authToken;
    @Mock
    private OAuth2User oauth2User;

    @InjectMocks
    private OAuth2AuthenticationSuccessHandler handler;

    private final String successUrl = "https://app.com/success";
    private final String failureUrl = "https://app.com/error";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(handler, "recipients", (Object) null);
        ReflectionTestUtils.setField(handler, "cloudVersion", false);
        ReflectionTestUtils.setField(handler, "passwordEncoder", passwordEncoder);
        ReflectionTestUtils.setField(handler, "brandingService", brandingService);
    }

    private User createUser(String email, String ssoProvider, String ssoProviderId) {
        Role role = new Role();
        role.setRoleType(RoleType.ROLE_CLIENT);
        User user = new User();
        user.setId(1L);
        user.setEmail(email);
        user.setRole(role);
        user.setSsoProvider(ssoProvider);
        user.setSsoProviderId(ssoProviderId);
        return user;
    }

    private void stubOAuthFlow(String provider, Map<String, Object> attributes) {
        lenient().when(authToken.getPrincipal()).thenReturn(oauth2User);
        lenient().when(authToken.getAuthorizedClientRegistrationId()).thenReturn(provider);
        lenient().when(oauth2User.getAttributes()).thenReturn(attributes);
    }

    @Test
    void onAuthenticationSuccess_responseAlreadyCommitted_returnsEarly() throws Exception {
        when(response.isCommitted()).thenReturn(true);

        handler.onAuthenticationSuccess(request, response, authToken);

        verifyNoInteractions(oAuth2Properties);
    }

    @Test
    void onAuthenticationSuccess_callsDetermineTargetUrlAndRedirects() throws Exception {
        RedirectStrategy redirectStrategy = mock(RedirectStrategy.class);
        ReflectionTestUtils.setField(handler, "redirectStrategy", redirectStrategy);
        OAuth2AuthenticationSuccessHandler spyHandler = spy(handler);
        doReturn("https://app.com/callback").when(spyHandler).determineTargetUrl(request, response, authToken);

        spyHandler.onAuthenticationSuccess(request, response, authToken);

        verify(redirectStrategy).sendRedirect(request, response, "https://app.com/callback");
        verify(spyHandler).determineTargetUrl(request, response, authToken);
    }

    @Nested
    class DetermineTargetUrl {

        @Test
        void existingUser_returnsUrlWithToken() {
            String email = "john@test.com";
            stubOAuthFlow("google", Map.of("email", email, "given_name", "John", "family_name", "Doe", "sub", "123"));
            when(request.getParameter("redirect_uri")).thenReturn(null);
            when(oAuth2Properties.getSuccessRedirectUrl()).thenReturn(successUrl);
            when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(createUser(email, null, null)));
            when(jwtTokenProvider.createToken(eq(email), anyList())).thenReturn("jwt-token");

            String result = handler.determineTargetUrl(request, response, authToken);

            assertTrue(result.startsWith(successUrl));
            assertTrue(result.contains("token=jwt-token"));
        }

        @Test
        void existingUser_updatesSsoProviderOnFirstLogin() {
            String email = "john@test.com";
            User user = createUser(email, null, null);
            Map<String, Object> attrs = Map.of("email", email, "given_name", "John", "family_name", "Doe", "sub",
                    "456");
            stubOAuthFlow("google", attrs);
            when(request.getParameter("redirect_uri")).thenReturn(null);
            when(oAuth2Properties.getSuccessRedirectUrl()).thenReturn(successUrl);
            when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
            when(userRepository.save(user)).thenReturn(user);
            when(jwtTokenProvider.createToken(eq(email), anyList())).thenReturn("jwt-token");

            handler.determineTargetUrl(request, response, authToken);

            assertEquals("google", user.getSsoProvider());
            assertEquals("456", user.getSsoProviderId());
            verify(userRepository).save(user);
        }

        @Test
        void existingUser_doesNotUpdateSsoProviderIfSameProvider() {
            String email = "john@test.com";
            User user = createUser(email, "google", "existing-id");
            Map<String, Object> attrs = Map.of("email", email, "given_name", "John", "family_name", "Doe", "sub",
                    "456");
            stubOAuthFlow("google", attrs);
            when(request.getParameter("redirect_uri")).thenReturn(null);
            when(oAuth2Properties.getSuccessRedirectUrl()).thenReturn(successUrl);
            when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
            when(jwtTokenProvider.createToken(eq(email), anyList())).thenReturn("jwt-token");

            handler.determineTargetUrl(request, response, authToken);

            assertEquals("google", user.getSsoProvider());
            assertEquals("existing-id", user.getSsoProviderId());
            verify(userRepository, never()).save(any());
        }

        @Test
        void existingUser_differentSsoProvider_updatesSsoDetails() {
            String email = "john@test.com";
            User user = createUser(email, "microsoft", "999");
            Map<String, Object> attrs = Map.of("email", email, "given_name", "John", "family_name", "Doe", "sub",
                    "456");
            stubOAuthFlow("google", attrs);
            when(request.getParameter("redirect_uri")).thenReturn(null);
            when(oAuth2Properties.getSuccessRedirectUrl()).thenReturn(successUrl);
            when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
            when(userRepository.save(user)).thenReturn(user);
            when(jwtTokenProvider.createToken(eq(email), anyList())).thenReturn("jwt-token");

            handler.determineTargetUrl(request, response, authToken);

            assertEquals("google", user.getSsoProvider());
            assertEquals("456", user.getSsoProviderId());
            verify(userRepository).save(user);
        }

        @Test
        void redirectUriParam_overridesDefault() {
            String email = "john@test.com";
            stubOAuthFlow("google", Map.of("email", email, "given_name", "John", "family_name", "Doe", "sub", "123"));
            when(request.getParameter("redirect_uri")).thenReturn("https://custom.com/callback");
            when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(createUser(email, null, null)));
            when(jwtTokenProvider.createToken(eq(email), anyList())).thenReturn("jwt-token");

            String result = handler.determineTargetUrl(request, response, authToken);

            assertTrue(result.startsWith("https://custom.com/callback"));
            assertTrue(result.contains("token=jwt-token"));
        }

        @Test
        void unsupportedProvider_returnsFailureUrlWithError() {
            Map<String, Object> attrs = Map.of("email", "user@test.com");
            stubOAuthFlow("unsupported", attrs);
            lenient().when(oAuth2Properties.getFailureRedirectUrl()).thenReturn(failureUrl);

            String result = handler.determineTargetUrl(request, response, authToken);

            assertTrue(result.startsWith(failureUrl));
            assertTrue(result.contains("Unsupported OAuth2 provider"));
        }

        @Test
        void emailNotFoundFromProvider_returnsFailureUrlWithError() {
            stubOAuthFlow("google", Map.of());
            lenient().when(oAuth2Properties.getFailureRedirectUrl()).thenReturn(failureUrl);

            String result = handler.determineTargetUrl(request, response, authToken);

            assertTrue(result.startsWith(failureUrl));
            assertTrue(result.contains("error"));
        }

        @Test
        void exceptionInOAuthUserAttributes_returnsFailureUrl() {
            stubOAuthFlow("google", Map.of("email", "x@x.com"));
            when(oauth2User.getAttributes()).thenThrow(new RuntimeException("Unexpected error"));
            lenient().when(oAuth2Properties.getFailureRedirectUrl()).thenReturn(failureUrl);

            String result = handler.determineTargetUrl(request, response, authToken);

            assertTrue(result.startsWith(failureUrl));
        }
    }

    @Nested
    class NewUserAutoRegister {

        private final String email = "new@testcorp.com";
        private final String emailDomain = "testcorp.com";
        private Role adminRole;

        @BeforeEach
        void init() {
            adminRole = new Role();
            adminRole.setName("Administrator");
            adminRole.setRoleType(RoleType.ROLE_CLIENT);
            adminRole.setCode(RoleCode.ADMIN);
        }

        private void stubNewUserFlow() {
            Map<String, Object> attrs = Map.of(
                    "email", email,
                    "given_name", "Jane",
                    "family_name", "Smith",
                    "sub", "oauth456"
            );
            stubOAuthFlow("google", attrs);
            when(request.getParameter("redirect_uri")).thenReturn(null);
            lenient().when(oAuth2Properties.getSuccessRedirectUrl()).thenReturn(successUrl);
            lenient().when(oAuth2Properties.getFailureRedirectUrl()).thenReturn(failureUrl);
            when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.empty());
            when(userRepository.findBySSOCompany(emailDomain)).thenReturn(List.of());
            lenient().when(utils.generateStringId()).thenReturn("randId");
            lenient().when(passwordEncoder.encode("randId")).thenReturn("encoded");
            lenient().when(jwtTokenProvider.createToken(eq(email), anyList())).thenReturn("jwt-token");
            lenient().when(brandingService.getBrandConfig()).thenReturn(new BrandConfig());
        }

        private void stubCreateCompany() {
            when(subscriptionPlanService.findByCode("BUSINESS"))
                    .thenReturn(Optional.of(SubscriptionPlan.builder()
                            .features(new HashSet<>(Set.of(PlanFeatures.API_ACCESS)))
                            .build()));
            lenient().when(subscriptionService.create(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));
            lenient().when(currencyService.findByCode("$")).thenReturn(Optional.of(new com.grash.model.Currency()));
            lenient().when(companyService.create(any(Company.class))).thenAnswer(inv -> inv.getArgument(0));
            lenient().when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            lenient().when(roleService.findDefaultRoleWithCode(RoleCode.ADMIN)).thenReturn(Optional.of(adminRole));
        }

        @Test
        void createsCompanyAndUserAndReturnsUrlWithToken() {
            stubNewUserFlow();
            stubCreateCompany();

            String result = handler.determineTargetUrl(request, response, authToken);

            assertTrue(result.startsWith(successUrl),
                    () -> "Expected success URL but got: " + result);
            verify(companyService).create(any(Company.class));
            verify(userRepository).save(any(User.class));
            assertTrue(result.contains("token=jwt-token"));
        }

        @Test
        void emailDomainAlreadyHasSsoCompany_throws() {
            Map<String, Object> attrs = Map.of(
                    "email", email,
                    "given_name", "Jane",
                    "family_name", "Smith",
                    "sub", "oauth456"
            );
            stubOAuthFlow("google", attrs);
            when(request.getParameter("redirect_uri")).thenReturn(null);
            lenient().when(oAuth2Properties.getSuccessRedirectUrl()).thenReturn(successUrl);
            lenient().when(oAuth2Properties.getFailureRedirectUrl()).thenReturn(failureUrl);
            when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.empty());
            when(userRepository.findBySSOCompany(emailDomain))
                    .thenReturn(List.of(new User()));

            String result = handler.determineTargetUrl(request, response, authToken);

            assertTrue(result.startsWith(failureUrl),
                    () -> "Expected failure URL but got: " + result);
            verify(companyService, never()).create(any());
        }

        @Test
        void subscriptionPlanNotFound_returnsFailureUrl() {
            stubNewUserFlow();
            lenient().when(subscriptionPlanService.findByCode("BUSINESS")).thenReturn(Optional.empty());

            String result = handler.determineTargetUrl(request, response, authToken);

            verify(companyService, never()).create(any());
            assertTrue(result.startsWith(failureUrl),
                    () -> "Expected failure URL but got: " + result);
        }

        @Test
        void microsoftOAuth_createsUserAndReturnsUrlWithToken() {
            String msEmail = "user@outlook.com";
            Map<String, Object> attrs = Map.of(
                    "email", msEmail,
                    "given_name", "Jane",
                    "family_name", "Doe",
                    "sub", "ms-sub-123"
            );
            stubOAuthFlow("microsoft", attrs);
            when(request.getParameter("redirect_uri")).thenReturn(null);
            lenient().when(oAuth2Properties.getSuccessRedirectUrl()).thenReturn(successUrl);
            lenient().when(oAuth2Properties.getFailureRedirectUrl()).thenReturn(failureUrl);
            when(userRepository.findByEmailIgnoreCase(msEmail)).thenReturn(Optional.empty());
            String msDomain = "outlook.com";
            when(userRepository.findBySSOCompany(msDomain)).thenReturn(List.of());
            lenient().when(utils.generateStringId()).thenReturn("randId");
            lenient().when(passwordEncoder.encode("randId")).thenReturn("encoded");
            lenient().when(jwtTokenProvider.createToken(eq(msEmail), anyList())).thenReturn("jwt-token");
            lenient().when(brandingService.getBrandConfig()).thenReturn(new BrandConfig());
            stubCreateCompany();

            String result = handler.determineTargetUrl(request, response, authToken);

            assertTrue(result.startsWith(successUrl),
                    () -> "Expected success URL but got: " + result);
            verify(userRepository).save(argThat(u ->
                    "Jane".equals(u.getFirstName()) && "Doe".equals(u.getLastName())));
            assertTrue(result.contains("token=jwt-token"));
        }

        @Test
        void microsoftOAuth_emailFromPreferredUsername() {
            String msEmail = "user@outlook.com";
            Map<String, Object> attrs = Map.of(
                    "preferred_username", msEmail,
                    "given_name", "Jane",
                    "sub", "ms-sub-456"
            );
            stubOAuthFlow("microsoft", attrs);
            when(request.getParameter("redirect_uri")).thenReturn(null);
            lenient().when(oAuth2Properties.getSuccessRedirectUrl()).thenReturn(successUrl);
            lenient().when(oAuth2Properties.getFailureRedirectUrl()).thenReturn(failureUrl);
            when(userRepository.findByEmailIgnoreCase(msEmail)).thenReturn(Optional.empty());
            String msDomain = "outlook.com";
            when(userRepository.findBySSOCompany(msDomain)).thenReturn(List.of());
            lenient().when(utils.generateStringId()).thenReturn("randId");
            lenient().when(passwordEncoder.encode("randId")).thenReturn("encoded");
            lenient().when(jwtTokenProvider.createToken(eq(msEmail), anyList())).thenReturn("jwt-token");
            lenient().when(brandingService.getBrandConfig()).thenReturn(new BrandConfig());
            stubCreateCompany();

            String result = handler.determineTargetUrl(request, response, authToken);

            assertTrue(result.startsWith(successUrl),
                    () -> "Expected success URL but got: " + result);
            verify(userRepository).save(argThat(u ->
                    "Jane".equals(u.getFirstName()) && "".equals(u.getLastName())));
        }

        @Test
        void oAuthWithRecipients_sendsNotificationEmail() throws Exception {
            ReflectionTestUtils.setField(handler, "cloudVersion", true);
            ReflectionTestUtils.setField(handler, "recipients", new String[]{"admin@test.com"});
            stubNewUserFlow();
            stubCreateCompany();
            when(mailServiceFactory.getMailService()).thenReturn(mailService);

            String result = handler.determineTargetUrl(request, response, authToken);

            assertTrue(result.startsWith(successUrl),
                    () -> "Expected success URL but got: " + result);
            verify(mailServiceFactory).getMailService();
            verify(mailService).sendHtmlMessage(
                    eq(new String[]{"admin@test.com"}),
                    anyString(),
                    anyString()
            );
        }

        @Test
        void oAuthWithRecipients_sendEmailFails_logsError() throws Exception {
            ReflectionTestUtils.setField(handler, "cloudVersion", true);
            ReflectionTestUtils.setField(handler, "recipients", new String[]{"admin@test.com"});
            stubNewUserFlow();
            stubCreateCompany();
            when(mailServiceFactory.getMailService()).thenReturn(mailService);
            doThrow(new RuntimeException("Mail server down"))
                    .when(mailService).sendHtmlMessage(any(), anyString(), anyString());

            String result = handler.determineTargetUrl(request, response, authToken);

            assertTrue(result.startsWith(successUrl),
                    () -> "Expected success URL but got: " + result);
        }
    }
}
