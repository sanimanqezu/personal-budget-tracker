package com.sanele.budgettracker.service;

import com.sanele.budgettracker.dto.AuthResponse;
import com.sanele.budgettracker.dto.LoginRequest;
import com.sanele.budgettracker.dto.RegisterRequest;
import com.sanele.budgettracker.entity.User;
import com.sanele.budgettracker.exception.ConflictException;
import com.sanele.budgettracker.repository.UserRepository;
import com.sanele.budgettracker.security.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("register — creates a user, hashes the password and returns a token")
    void register_newUser_returnsAuthResponse() {
        RegisterRequest request = new RegisterRequest("alice", "alice@example.com", "password123");
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(jwtUtil.generateToken("alice")).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.username()).isEqualTo("alice");
        assertThat(response.email()).isEqualTo("alice@example.com");
        assertThat(response.userId()).isEqualTo(1L);

        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(argThat(u -> "hashed".equals(u.getPasswordHash())));
    }

    @Test
    @DisplayName("register — throws ConflictException when the username already exists")
    void register_duplicateUsername_throwsConflict() {
        RegisterRequest request = new RegisterRequest("alice", "alice@example.com", "password123");
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("alice");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register — throws ConflictException when the email already exists")
    void register_duplicateEmail_throwsConflict() {
        RegisterRequest request = new RegisterRequest("alice", "alice@example.com", "password123");
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("alice@example.com");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("login — authenticates and returns a token for valid credentials")
    void login_validCredentials_returnsAuthResponse() {
        LoginRequest request = new LoginRequest("alice", "password123");
        Authentication authentication =
                new UsernamePasswordAuthenticationToken("alice", "password123");
        when(authenticationManager.authenticate(any())).thenReturn(authentication);

        User user = User.builder().id(7L).username("alice").email("alice@example.com")
                .passwordHash("hashed").build();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken("alice")).thenReturn("jwt-token");

        AuthResponse response = authService.login(request);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.userId()).isEqualTo(7L);
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("login — propagates BadCredentialsException from the authentication manager")
    void login_badCredentials_propagatesException() {
        LoginRequest request = new LoginRequest("alice", "wrong");
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);

        verify(jwtUtil, never()).generateToken(anyString());
    }
}
