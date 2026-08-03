package com.grash.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "oauth2")
public class OAuth2Properties {
    private String successRedirectUrl;
    private String failureRedirectUrl;
    private String provider;
    private String clientId;
    private String clientSecret;
}
