package com.grash.security;

import com.grash.exception.CustomException;
import com.grash.model.Company;
import com.grash.model.OwnUser;
import com.grash.model.Subscription;
import com.grash.repository.UserRepository;
import com.grash.service.*;
import com.grash.utils.Helper;
import com.grash.utils.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final OAuth2Properties oAuth2Properties;
    private final UserRepository userRepository;
    private final SubscriptionService subscriptionService;
    private final SubscriptionPlanService subscriptionPlanService;
    private final CurrencyService currencyService;
    private final EmailService2 emailService2;
    private final CompanyService companyService;
    @Value("${mail.recipients:#{null}}")
    private String[] recipients;
    @Value("${cloud-version}")
    private boolean cloudVersion;
    private final Utils utils;
    @Autowired
    @Lazy
    private PasswordEncoder passwordEncoder;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        if (response.isCommitted()) {
            return;
        }

        String targetUrl = determineTargetUrl(request, response, authentication);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) {
        try {
            Optional<String> redirectUri = Optional.ofNullable(request.getParameter("redirect_uri"));

            String targetUrl = redirectUri.orElse(oAuth2Properties.getSuccessRedirectUrl());

            // Extract user details and generate token
            OAuth2AuthenticationToken authToken = (OAuth2AuthenticationToken) authentication;
            OAuth2User oauth2User = authToken.getPrincipal();
            Map<String, Object> attributes = oauth2User.getAttributes();

            // Get email from OAuth provider
            String email = extractEmail(attributes, authToken.getAuthorizedClientRegistrationId());

            // Find or create user
            Optional<OwnUser> userOptional = userRepository.findByEmailIgnoreCase(email);
            OwnUser user;

            if (!userOptional.isPresent()) {
                // Auto-register new users from SSO if they don't exist
                // In a real implementation, you might want more complex logic here
                user = createUserFromOAuth(email, attributes, authToken.getAuthorizedClientRegistrationId());
            } else {
                user = userOptional.get();

                // Update SSO provider details if this is first time login with this provider
                if (user.getSsoProvider() == null || !user.getSsoProvider().equals(authToken.getAuthorizedClientRegistrationId())) {
                    user.setSsoProvider(authToken.getAuthorizedClientRegistrationId());
                    user.setSsoProviderId(extractProviderId(attributes, authToken.getAuthorizedClientRegistrationId()));
                    userRepository.save(user);
                }
            }

            // Generate JWT token
            String token = jwtTokenProvider.createToken(user.getEmail(),
                    Collections.singletonList(user.getRole().getRoleType()));
            return UriComponentsBuilder.fromUriString(targetUrl)
                    .queryParam("token", token)
                    .build().toUriString();
        } catch (Exception e) {
            return UriComponentsBuilder.fromUriString(oAuth2Properties.getFailureRedirectUrl())
                    .queryParam("error", e.getLocalizedMessage())
                    .build().toUriString();
        }

    }

    private OwnUser createUserFromOAuth(String email, Map<String, Object> attributes, String provider) {
        OwnUser user = new OwnUser();
        user.setEmail(email);
        String emailDomain = user.getEmail().split("@")[1];
        List<OwnUser> users = userRepository.findBySSOCompany(emailDomain);
        if (!users.isEmpty())
            throw new CustomException("You must be invited to your organization", HttpStatus.BAD_REQUEST);
        user.setEnabled(true);
        user.setCreatedViaSso(true);
        user.setSsoProvider(provider);
        user.setSsoProviderId(extractProviderId(attributes, provider));
        user.setFirstName(extractFirstName(attributes, provider));
        user.setLastName(extractLastName(attributes, provider));
        user.setUsername(utils.generateStringId());
        user.setPassword(passwordEncoder.encode(utils.generateStringId()));

        try {
            Subscription subscription = Subscription.builder()
                    .usersCount(cloudVersion ? 10 : 100)
                    .monthly(cloudVersion)
                    .startsOn(new Date())
                    .endsOn(cloudVersion ? Helper.incrementDays(new Date(), 15) : null)
                    .subscriptionPlan(subscriptionPlanService.findByCode("BUSINESS").get())
                    .build();

            subscriptionService.create(subscription);

            Company company = new Company("Organization " + emailDomain, 10, subscription);
            company.getCompanySettings().getGeneralPreferences().setCurrency(currencyService.findByCode("$").get());

            companyService.create(company);

            user.setOwnsCompany(true);
            user.setRole(company.getCompanySettings().getRoleList().stream()
                    .filter(role -> role.getName().equals("Administrator"))
                    .findFirst().get());

            user.setCompany(company);
            OwnUser savedUser = userRepository.save(user);

            // Send notification to super admins about new SSO account
            if (recipients != null && recipients.length > 0) {
                try {
                    emailService2.sendHtmlMessage(
                            recipients,
                            "New Atlas SSO registration",
                            user.getFirstName() + " " + user.getLastName() +
                                    " just created an account via SSO from company " + company.getName() +
                                    ".\nEmail: " + user.getEmail()
                    );
                } catch (Exception e) {
                    log.error("Failed to send notification email about new SSO user", e);
                }
            }

            return savedUser;
        } catch (Exception e) {
            e.printStackTrace();
            throw new CustomException("Error creating user from SSO: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String extractEmail(Map<String, Object> attributes, String registrationId) {
        String email;

        switch (registrationId) {
            case "google":
                email = (String) attributes.get("email");
                break;
            case "github":
                email = (String) attributes.get("email");
                break;
            case "microsoft":
                // Microsoft Entra ID (Azure AD) can return email in different attributes
                email = (String) attributes.get("email");
                if (email == null) {
                    email = (String) attributes.get("preferred_username");
                }
                break;
            default:
                throw new CustomException("Unsupported OAuth2 provider", HttpStatus.BAD_REQUEST);
        }

        if (email == null || email.isEmpty()) {
            throw new CustomException("Email not found from OAuth2 provider", HttpStatus.BAD_REQUEST);
        }

        return email;
    }

    private String extractProviderId(Map<String, Object> attributes, String registrationId) {
        switch (registrationId) {
            case "google":
            case "microsoft":
                return (String) attributes.get("sub");
            case "github":
                return String.valueOf(attributes.get("id"));
            default:
                return "unknown";
        }
    }

    private String extractFirstName(Map<String, Object> attributes, String registrationId) {
        switch (registrationId) {
            case "google":
            case "microsoft":
                return (String) attributes.get("given_name");
            case "github":
                String name = (String) attributes.get("name");
                return name != null ? name.split(" ")[0] : "User";
            default:
                return "User";
        }
    }

    private String extractLastName(Map<String, Object> attributes, String registrationId) {
        switch (registrationId) {
            case "google":
                return (String) attributes.get("family_name");
            case "github":
                String name = (String) attributes.get("name");
                String[] parts = name != null ? name.split(" ") : new String[]{"User"};
                return parts.length > 1 ? parts[1] : "";
            case "microsoft":
                String lastName = (String) attributes.get("family_name");
                if (lastName == null) {
                    lastName = "";
                }
                return lastName;
            default:
                return "";
        }
    }
}
