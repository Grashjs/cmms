package com.grash.controller;

import com.grash.dto.license.LicenseEntitlement;
import com.grash.model.OwnUser;
import com.grash.model.enums.PlanFeatures;
import com.grash.security.JwtTokenProvider;
import com.grash.service.LicenseService;
import com.grash.service.UserService;
import com.grash.utils.Consts;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@Hidden
@RequestMapping("/swagger")
public class SwaggerAccessController {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private UserService userService;
    @Autowired
    private LicenseService licenseService;
    @Value("${frontend.url}")
    private String frontendUrl;

    @GetMapping("/swagger-session")
    public ResponseEntity<?> createSwaggerSession(
            @RequestHeader("Authorization") String authHeader,
            HttpServletResponse response) {

        // Verify the JWT from Authorization header
        String token = authHeader.replace(Consts.TOKEN_PREFIX, "");

        if (!jwtTokenProvider.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String username = jwtTokenProvider.getUsername(token);
        OwnUser user = userService.findByEmail(username).orElseThrow();

        // Check API access permission
        if (!licenseService.hasEntitlement(LicenseEntitlement.API_ACCESS) || user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().stream().noneMatch(planFeatures -> planFeatures.equals(PlanFeatures.API_ACCESS))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("API Access not available");
        }

        Cookie swaggerCookie = new Cookie("swagger_jwt", token);
        swaggerCookie.setHttpOnly(true);
        swaggerCookie.setSecure(true);
        swaggerCookie.setPath("/");
        swaggerCookie.setMaxAge(15 * 60); // 15 minutes

        response.addCookie(swaggerCookie);
        response.setHeader("Access-Control-Allow-Origin", frontendUrl);
        response.setHeader("Access-Control-Allow-Credentials", "true");

        return ResponseEntity.ok().body(Map.of("message", "Swagger session created"));
    }
}