package com.financetracker.service;

import com.financetracker.dto.AuthResponse;
import com.financetracker.dto.LoginRequest;
import com.financetracker.dto.MessageResponse;
import com.financetracker.dto.RegisterRequest;
import com.financetracker.entity.User;
import com.financetracker.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ============================================
    // REGISTER
    // ============================================
    public MessageResponse register(RegisterRequest request) {

        // 1. Check if username is taken
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username is already taken");
        }

        // 2. Check if email is taken
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already in use");
        }

        // 3. Hash the password (NEVER store plain text!)
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        // 4. Create the User entity (username, email, password)
        User user = new User(
                request.getUsername(),
                request.getEmail(),
                hashedPassword
        );

        // 5. Save to the database
        userRepository.save(user);

        // 6. Return a friendly message
        return new MessageResponse("User registered successfully");
    }

    // ============================================
    // LOGIN
    // ============================================
    public AuthResponse login(LoginRequest request) {

        // 1. Find the user by username
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Invalid username or password"));

        // 2. Check that the submitted password matches the stored hash
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }

        // 3. TODO: Generate a real JWT (next phase)
        String placeholderToken = "PLACEHOLDER_TOKEN_" + user.getId();

        return new AuthResponse(
                placeholderToken,
                user.getUsername(),
                "Login successful"
        );
    }
}