package com.cloudvault.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class PasswordStrengthValidator implements ConstraintValidator<PasswordStrength, String> {

    private static final Pattern UPPERCASE_PATTERN = Pattern.compile(".*[A-Z].*");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile(".*[a-z].*");
    private static final Pattern DIGIT_PATTERN = Pattern.compile(".*[0-9].*");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return true;
        }

        if (value.contains(" ")) {
            return false;
        }

        if (!UPPERCASE_PATTERN.matcher(value).matches()) {
            return false;
        }

        if (!LOWERCASE_PATTERN.matcher(value).matches()) {
            return false;
        }

        if (!DIGIT_PATTERN.matcher(value).matches()) {
            return false;
        }

        if (!SPECIAL_CHAR_PATTERN.matcher(value).matches()) {
            return false;
        }

        return true;
    }
}
