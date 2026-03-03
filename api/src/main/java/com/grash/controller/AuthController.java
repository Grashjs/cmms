package com.grash.controller;

import com.grash.dto.*;
import com.grash.exception.CustomException;
import com.grash.mapper.UserMapper;
import com.grash.factory.MailServiceFactory;
import com.grash.model.Company;
import com.grash.model.OwnUser;
import com.grash.model.SuperAccountRelation;
import com.grash.repository.SuperAccountRelationRepository;
import com.grash.repository.UserRepository;
import com.grash.security.CurrentUser;
import com.grash.security.JwtTokenProvider;
import com.grash.service.CompanyService;
import com.grash.service.UserService;
import com.grash.service.VerificationTokenService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
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
@Tag(name = "auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final VerificationTokenService verificationTokenService;
    private final UserMapper userMapper;
    private final SuperAccountRelationRepository superAccountRelationRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final MailServiceFactory mailServiceFactory;
    private final CompanyService companyService;
    private final UserRepository userRepository;
    @Value("${frontend.url}")
    private String frontendUrl;

    @PostMapping(
            path = "/signin",
            produces = {
                    MediaType.APPLICATION_JSON_VALUE
            }
    )
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody UserLoginRequest userLoginRequest) {
        AuthResponse authResponse = new AuthResponse(userService.signin(userLoginRequest.getEmail().toLowerCase(),
                userLoginRequest.getPassword(), userLoginRequest.getType()));
        return new ResponseEntity<>(authResponse, HttpStatus.OK);
    }

    @PostMapping(
            path = "/signup",
            produces = {
                    MediaType.APPLICATION_JSON_VALUE
            })
    public SignupSuccessResponse<UserResponseDTO> signup(@Valid @RequestBody UserSignupRequest user) {
        SignupSuccessResponse<OwnUser> response = userService.signup(user);
        return new SignupSuccessResponse<>(response.isSuccess(), response.getMessage(),
                userMapper.toResponseDto(response.getUser()));
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
            @RequestParam String token, HttpServletResponse httpServletResponse
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
            @RequestParam String token,
            HttpServletResponse httpServletResponse
    ) {
        try {
            OwnUser user = verificationTokenService.confirmResetPassword(token);
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

    @GetMapping("/refresh")
    @PreAuthorize("permitAll()")
    public AuthResponse refresh(HttpServletRequest req) {
        return new AuthResponse(userService.refresh(req.getRemoteUser()));
    }

    @PreAuthorize("permitAll()")
    @GetMapping(value = "/resetpwd", produces = "application/json")
    public SuccessResponse resetPassword(@RequestParam String email) {
        return userService.resetPasswordRequest(email);
    }

    @PreAuthorize("permitAll()")
    @PostMapping(value = "/updatepwd", produces = "application/json")
    public ResponseEntity<SuccessResponse> updatePassword(@Valid @RequestBody UpdatePasswordRequest updatePasswordRequest, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        String password = user.getPassword();
        String oldPassword = updatePasswordRequest.getOldPassword();
        if (passwordEncoder.matches(oldPassword, password)) {
            user.setPassword(passwordEncoder.encode(updatePasswordRequest.getNewPassword()));
            userService.save(user);
            return ResponseEntity.ok(new SuccessResponse(true, "Password changed successfully"));
        } else {
            return new ResponseEntity(new SuccessResponse(false, "Bad credentials"),
                    HttpStatus.NOT_ACCEPTABLE);
        }
    }

    @GetMapping("/switch-account")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public AuthResponse switchAccount(
            @RequestParam("id") Long id, @Parameter(hidden = true) @CurrentUser OwnUser user
    ) {
        if (!user.getSuperAccountRelations().isEmpty()) {//user is superUser
            SuperAccountRelation superAccountRelation =
                    superAccountRelationRepository.findBySuperUser_IdAndChildUser_Id(user.getId(), id);
            if (superAccountRelation == null) throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
            OwnUser childUser = userService.findById(id).get();
            if (!childUser.isEnabled()) throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
            return new AuthResponse(jwtTokenProvider.createToken(childUser.getEmail(),
                    Collections.singletonList(childUser.getRole().getRoleType())));
        } else if (user.getParentSuperAccount() != null) { //user is child
            SuperAccountRelation superAccountRelation =
                    superAccountRelationRepository.findBySuperUser_IdAndChildUser_Id(id, user.getId());
            if (superAccountRelation == null) throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
            OwnUser superUser = userService.findById(id).get();
            if (!superUser.isEnabled()) throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
            return new AuthResponse(jwtTokenProvider.createToken(superUser.getEmail(),
                    Collections.singletonList(superUser.getRole().getRoleType())));
        }
        throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
    }

    @DeleteMapping("")
    @PreAuthorize("permitAll()")
    public SuccessResponse deleteAccount(@Parameter(hidden = true) @CurrentUser OwnUser user) {
        if (user.isOwnsCompany())
            companyService.delete(user.getCompany().getId());
        else userRepository.delete(user);
        return new SuccessResponse(true, "Account deleted successfully");
    }

}


