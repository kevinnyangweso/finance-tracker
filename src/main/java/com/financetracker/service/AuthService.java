package com.financetracker.service;

import com.financetracker.dto.AuthResponse;
import com.financetracker.dto.LoginRequest;
import com.financetracker.dto.MessageResponse;
import com.financetracker.dto.RegisterRequest;
import com.financetracker.entity.User;
import com.financetracker.exception.EmailAlreadyExistsException;
import com.financetracker.exception.InvalidCredentialsException;
import com.financetracker.exception.UsernameAlreadyExistsException;
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
            throw new UsernameAlreadyExistsException("Username is already taken");
        }

        // 2. Check if email is taken
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email is already in use");
        }

        // 3. Hash the password
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        // 4. Create the User entity
        User user = new User(
                request.getUsername(),
                request.getEmail(),
                hashedPassword
        );

        // 5. Save
        userRepository.save(user);

        // 6. Return
        return new MessageResponse("User registered successfully");
    }

    // ============================================
    // LOGIN
    // ============================================
    public AuthResponse login(LoginRequest request) {

        // 1. Find the user
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));

        // 2. Check the password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        // 3. Placeholder token (we'll replace with real JWT in Phase 6)
        String placeholderToken = "PLACEHOLDER_TOKEN_" + user.getId();

        return new AuthResponse(
                placeholderToken,
                user.getUsername(),
                "Login successful"
        );
    }
}