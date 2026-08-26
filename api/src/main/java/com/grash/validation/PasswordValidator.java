package com.grash.validation;

import com.grash.configuration.PasswordPolicyProperties;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    private final PasswordPolicyProperties policyProperties;
    private Set<String> commonPasswords = new HashSet<>();

    @PostConstruct
    void init() {
        if (policyProperties.isCheckCommonPasswords()) {
            commonPasswords = loadCommonPasswords();
        }
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.isEmpty()) {
            return true;
        }

        if (password.length() < policyProperties.getMinLength()) {
            setConstraintMessage(context,
                    "Password must be at least " + policyProperties.getMinLength() + " characters");
            return false;
        }

        if (password.length() > policyProperties.getMaxLength()) {
            setConstraintMessage(context,
                    "Password must not exceed " + policyProperties.getMaxLength() + " characters");
            return false;
        }

        if (policyProperties.isCheckCommonPasswords()
                && commonPasswords.contains(password.toLowerCase())) {
            setConstraintMessage(context, "Password is too common. Please choose a more unique password");
            return false;
        }

        if (isAllSameCharacter(password)) {
            setConstraintMessage(context, "Password must not consist of a single repeated character");
            return false;
        }

        return true;
    }

    private void setConstraintMessage(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }

    private boolean isAllSameCharacter(String password) {
        char first = password.charAt(0);
        for (int i = 1; i < password.length(); i++) {
            if (password.charAt(i) != first) {
                return false;
            }
        }
        return true;
    }

    Set<String> loadCommonPasswords() {
        Set<String> passwords = new HashSet<>();
        try {
            ClassPathResource resource = new ClassPathResource("common-passwords.txt");
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty()) {
                        passwords.add(trimmed.toLowerCase());
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return passwords;
    }
}
