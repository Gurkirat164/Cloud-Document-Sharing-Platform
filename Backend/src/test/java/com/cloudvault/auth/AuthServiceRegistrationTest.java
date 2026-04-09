package com.cloudvault.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.cloudvault.auth.dto.AuthResponse;
import com.cloudvault.auth.dto.RegisterRequest;
import com.cloudvault.auth.repository.RefreshTokenRepository;
import com.cloudvault.auth.service.AuthService;
import com.cloudvault.common.exception.EmailAlreadyExistsException;
import com.cloudvault.domain.User;
import com.cloudvault.security.JwtTokenProvider;
import com.cloudvault.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceRegistrationTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshExpiryMs", 86400000L);
    }

    private RegisterRequest createValidRequest() {
        RegisterRequest req = new RegisterRequest();
        ReflectionTestUtils.setField(req, "fullName", "John Doe");
        ReflectionTestUtils.setField(req, "email", "john@example.com");
        ReflectionTestUtils.setField(req, "password", "StrongPass1!");
        ReflectionTestUtils.setField(req, "confirmPassword", "StrongPass1!");
        return req;
    }

    @Test
    void register_success_returnsAuthResponse() {
        RegisterRequest req = createValidRequest();

        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_pass");
        when(passwordEncoder.matches("StrongPass1!", "hashed_pass")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(any(User.class))).thenReturn("access_token");
        when(jwtTokenProvider.generateRefreshToken()).thenReturn("refresh_token");

        AuthResponse resp = authService.register(req);

        assertNotNull(resp);
        assertEquals("john@example.com", resp.getEmail());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_duplicateEmail_throwsEmailAlreadyExistsException() {
        RegisterRequest req = createValidRequest();
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> authService.register(req));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_commonPassword_throwsIllegalArgumentException() {
        RegisterRequest req = createValidRequest();
        ReflectionTestUtils.setField(req, "password", "password1"); // common password

        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> authService.register(req));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_passwordEqualsEmail_throwsIllegalArgumentException() {
        RegisterRequest req = createValidRequest();
        ReflectionTestUtils.setField(req, "password", "john@example.com");

        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> authService.register(req));
        verify(userRepository, never()).save(any(User.class));
    }

    // register_passwordsMismatch_caughtByValidation — this is caught by @PasswordsMatch at controller level, tested via RegisterRequestValidationTest
}
