package com.cloudvault.common.validation;

import com.cloudvault.auth.dto.RegisterRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordsMatchValidator implements ConstraintValidator<PasswordsMatch, RegisterRequest> {

    @Override
    public boolean isValid(RegisterRequest request, ConstraintValidatorContext context) {
        if (request.getPassword() == null && request.getConfirmPassword() == null) {
            return true;
        }
        
        if (request.getPassword() != null && request.getPassword().equals(request.getConfirmPassword())) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
               .addPropertyNode("confirmPassword")
               .addConstraintViolation();

        return false;
    }
}
