package com.kevin.financetracker.service;

import com.kevin.financetracker.exception.DuplicateResourceException;
import com.kevin.financetracker.exception.ResourceNotFoundException;
import com.kevin.financetracker.model.User;
import com.kevin.financetracker.repository.UserRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private Validator validator;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@gmail.com");
        testUser.setPassword("password123");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setRole(User.Role.USER);
        testUser.setEnabled(true);
        testUser.setAccountNonExpired(true);
        testUser.setAccountNonLocked(true);
        testUser.setCredentialsNonExpired(true);

        adminUser = new User();
        adminUser.setId(2L);
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@gmail.com");
        adminUser.setPassword("admin123");
        adminUser.setFirstName("Admin");
        adminUser.setLastName("User");
        adminUser.setRole(User.Role.ADMIN);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // Helper methods for authentication setup
    private void setupAuthentication(String role) {
        Collection<GrantedAuthority> authorities = Collections.singleton(new SimpleGrantedAuthority(role));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.getAuthorities()).thenReturn((Collection) authorities);
    }

    private void setupNoAuthentication() {
        when(securityContext.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(securityContext);
    }

    // Tests that don't involve authentication at all
    @Test
    @DisplayName("Load user by username - Success")
    void loadUserByUsername_Success() {
        // Arrange
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = userService.loadUserByUsername("test@gmail.com");

        // Assert
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("test@gmail.com");
        assertThat(userDetails.getPassword()).isEqualTo("password123");
        assertThat(userDetails.getAuthorities()).hasSize(1);
        assertThat(userDetails.getAuthorities().iterator().next().getAuthority()).isEqualTo("USER");
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();

        verify(userRepository).findByEmail("test@gmail.com");
    }

    @Test
    @DisplayName("Load user by username - User not found")
    void loadUserByUsername_UserNotFound() {
        // Arrange
        when(userRepository.findByEmail("nonexistent@gmail.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.loadUserByUsername("nonexistent@gmail.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found with email: nonexistent@gmail.com");

        verify(userRepository).findByEmail("nonexistent@gmail.com");
    }

    @Test
    @DisplayName("Create user - Success")
    void createUser_Success() {
        // Arrange
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setEmail("new@gmail.com");
        newUser.setPassword("plainpassword");
        newUser.setFirstName("New");
        newUser.setLastName("User");

        when(validator.validate(newUser)).thenReturn(Collections.emptySet());
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@gmail.com")).thenReturn(false);
        when(passwordEncoder.encode("plainpassword")).thenReturn("encodedpassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(3L);
            return user;
        });

        // Act
        User createdUser = userService.createUser(newUser);

        // Assert
        assertThat(createdUser).isNotNull();
        assertThat(createdUser.getId()).isEqualTo(3L);
        assertThat(createdUser.getPassword()).isEqualTo("encodedpassword");
        assertThat(createdUser.getRole()).isEqualTo(User.Role.USER);

        verify(validator).validate(newUser);
        verify(userRepository).existsByUsername("newuser");
        verify(userRepository).existsByEmail("new@gmail.com");
        verify(passwordEncoder).encode("plainpassword");
        verify(userRepository).save(newUser);
    }

    @Test
    @DisplayName("Create user - Validation fails")
    void createUser_ValidationFails() {
        // Arrange
        User invalidUser = new User();
        invalidUser.setUsername("ab"); // Too short
        invalidUser.setEmail("invalid-email");

        Set<ConstraintViolation<User>> violations = new HashSet<>();
        ConstraintViolation<User> violation1 = mock(ConstraintViolation.class);
        ConstraintViolation<User> violation2 = mock(ConstraintViolation.class);
        violations.add(violation1);
        violations.add(violation2);

        when(validator.validate(invalidUser)).thenReturn(violations);

        // Act & Assert
        assertThatThrownBy(() -> userService.createUser(invalidUser))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessage("Validation failed for user creation");

        verify(validator).validate(invalidUser);
        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Create user - Duplicate username")
    void createUser_DuplicateUsername() {
        // Arrange
        User duplicateUser = new User();
        duplicateUser.setUsername("existinguser");
        duplicateUser.setEmail("new@gmail.com");

        when(validator.validate(duplicateUser)).thenReturn(Collections.emptySet());
        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> userService.createUser(duplicateUser))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Username already exists: existinguser");

        verify(validator).validate(duplicateUser);
        verify(userRepository).existsByUsername("existinguser");
        verify(userRepository, never()).existsByEmail(anyString());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("Create user - Duplicate email")
    void createUser_DuplicateEmail() {
        // Arrange
        User duplicateUser = new User();
        duplicateUser.setUsername("newuser");
        duplicateUser.setEmail("existing@gmail.com");

        when(validator.validate(duplicateUser)).thenReturn(Collections.emptySet());
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("existing@gmail.com")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> userService.createUser(duplicateUser))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Email already exists: existing@gmail.com");

        verify(validator).validate(duplicateUser);
        verify(userRepository).existsByUsername("newuser");
        verify(userRepository).existsByEmail("existing@gmail.com");
        verifyNoInteractions(passwordEncoder);
    }

    // Tests that involve update operations without role changes
    @Test
    @DisplayName("Update user - Success")
    void updateUser_Success() {
        // Arrange
        User updatedDetails = new User();
        updatedDetails.setUsername("updateduser");
        updatedDetails.setFirstName("Updated");
        updatedDetails.setLastName("Name");
        updatedDetails.setRole(User.Role.USER); // Explicitly set role to match testUser

        // No authentication setup - these updates don't require admin
        setupNoAuthentication();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByUsername("updateduser")).thenReturn(false);
        when(validator.validate(any(User.class))).thenReturn(Collections.emptySet());
        when(userRepository.save(testUser)).thenReturn(testUser);

        // Act
        User result = userService.updateUser(1L, updatedDetails);

        // Assert
        assertThat(result).isEqualTo(testUser);
        assertThat(testUser.getUsername()).isEqualTo("updateduser");
        assertThat(testUser.getFirstName()).isEqualTo("Updated");
        assertThat(testUser.getLastName()).isEqualTo("Name");

        verify(userRepository).findById(1L);
        verify(userRepository).existsByUsername("updateduser");
        verify(validator).validate(testUser);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Update user - User not found")
    void updateUser_UserNotFound() {
        // Arrange
        User updatedDetails = new User();
        updatedDetails.setUsername("updateduser");

        // No authentication needed for this failure case
        setupNoAuthentication();

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.updateUser(999L, updatedDetails))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found with id: 999");

        verify(userRepository).findById(999L);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update user - Duplicate username")
    void updateUser_DuplicateUsername() {
        // Arrange
        User updatedDetails = new User();
        updatedDetails.setUsername("existinguser");

        // No authentication needed for this failure case
        setupNoAuthentication();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> userService.updateUser(1L, updatedDetails))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Username already exists: existinguser");

        verify(userRepository).findById(1L);
        verify(userRepository).existsByUsername("existinguser");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update user - Duplicate email")
    void updateUser_DuplicateEmail() {
        // Arrange
        User updatedDetails = new User();
        updatedDetails.setEmail("existing@gmail.com");

        // No authentication needed for this failure case
        setupNoAuthentication();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail("existing@gmail.com")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> userService.updateUser(1L, updatedDetails))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Email already exists: existing@gmail.com");

        verify(userRepository).findById(1L);
        verify(userRepository).existsByEmail("existing@gmail.com");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update user with same email - Should not check for duplicates")
    void updateUser_SameEmail_ShouldNotCheckDuplicates() {
        // Arrange
        User updatedDetails = new User();
        updatedDetails.setEmail("test@gmail.com"); // Same as current email
        updatedDetails.setFirstName("UpdatedFirstName");
        updatedDetails.setRole(User.Role.USER); // Explicitly set role to match testUser

        // No authentication needed - not changing role
        setupNoAuthentication();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(validator.validate(any(User.class))).thenReturn(Collections.emptySet());
        when(userRepository.save(testUser)).thenReturn(testUser);

        // Act
        User result = userService.updateUser(1L, updatedDetails);

        // Assert
        assertThat(result).isEqualTo(testUser);
        assertThat(testUser.getFirstName()).isEqualTo("UpdatedFirstName");
        verify(userRepository, never()).existsByEmail(anyString()); // Should not check for duplicates
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Update user - Validation fails after update")
    void updateUser_ValidationFailsAfterUpdate() {
        // Arrange
        User updatedDetails = new User();
        updatedDetails.setUsername("updateduser");
        updatedDetails.setRole(User.Role.USER); // Explicitly set role to match testUser

        // No authentication needed - not changing role
        setupNoAuthentication();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByUsername("updateduser")).thenReturn(false);

        Set<ConstraintViolation<User>> violations = new HashSet<>();
        ConstraintViolation<User> violation = mock(ConstraintViolation.class);
        violations.add(violation);
        when(validator.validate(any(User.class))).thenReturn(violations);

        // Act & Assert
        assertThatThrownBy(() -> userService.updateUser(1L, updatedDetails))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessage("Validation failed for user update");

        verify(userRepository).findById(1L);
        verify(validator).validate(any(User.class));
        verify(userRepository, never()).save(any());
    }

    // Tests that specifically involve role changes and require authentication
    @Test
    @DisplayName("Update user role - Admin user")
    void updateUserRole_AdminUser() {
        // Arrange
        User updatedDetails = new User();
        updatedDetails.setRole(User.Role.ADMIN);

        setupAuthentication("ADMIN");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(validator.validate(any(User.class))).thenReturn(Collections.emptySet());
        when(userRepository.save(testUser)).thenReturn(testUser);

        // Act
        User result = userService.updateUser(1L, updatedDetails);

        // Assert
        assertThat(result).isEqualTo(testUser);
        assertThat(testUser.getRole()).isEqualTo(User.Role.ADMIN);

        verify(authentication).getAuthorities();
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Update user role - Non-admin user")
    void updateUserRole_NonAdminUser() {
        // Arrange
        User updatedDetails = new User();
        updatedDetails.setRole(User.Role.ADMIN);

        setupAuthentication("USER");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThatThrownBy(() -> userService.updateUser(1L, updatedDetails))
                .isInstanceOf(SecurityException.class)
                .hasMessage("Only admins can change user roles");

        verify(authentication).getAuthorities();
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update user role - No authentication")
    void updateUserRole_NoAuthentication() {
        // Arrange
        User updatedDetails = new User();
        updatedDetails.setRole(User.Role.ADMIN);

        setupNoAuthentication();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThatThrownBy(() -> userService.updateUser(1L, updatedDetails))
                .isInstanceOf(SecurityException.class)
                .hasMessage("Only admins can change user roles");

        verify(userRepository, never()).save(any());
    }

    // Other tests that don't involve authentication
    @Test
    @DisplayName("Get user by ID - Success")
    void getUserById_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        Optional<User> result = userService.getUserById(1L);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testUser);
        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("Get user by ID - Not found")
    void getUserById_NotFound() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        Optional<User> result = userService.getUserById(999L);

        // Assert
        assertThat(result).isEmpty();
        verify(userRepository).findById(999L);
    }

    @Test
    @DisplayName("Get user by username - Success")
    void getUserByUsername_Success() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        Optional<User> result = userService.getUserByUsername("testuser");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testUser);
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    @DisplayName("Get user by email - Success")
    void getUserByEmail_Success() {
        // Arrange
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(testUser));

        // Act
        Optional<User> result = userService.getUserByEmail("test@gmail.com");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testUser);
        verify(userRepository).findByEmail("test@gmail.com");
    }

    @Test
    @DisplayName("Get all users - Success")
    void getAllUsers_Success() {
        // Arrange
        List<User> users = Arrays.asList(testUser, adminUser);
        when(userRepository.findAll()).thenReturn(users);

        // Act
        List<User> result = userService.getAllUsers();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(testUser, adminUser);
        verify(userRepository).findAll();
    }

    @Test
    @DisplayName("Update password - Success")
    void updatePassword_Success() {
        // Arrange
        String newPassword = "newpassword123";
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(newPassword)).thenReturn("encodednewpassword");
        when(userRepository.save(testUser)).thenReturn(testUser);

        // Act
        userService.updatePassword(1L, newPassword);

        // Assert
        assertThat(testUser.getPassword()).isEqualTo("encodednewpassword");
        verify(userRepository).findById(1L);
        verify(passwordEncoder).encode(newPassword);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Update password - User not found")
    void updatePassword_UserNotFound() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.updatePassword(999L, "newpassword"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found with id: 999");

        verify(userRepository).findById(999L);
        verifyNoInteractions(passwordEncoder);
    }

    @ParameterizedTest
    @MethodSource("invalidPasswordProvider")
    @DisplayName("Update password - Invalid password")
    void updatePassword_InvalidPassword(String password, String expectedMessage) {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThatThrownBy(() -> userService.updatePassword(1L, password))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage);

        verify(userRepository).findById(1L);
        verifyNoMoreInteractions(passwordEncoder);
    }

    private static Stream<Arguments> invalidPasswordProvider() {
        return Stream.of(
                Arguments.of(null, "Password must be at least 8 characters long"),
                Arguments.of("short", "Password must be at least 8 characters long"),
                Arguments.of("", "Password must be at least 8 characters long")
        );
    }

    @Test
    @DisplayName("Verify password - Success")
    void verifyPassword_Success() {
        // Arrange
        String rawPassword = "password123";
        String encodedPassword = "encodedpassword";
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true);

        // Act
        boolean result = userService.verifyPassword(rawPassword, encodedPassword);

        // Assert
        assertThat(result).isTrue();
        verify(passwordEncoder).matches(rawPassword, encodedPassword);
    }

    @Test
    @DisplayName("Delete user - Success")
    void deleteUser_Success() {
        // Arrange
        when(userRepository.existsById(1L)).thenReturn(true);

        // Act
        userService.deleteUser(1L);

        // Assert
        verify(userRepository).existsById(1L);
        verify(userRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Delete user - User not found")
    void deleteUser_UserNotFound() {
        // Arrange
        when(userRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> userService.deleteUser(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found with id: 999");

        verify(userRepository).existsById(999L);
        verify(userRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("User exists - True")
    void userExists_True() {
        // Arrange
        when(userRepository.existsById(1L)).thenReturn(true);

        // Act
        boolean result = userService.userExists(1L);

        // Assert
        assertThat(result).isTrue();
        verify(userRepository).existsById(1L);
    }

    @Test
    @DisplayName("User exists - False")
    void userExists_False() {
        // Arrange
        when(userRepository.existsById(999L)).thenReturn(false);

        // Act
        boolean result = userService.userExists(999L);

        // Assert
        assertThat(result).isFalse();
        verify(userRepository).existsById(999L);
    }

    @Test
    @DisplayName("Search users by name - Success")
    void searchUsersByName_Success() {
        // Arrange
        List<User> firstNameMatches = new ArrayList<>(Collections.singletonList(testUser));
        List<User> lastNameMatches = new ArrayList<>(Collections.singletonList(adminUser));

        when(userRepository.findByFirstNameContainingIgnoreCase("test")).thenReturn(firstNameMatches);
        when(userRepository.findByLastNameContainingIgnoreCase("test")).thenReturn(lastNameMatches);

        // Act
        List<User> result = userService.searchUsersByName("test");

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder(testUser, adminUser);

        verify(userRepository).findByFirstNameContainingIgnoreCase("test");
        verify(userRepository).findByLastNameContainingIgnoreCase("test");
    }

    @Test
    @DisplayName("Search users by name - No duplicates")
    void searchUsersByName_NoDuplicates() {
        // Arrange
        List<User> firstNameMatches = new ArrayList<>(Collections.singletonList(testUser));
        List<User> lastNameMatches = new ArrayList<>(Collections.singletonList(testUser)); // Same user in both lists

        when(userRepository.findByFirstNameContainingIgnoreCase("test")).thenReturn(firstNameMatches);
        when(userRepository.findByLastNameContainingIgnoreCase("test")).thenReturn(lastNameMatches);

        // Act
        List<User> result = userService.searchUsersByName("test");

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(testUser);
    }

    @Test
    @DisplayName("Has role - True")
    void hasRole_True() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        boolean result = userService.hasRole(1L, User.Role.USER);

        // Assert
        assertThat(result).isTrue();
        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("Has role - False")
    void hasRole_False() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        boolean result = userService.hasRole(1L, User.Role.ADMIN);

        // Assert
        assertThat(result).isFalse();
        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("Has role - User not found")
    void hasRole_UserNotFound() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        boolean result = userService.hasRole(999L, User.Role.USER);

        // Assert
        assertThat(result).isFalse();
        verify(userRepository).findById(999L);
    }

    @Test
    @DisplayName("Promote to admin - Success")
    void promoteToAdmin_Success() {
        // Arrange
        setupAuthentication("ADMIN");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);

        // Act
        User result = userService.promoteToAdmin(1L);

        // Assert
        assertThat(result).isEqualTo(testUser);
        assertThat(testUser.getRole()).isEqualTo(User.Role.ADMIN);

        verify(authentication).getAuthorities();
        verify(userRepository).findById(1L);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Promote to admin - Non-admin user")
    void promoteToAdmin_NonAdminUser() {
        // Arrange
        setupAuthentication("USER");

        // Act & Assert
        assertThatThrownBy(() -> userService.promoteToAdmin(1L))
                .isInstanceOf(SecurityException.class)
                .hasMessage("Only admins can promote users to admin");

        verify(authentication).getAuthorities();
        verify(userRepository, never()).findById(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Promote to admin - User not found")
    void promoteToAdmin_UserNotFound() {
        // Arrange
        setupAuthentication("ADMIN");

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.promoteToAdmin(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found with id: 999");

        verify(authentication).getAuthorities();
        verify(userRepository).findById(999L);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create user with existing role")
    void createUser_WithExistingRole() {
        // Arrange
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setEmail("new@gmail.com");
        newUser.setPassword("plainpassword");
        newUser.setFirstName("New");
        newUser.setLastName("User");
        newUser.setRole(User.Role.MODERATOR);

        when(validator.validate(newUser)).thenReturn(Collections.emptySet());
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@gmail.com")).thenReturn(false);
        when(passwordEncoder.encode("plainpassword")).thenReturn("encodedpassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(3L);
            return user;
        });

        // Act
        User createdUser = userService.createUser(newUser);

        // Assert
        assertThat(createdUser).isNotNull();
        assertThat(createdUser.getRole()).isEqualTo(User.Role.MODERATOR); // Should preserve the provided role

        verify(validator).validate(newUser);
        verify(userRepository).save(newUser);
    }

    @Test
    @DisplayName("Load user by username - Disabled user")
    void loadUserByUsername_DisabledUser() {
        // Arrange
        testUser.setEnabled(false);
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = userService.loadUserByUsername("test@gmail.com");

        // Assert
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.isEnabled()).isFalse();
        verify(userRepository).findByEmail("test@gmail.com");
    }

    @Test
    @DisplayName("Load user by username - Locked account")
    void loadUserByUsername_LockedAccount() {
        // Arrange
        testUser.setAccountNonLocked(false);
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = userService.loadUserByUsername("test@gmail.com");

        // Assert
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.isAccountNonLocked()).isFalse();
        verify(userRepository).findByEmail("test@gmail.com");
    }

    @Test
    @DisplayName("Search users by name - Empty results")
    void searchUsersByName_EmptyResults() {
        // Arrange
        when(userRepository.findByFirstNameContainingIgnoreCase("nonexistent")).thenReturn(Collections.emptyList());
        when(userRepository.findByLastNameContainingIgnoreCase("nonexistent")).thenReturn(Collections.emptyList());

        // Act
        List<User> result = userService.searchUsersByName("nonexistent");

        // Assert
        assertThat(result).isEmpty();

        verify(userRepository).findByFirstNameContainingIgnoreCase("nonexistent");
        verify(userRepository).findByLastNameContainingIgnoreCase("nonexistent");
    }

    @Test
    @DisplayName("Verify password - Failure")
    void verifyPassword_Failure() {
        // Arrange
        String rawPassword = "password123";
        String encodedPassword = "encodedpassword";
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(false);

        // Act
        boolean result = userService.verifyPassword(rawPassword, encodedPassword);

        // Assert
        assertThat(result).isFalse();
        verify(passwordEncoder).matches(rawPassword, encodedPassword);
    }
}