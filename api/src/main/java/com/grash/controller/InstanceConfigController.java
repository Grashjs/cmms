package com.grash.controller;


import com.grash.dto.InstanceConfig;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/instance-config")
@RequiredArgsConstructor
@Hidden
public class InstanceConfigController {

    @Value("${ldap.enabled:false}")
    private boolean ldapEnabled;

    @GetMapping("")
    public InstanceConfig getInstanceConfig(HttpServletRequest req) {
        return new InstanceConfig(ldapEnabled);
    }
}


