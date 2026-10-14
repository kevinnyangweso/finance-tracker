package com.financetracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financetracker.dto.LoginRequest;
import com.financetracker.dto.RegisterRequest;
import com.financetracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    // ============================================
    // REGISTER — Happy path → 201 Created
    // ============================================
    @Test
    void register_shouldReturn201_whenDataIsValid() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("kevin");
        req.setEmail("kevin@example.com");
        req.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("User registered successfully"));
    }

    // ============================================
    // REGISTER — Duplicate → 409 Conflict
    // ============================================
    @Test
    void register_shouldReturn409_whenUsernameIsTaken() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("kevin");
        req.setEmail("kevin@example.com");
        req.setPassword("password123");

        // First registration — should succeed
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        // Second registration with the same username — should conflict
        req.setEmail("different@example.com");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username is already taken"));
    }

    // ============================================
    // REGISTER — Invalid email → 400 Bad Request
    // ============================================
    @Test
    void register_shouldReturn400_whenEmailIsInvalid() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("kevin");
        req.setEmail("not-an-email");
        req.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    // ============================================
    // LOGIN — Happy path → 200 with JWT
    // ============================================
    @Test
    void login_shouldReturnToken_whenCredentialsAreValid() throws Exception {
        // Register first
        RegisterRequest reg = new RegisterRequest();
        reg.setUsername("kevin");
        reg.setEmail("kevin@example.com");
        reg.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        // Then log in
        LoginRequest login = new LoginRequest();
        login.setUsername("kevin");
        login.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.token").value(containsString(".")))
                .andExpect(jsonPath("$.username").value("kevin"))
                .andExpect(jsonPath("$.type").value("Bearer"));
    }

    // ============================================
    // LOGIN — Wrong password → 401 Unauthorized
    // ============================================
    @Test
    void login_shouldReturn401_whenPasswordIsWrong() throws Exception {
        RegisterRequest reg = new RegisterRequest();
        reg.setUsername("kevin");
        reg.setEmail("kevin@example.com");
        reg.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        LoginRequest badLogin = new LoginRequest();
        badLogin.setUsername("kevin");
        badLogin.setPassword("wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badLogin)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }
}