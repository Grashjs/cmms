package com.grash.controller;

import com.grash.dto.*;
import com.grash.exception.CustomException;
import com.grash.mapper.UserMapper;
import com.grash.factory.MailServiceFactory;
import com.grash.model.User;
import com.grash.model.SuperAccountRelation;
import com.grash.repository.SuperAccountRelationRepository;
import com.grash.security.CurrentUser;
import com.grash.service.LdapService;
import com.grash.service.RefreshTokenService;
import com.grash.service.UserService;
import com.grash.service.VerificationTokenService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import java.util.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Authentication and authorization operations")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final VerificationTokenService verificationTokenService;
    private final UserMapper userMapper;
    private final SuperAccountRelationRepository superAccountRelationRepository;
    private final LdapService ldapService;
    private final RefreshTokenService refreshTokenService;
    @Value("${frontend.url}")
    private String frontendUrl;

    @PostMapping(
            path = "/signin",
            produces = {
                    MediaType.APPLICATION_JSON_VALUE
            }
    )
    public ResponseEntity<AuthResponse> login(
            @Parameter(description = "User login credentials") @Valid @RequestBody UserLoginRequest userLoginRequest) {
        AuthTokens tokens = userService.signin(userLoginRequest.getEmail().toLowerCase(),
                userLoginRequest.getPassword(), userLoginRequest.getType());
        AuthResponse authResponse = AuthResponse.of(tokens);
        return new ResponseEntity<>(authResponse, HttpStatus.OK);
    }

    @PostMapping(
            path = "/signin-ldap",
            produces = {
                    MediaType.APPLICATION_JSON_VALUE
            }
    )
    public ResponseEntity<AuthResponse> signinLdap(
            @Parameter(description = "LDAP login credentials") @Valid @RequestBody LdapLoginRequest ldapLoginRequest) {
        AuthResponse authResponse = AuthResponse.of(ldapService.signinLdap(ldapLoginRequest));
        return new ResponseEntity<>(authResponse, HttpStatus.OK);
    }

    @PostMapping(
            path = "/signup",
            produces = {
                    MediaType.APPLICATION_JSON_VALUE
            })
    public SignupSuccessResponse<UserResponseDTO> signup(@Parameter(description = "User signup data") @Valid @RequestBody UserSignupRequest user) {
        SignupSuccessResponse<User> response = userService.signup(user);
        return new SignupSuccessResponse<>(response.isSuccess(), response.getMessage(),
                userMapper.toResponseDto(response.getUser()), response.getRefreshToken());
    }

//    @PostMapping(
//            path = "/sendMail",
//            produces = "text/html;charset=UTF-8"
//    )
//    @ApiOperation(value = "${AuthController.signup}")
//    @ApiResponses(value = {//
//            @ApiResponse(code = 400, message = "Something went wrong"), //
//            @ApiResponse(code = 403, message = "Access denied"), //
//            @ApiResponse(code = 422, message = "Username is already in use")})
//    public void sendMail( @Valid @RequestBody UserSignupRequest user) {
//        String email = "ibracool99@gmail.com";
//        String subject = "GG";
//        Map<String, Object> variables = new HashMap<String, Object>() {{
//            put("verifyTokenLink", "gg");
//            put("featuresLink", "s");
//        }};
//        mailServiceFactory.getMailService().sendMessageUsingThymeleafTemplate(new String[]{email}, subject,
//        variables, "new-work-order"
//        + ".html", Locale.FRENCH, null);
//    }

    @GetMapping("/activate-account")
    public void activateAcount(
            @Parameter(description = "Account activation token") @RequestParam String token,
            HttpServletResponse httpServletResponse
    ) {
        try {
            String email = verificationTokenService.confirmMail(token);
            httpServletResponse.setHeader("Location",
                    frontendUrl + "/account/login?email=" + email);
        } catch (Exception ex) {
            httpServletResponse.setHeader("Location", frontendUrl + "/account/register");
        }
        httpServletResponse.setStatus(302);
    }

    @GetMapping("/reset-pwd-confirm")
    public void resetPasswordConfirm(
            @Parameter(description = "Password reset token") @RequestParam String token,
            HttpServletResponse httpServletResponse
    ) {
        try {
            User user = verificationTokenService.confirmResetPassword(token);
            httpServletResponse.setHeader("Location", frontendUrl + "/account/login?email=" + user.getEmail());
        } catch (Exception ex) {
            httpServletResponse.setHeader("Location", frontendUrl + "/account/register");
        }
        httpServletResponse.setStatus(302);
    }

    @DeleteMapping(value = "/{username}")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    public String delete(@PathVariable String username) {
        userService.delete(username);
        return username;
    }

    @GetMapping(value = "/{username}")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    public UserResponseDTO search(@PathVariable String username) {
        return userMapper.toResponseDto(userService.findByEmail(username).get());
    }

    @GetMapping(value = "/me")
    @PreAuthorize("permitAll()")
    public UserResponseDTO whoami(HttpServletRequest req) {
        return userMapper.toResponseDto(userService.whoami(req, false));
    }

    @PostMapping("/refresh")
    @PreAuthorize("permitAll()")
    public AuthResponse refresh(@Parameter(description = "Refresh token request") @Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        AuthTokens tokens = refreshTokenService.rotate(refreshTokenRequest.getRefreshToken());
        return AuthResponse.of(tokens);
    }

    @PostMapping("/logout")
    @PreAuthorize("permitAll()")
    public SuccessResponse logout(@Parameter(hidden = true) @CurrentUser User user) {
        userService.invalidateSessions(user);
        return new SuccessResponse(true, "Logged out successfully");
    }

    @PreAuthorize("permitAll()")
    @GetMapping(value = "/resetpwd", produces = "application/json")
    public SuccessResponse resetPassword(@Parameter(description = "User email address for password reset") @RequestParam String email) {
        return userService.resetPasswordRequest(email);
    }

    @PreAuthorize("permitAll()")
    @PostMapping(value = "/updatepwd", produces = "application/json")
    public AuthResponse updatePassword(@Parameter(description = "Password update request") @Valid @RequestBody UpdatePasswordRequest updatePasswordRequest, HttpServletRequest req) {
        User user = userService.whoami(req);
        String password = user.getPassword();
        String oldPassword = updatePasswordRequest.getOldPassword();
        if (passwordEncoder.matches(oldPassword, password)) {
            user.setPassword(passwordEncoder.encode(updatePasswordRequest.getNewPassword()));
            userService.invalidateSessions(user);
            AuthTokens tokens = refreshTokenService.createTokenPair(user);
            return AuthResponse.of(tokens);
        } else {
            throw new CustomException("Bad credentials", HttpStatus.NOT_ACCEPTABLE);
        }
    }

    @GetMapping("/switch-account")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public AuthResponse switchAccount(
            @Parameter(description = "Target user ID to switch to") @RequestParam("id") Long id, @Parameter(hidden =
                    true) @CurrentUser User user
    ) {
        if (!user.getSuperAccountRelations().isEmpty()) {//user is superUser
            SuperAccountRelation superAccountRelation =
                    superAccountRelationRepository.findBySuperUser_IdAndChildUser_Id(user.getId(), id);
            if (superAccountRelation == null) throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
            User childUser = userService.findById(id).get();
            if (!childUser.isEnabled()) throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
            return AuthResponse.of(refreshTokenService.createTokenPair(childUser));
        } else if (user.getParentSuperAccount() != null) { //user is child
            SuperAccountRelation superAccountRelation =
                    superAccountRelationRepository.findBySuperUser_IdAndChildUser_Id(id, user.getId());
            if (superAccountRelation == null) throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
            User superUser = userService.findById(id).get();
            if (!superUser.isEnabled()) throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
            return AuthResponse.of(refreshTokenService.createTokenPair(superUser));
        }
        throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
    }

    @PreAuthorize("permitAll()")
    @PostMapping(value = "/delete-account-request", produces = "application/json")
    public SuccessResponse deleteAccountRequest(@Parameter(hidden = true) @CurrentUser User user) {
        return userService.deleteAccountRequest(user);
    }

    @GetMapping("/delete-account-confirm")
    public void deleteAccountConfirm(
            @Parameter(description = "Account deletion token") @RequestParam String token,
            HttpServletResponse httpServletResponse
    ) {
        try {
            verificationTokenService.confirmDeleteAccount(token);
            httpServletResponse.setHeader("Location", frontendUrl + "/account/deleted");
        } catch (Exception ex) {
            httpServletResponse.setHeader("Location", frontendUrl + "/account/register");
        }
        httpServletResponse.setStatus(302);
    }

}


