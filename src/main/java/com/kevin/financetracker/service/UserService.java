package com.kevin.financetracker.service;

import com.kevin.financetracker.exception.DuplicateResourceException;
import com.kevin.financetracker.exception.ResourceNotFoundException;
import com.kevin.financetracker.model.User;
import com.kevin.financetracker.repository.UserRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Validator validator; // For manual validation

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, Validator validator) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.validator = validator;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(user.getRole().name())
                .accountExpired(!user.isAccountNonExpired())
                .accountLocked(!user.isAccountNonLocked())
                .credentialsExpired(!user.isCredentialsNonExpired())
                .disabled(!user.isEnabled())
                .build();
    }

    public User createUser(User user) {
        // Validate user entity
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException("Validation failed for user creation", violations);
        }

        // Check for duplicate username
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new DuplicateResourceException("Username already exists: " + user.getUsername());
        }

        // Check for duplicate email
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new DuplicateResourceException("Email already exists: " + user.getEmail());
        }

        // Encode password
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Set default role if not provided
        if (user.getRole() == null) {
            user.setRole(User.Role.USER);
        }

        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User updateUser(Long id, User userDetails) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Validate updated fields
        if (userDetails.getUsername() != null) {
            if (!userDetails.getUsername().equals(user.getUsername()) && userRepository.existsByUsername(userDetails.getUsername())) {
                throw new DuplicateResourceException("Username already exists: " + userDetails.getUsername());
            }
            user.setUsername(userDetails.getUsername());
        }
        if (userDetails.getEmail() != null && !userDetails.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(userDetails.getEmail())) {
                throw new DuplicateResourceException("Email already exists: " + userDetails.getEmail());
            }
            user.setEmail(userDetails.getEmail());
        }
        if (userDetails.getFirstName() != null) {
            user.setFirstName(userDetails.getFirstName());
        }
        if (userDetails.getLastName() != null) {
            user.setLastName(userDetails.getLastName());
        }
        if (userDetails.getRole() != null && !userDetails.getRole().equals(user.getRole())) {
            // Restrict role changes to admins
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(User.Role.ADMIN.name()))) {
                user.setRole(userDetails.getRole());
            } else {
                throw new SecurityException("Only admins can change user roles");
            }
        }

        // Validate updated user
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException("Validation failed for user update", violations);
        }

        return userRepository.save(user);
    }

    public void updatePassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public boolean verifyPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public boolean userExists(Long id) {
        return userRepository.existsById(id);
    }

    @Transactional(readOnly = true)
    public List<User> searchUsersByName(String name) {
        List<User> byFirstName = userRepository.findByFirstNameContainingIgnoreCase(name);
        List<User> byLastName = userRepository.findByLastNameContainingIgnoreCase(name);
        byFirstName.addAll(byLastName);
        return byFirstName.stream().distinct().toList();
    }

    @Transactional(readOnly = true)
    public boolean hasRole(Long userId, User.Role role) {
        return userRepository.findById(userId)
                .map(user -> user.getRole() == role)
                .orElse(false);
    }

    public User promoteToAdmin(Long userId) {
        // Restrict to admins
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(User.Role.ADMIN.name()))) {
            throw new SecurityException("Only admins can promote users to admin");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        user.setRole(User.Role.ADMIN);
        return userRepository.save(user);
    }
}