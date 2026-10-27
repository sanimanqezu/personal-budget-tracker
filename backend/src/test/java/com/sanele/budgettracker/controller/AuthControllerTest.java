package com.sanele.budgettracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanele.budgettracker.dto.AuthResponse;
import com.sanele.budgettracker.dto.LoginRequest;
import com.sanele.budgettracker.dto.RegisterRequest;
import com.sanele.budgettracker.exception.ConflictException;
import com.sanele.budgettracker.security.JwtAuthFilter;
import com.sanele.budgettracker.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AuthController Slice Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @Test
    @DisplayName("POST /api/auth/register — returns 201 with the auth response")
    void register_valid_returns201() throws Exception {
        RegisterRequest request = new RegisterRequest("alice", "alice@example.com", "password123");
        when(authService.register(any())).thenReturn(
                new AuthResponse("jwt-token", "alice", "alice@example.com", 1L));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    @DisplayName("POST /api/auth/register — returns 400 when the body fails validation")
    void register_invalid_returns400() throws Exception {
        RegisterRequest request = new RegisterRequest("", "not-an-email", "123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors").isNotEmpty());

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("POST /api/auth/register — maps ConflictException to 409")
    void register_duplicate_returns409() throws Exception {
        RegisterRequest request = new RegisterRequest("alice", "alice@example.com", "password123");
        when(authService.register(any())).thenThrow(new ConflictException("Username 'alice' is already taken"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Username")));
    }

    @Test
    @DisplayName("POST /api/auth/login — returns 200 with a token for valid credentials")
    void login_valid_returns200() throws Exception {
        LoginRequest request = new LoginRequest("alice", "password123");
        when(authService.login(any())).thenReturn(
                new AuthResponse("jwt-token", "alice", "alice@example.com", 1L));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    @DisplayName("POST /api/auth/login — maps BadCredentialsException to 401")
    void login_badCredentials_returns401() throws Exception {
        LoginRequest request = new LoginRequest("alice", "wrong");
        when(authService.login(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }
}
