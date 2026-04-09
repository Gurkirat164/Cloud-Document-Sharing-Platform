package com.cloudvault.auth;

import static org.junit.jupiter.api.Assertions.*;

import com.cloudvault.auth.dto.RegisterRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

class RegisterRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    private RegisterRequest createValidRequest() {
        RegisterRequest req = new RegisterRequest();
        ReflectionTestUtils.setField(req, "fullName", "John Doe");
        ReflectionTestUtils.setField(req, "email", "john@example.com");
        ReflectionTestUtils.setField(req, "password", "StrongP@ss1");
        ReflectionTestUtils.setField(req, "confirmPassword", "StrongP@ss1");
        return req;
    }

    @Test
    void validRequest_noViolations() {
        RegisterRequest req = createValidRequest();
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(req);
        assertTrue(violations.isEmpty());
    }

    @Test
    void blankEmail_hasViolation() {
        RegisterRequest req = createValidRequest();
        ReflectionTestUtils.setField(req, "email", "");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
    }

    @Test
    void invalidEmailFormat_hasViolation() {
        RegisterRequest req = createValidRequest();
        ReflectionTestUtils.setField(req, "email", "invalid-email");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
    }

    @Test
    void shortPassword_hasViolation() {
        RegisterRequest req = createValidRequest();
        ReflectionTestUtils.setField(req, "password", "S@1p");
        ReflectionTestUtils.setField(req, "confirmPassword", "S@1p");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password")));
    }

    @Test
    void weakPassword_noUppercase_hasViolation() {
        RegisterRequest req = createValidRequest();
        ReflectionTestUtils.setField(req, "password", "strongp@ss1");
        ReflectionTestUtils.setField(req, "confirmPassword", "strongp@ss1");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password")));
    }

    @Test
    void weakPassword_noSpecialChar_hasViolation() {
        RegisterRequest req = createValidRequest();
        ReflectionTestUtils.setField(req, "password", "StrongPass1");
        ReflectionTestUtils.setField(req, "confirmPassword", "StrongPass1");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password")));
    }

    @Test
    void passwordsDoNotMatch_hasViolation() {
        RegisterRequest req = createValidRequest();
        ReflectionTestUtils.setField(req, "confirmPassword", "DifferentP@ss1");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("confirmPassword")));
    }

    @Test
    void fullNameWithNumbers_hasViolation() {
        RegisterRequest req = createValidRequest();
        ReflectionTestUtils.setField(req, "fullName", "John Doe 123");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("fullName")));
    }
}
