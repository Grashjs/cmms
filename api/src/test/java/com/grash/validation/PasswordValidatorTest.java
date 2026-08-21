package com.grash.validation;

import com.grash.configuration.PasswordPolicyProperties;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PasswordValidatorTest {

    private PasswordValidator validator;
    private PasswordPolicyProperties properties;
    private ConstraintValidatorContext context;
    private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

    @BeforeEach
    void setUp() {
        properties = new PasswordPolicyProperties();
        properties.setMinLength(12);
        properties.setMaxLength(128);
        properties.setCheckCommonPasswords(true);
        validator = new PasswordValidator(properties);
        validator.init();
        context = mock(ConstraintValidatorContext.class);
        violationBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
    }

    @Nested
    class MinLength {
        @Test
        void rejectsPasswordShorterThanMinLength() {
            assertFalse(validator.isValid("Ab1!2345", context));
        }

        @Test
        void rejectsPasswordAtBoundary() {
            assertFalse(validator.isValid("Ab1!234567", context));
        }

        @Test
        void acceptsPasswordAtMinLength() {
            assertTrue(validator.isValid("Ab1!23456789", context));
        }

        @Test
        void acceptsLongerPassword() {
            assertTrue(validator.isValid("MyVerySecureP@ssw0rd!", context));
        }
    }

    @Nested
    class MaxLength {
        @Test
        void rejectsOversizedPassword() {
            String longPassword = "A".repeat(129);
            assertFalse(validator.isValid(longPassword, context));
        }

        @Test
        void acceptsMaxLengthPassword() {
            String maxPassword = "A".repeat(127) + "1";
            assertTrue(validator.isValid(maxPassword, context));
        }
    }

    @Nested
    class NullAndEmpty {
        @Test
        void acceptsNullPassword() {
            assertTrue(validator.isValid(null, context));
        }

        @Test
        void acceptsEmptyPassword() {
            assertTrue(validator.isValid("", context));
        }
    }

    @Nested
    class CommonPasswords {
        @Test
        void rejectsPasswordFromCommonList() {
            assertFalse(validator.isValid("1qaz2wsx3edc", context));
        }

        @Test
        void rejectsCommonNumericPattern() {
            assertFalse(validator.isValid("123456654321", context));
        }

        @Test
        void rejectsCommonPasswordCaseInsensitive() {
            assertFalse(validator.isValid("QwertyQwerty", context));
        }

        @Test
        void rejectsCommonKeyboardPattern() {
            assertFalse(validator.isValid("qwerasdfzxcv", context));
        }

        @Test
        void rejectsCommonPasswordOf12Chars() {
            assertFalse(validator.isValid("motherfucker", context));
        }

        @Test
        void acceptsUniquePassword() {
            assertTrue(validator.isValid("X#9mK$pL2vQn!", context));
        }

        @Test
        void acceptsPasswordNotInList() {
            assertTrue(validator.isValid("zT7!kR9$mN2pQ", context));
        }
    }

    @Nested
    class RepeatedCharacters {
        @Test
        void rejectsAllSameCharacter() {
            assertFalse(validator.isValid("aaaaaaaaaaaa", context));
        }

        @Test
        void rejectsAllSameDigit() {
            assertFalse(validator.isValid("111111111111", context));
        }

        @Test
        void acceptsPasswordWithSomeRepeatedChars() {
            assertTrue(validator.isValid("aabbaabbaabb", context));
        }

        @Test
        void acceptsNoRepeatedChars() {
            assertTrue(validator.isValid("zyxwvu654321", context));
        }
    }

    @Nested
    class CommonPasswordsLoading {
        @Test
        void loadsPasswordsFromFile() {
            PasswordPolicyProperties props = new PasswordPolicyProperties();
            props.setCheckCommonPasswords(true);
            PasswordValidator v = new PasswordValidator(props);
            v.init();
            Set<String> loaded = v.loadCommonPasswords();
            assertFalse(loaded.isEmpty());
            assertTrue(loaded.contains("motherfucker"));
            assertTrue(loaded.contains("leavemealone"));
        }

        @Test
        void validatorSkipsCommonCheckWhenDisabled() {
            PasswordPolicyProperties props = new PasswordPolicyProperties();
            props.setCheckCommonPasswords(false);
            PasswordValidator v = new PasswordValidator(props);
            v.init();
            assertTrue(v.isValid("motherfucker", context));
        }
    }
}
