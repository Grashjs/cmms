package com.grash.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "security.password")
public class PasswordPolicyProperties {
    private int minLength = 12;
    private int maxLength = 128;
    private boolean checkCommonPasswords = true;
}
