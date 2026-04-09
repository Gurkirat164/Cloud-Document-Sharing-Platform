package com.cloudvault.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class EmailValidator implements ConstraintValidator<ValidEmail, String> {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return true;
        }

        String normalized = value.trim().toLowerCase();

        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            return false;
        }

        String localPart = normalized.substring(0, normalized.indexOf('@'));

        if (localPart.startsWith(".") || localPart.endsWith(".")) {
            return false;
        }

        if (localPart.contains("..")) {
            return false;
        }

        return true;
    }
}
