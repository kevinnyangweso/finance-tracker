package com.kevin.financetracker.controller;

import com.kevin.financetracker.exception.ResourceNotFoundException;
import com.kevin.financetracker.model.User;
import com.kevin.financetracker.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private User testUser;
    private User anotherUser;
    private Validator validator;

    private final Long userId = 1L;
    private final Long nonExistentUserId = 999L;
    private final String username = "testuser";
    private final String email = "test@gmail.com";
    private final String nonExistentUsername = "nonexistent";
    private final String nonExistentEmail = "nonexistent@gmail.com";

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(userId);
        testUser.setUsername(username);
        testUser.setEmail(email);
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setPassword("Test123!");

        anotherUser = new User();
        anotherUser.setId(2L);
        anotherUser.setUsername("anotheruser");
        anotherUser.setEmail("another@gmail.com");
        anotherUser.setFirstName("Jane");
        anotherUser.setLastName("Smith");
        anotherUser.setPassword("Test456!");

        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // CREATE USER TESTS
    @Test
    void createUser_WithValidUser_ShouldReturnCreatedUserWithHttpStatusCreated() {
        when(userService.createUser(any(User.class))).thenReturn(testUser);

        ResponseEntity<User> response = userController.createUser(testUser);

        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testUser.getId(), response.getBody().getId());
        assertEquals(testUser.getUsername(), response.getBody().getUsername());
        assertEquals(testUser.getEmail(), response.getBody().getEmail());
        verify(userService, times(1)).createUser(any(User.class));
    }

    // GET ALL USERS TESTS
    @Test
    void getAllUsers_WhenUsersExist_ShouldReturnListOfUsersWithHttpStatusOk() {
        List<User> users = Arrays.asList(testUser, anotherUser);
        when(userService.getAllUsers()).thenReturn(users);

        ResponseEntity<List<User>> response = userController.getAllUsers();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertTrue(response.getBody().contains(testUser));
        assertTrue(response.getBody().contains(anotherUser));
        verify(userService, times(1)).getAllUsers();
    }

    @Test
    void getAllUsers_WhenNoUsersExist_ShouldReturnEmptyListWithHttpStatusOk() {
        when(userService.getAllUsers()).thenReturn(Collections.emptyList());

        ResponseEntity<List<User>> response = userController.getAllUsers();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
        verify(userService, times(1)).getAllUsers();
    }

    @Test
    void getAllUsers_WhenServiceReturnsNull_ShouldHandleGracefully() {
        when(userService.getAllUsers()).thenReturn(null);

        ResponseEntity<List<User>> response = userController.getAllUsers();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody());
        verify(userService, times(1)).getAllUsers();
    }

    // GET USER BY ID TESTS
    @Test
    void getUserById_WithExistingId_ShouldReturnUserWithHttpStatusOk() {
        when(userService.getUserById(userId)).thenReturn(Optional.of(testUser));

        ResponseEntity<User> response = userController.getUserById(userId);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testUser.getId(), response.getBody().getId());
        assertEquals(testUser.getUsername(), response.getBody().getUsername());
        verify(userService, times(1)).getUserById(userId);
    }

    @Test
    void getUserById_WithNonExistingId_ShouldThrowResourceNotFoundException() {
        when(userService.getUserById(nonExistentUserId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> userController.getUserById(nonExistentUserId)
        );

        assertEquals("User not found with id: " + nonExistentUserId, exception.getMessage());
        verify(userService, times(1)).getUserById(nonExistentUserId);
    }

    @Test
    void getUserById_WithNullId_ShouldCallServiceWithNull() {
        when(userService.getUserById(null)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> userController.getUserById(null)
        );

        assertEquals("User not found with id: null", exception.getMessage());
        verify(userService, times(1)).getUserById(null);
    }

    // GET USER BY USERNAME TESTS
    @Test
    void getUserByUsername_WithValidExistingUsername_ShouldReturnUserWithHttpStatusOk() {
        when(userService.getUserByUsername(username)).thenReturn(Optional.of(testUser));

        ResponseEntity<User> response = userController.getUserByUsername(username);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testUser.getUsername(), response.getBody().getUsername());
        verify(userService, times(1)).getUserByUsername(username);
    }

    @Test
    void getUserByUsername_WithNonExistingUsername_ShouldThrowResourceNotFoundException() {
        when(userService.getUserByUsername(nonExistentUsername)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> userController.getUserByUsername(nonExistentUsername)
        );

        assertEquals("User not found with username: " + nonExistentUsername, exception.getMessage());
        verify(userService, times(1)).getUserByUsername(nonExistentUsername);
    }

    @Test
    void getUserByUsername_WithEmptyUsername_ShouldThrowIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userController.getUserByUsername("")
        );

        assertEquals("Username cannot be empty", exception.getMessage());
        verify(userService, never()).getUserByUsername(anyString());
    }

    @Test
    void getUserByUsername_WithNullUsername_ShouldThrowIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userController.getUserByUsername(null)
        );

        assertEquals("Username cannot be empty", exception.getMessage());
        verify(userService, never()).getUserByUsername(anyString());
    }

    @Test
    void getUserByUsername_WithTooShortUsername_ShouldThrowIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userController.getUserByUsername("ab")
        );

        assertEquals("Username must be between 3 and 50 characters", exception.getMessage());
        verify(userService, never()).getUserByUsername(anyString());
    }

    @Test
    void getUserByUsername_WithTooLongUsername_ShouldThrowIllegalArgumentException() {
        String longUsername = "a".repeat(51);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userController.getUserByUsername(longUsername)
        );

        assertEquals("Username must be between 3 and 50 characters", exception.getMessage());
        verify(userService, never()).getUserByUsername(anyString());
    }

    @Test
    void getUserByUsername_WithUsernameContainingInvalidCharacters_ShouldThrowIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userController.getUserByUsername("test@user")
        );

        assertEquals("Username can only contain letters, numbers, and underscores", exception.getMessage());
        verify(userService, never()).getUserByUsername(anyString());
    }

    @Test
    void getUserByUsername_WithBoundaryValidUsername_ShouldCallService() {
        String minLengthUsername = "abc";
        String maxLengthUsername = "a".repeat(50);
        when(userService.getUserByUsername(anyString())).thenReturn(Optional.of(testUser));

        assertDoesNotThrow(() -> userController.getUserByUsername(minLengthUsername));
        assertDoesNotThrow(() -> userController.getUserByUsername(maxLengthUsername));

        verify(userService, times(2)).getUserByUsername(anyString());
    }

    // GET USER BY EMAIL TESTS
    @Test
    void getUserByEmail_WithValidExistingEmail_ShouldReturnUserWithHttpStatusOk() {
        when(userService.getUserByEmail(email)).thenReturn(Optional.of(testUser));

        ResponseEntity<User> response = userController.getUserByEmail(email);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testUser.getEmail(), response.getBody().getEmail());
        verify(userService, times(1)).getUserByEmail(email);
    }

    @Test
    void getUserByEmail_WithNonExistingEmail_ShouldThrowResourceNotFoundException() {
        when(userService.getUserByEmail(nonExistentEmail)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> userController.getUserByEmail(nonExistentEmail)
        );

        assertEquals("User not found with email: " + nonExistentEmail, exception.getMessage());
        verify(userService, times(1)).getUserByEmail(nonExistentEmail);
    }

    @Test
    void getUserByEmail_WithEmptyEmail_ShouldThrowIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userController.getUserByEmail("")
        );

        assertEquals("Email cannot be empty", exception.getMessage());
        verify(userService, never()).getUserByEmail(anyString());
    }

    @Test
    void getUserByEmail_WithNullEmail_ShouldThrowIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userController.getUserByEmail(null)
        );

        assertEquals("Email cannot be empty", exception.getMessage());
        verify(userService, never()).getUserByEmail(anyString());
    }

    @Test
    void getUserByEmail_WithInvalidEmailFormat_ShouldThrowIllegalArgumentException() {
        String[] invalidEmails = {
                "invalid-email",
                "@gmail.com",
                "test@",
                "test@.com",
                "test@gmail" // Invalid due to missing TLD
        };

        for (String invalidEmail : invalidEmails) {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> userController.getUserByEmail(invalidEmail)
            );
            assertEquals("Invalid email format", exception.getMessage());
        }

        // Empty and blank emails should throw "Email cannot be empty"
        String[] emptyEmails = { "", "   " };
        for (String emptyEmail : emptyEmails) {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> userController.getUserByEmail(emptyEmail)
            );
            assertEquals("Email cannot be empty", exception.getMessage());
        }

        verify(userService, never()).getUserByEmail(anyString());
    }

    @Test
    void getUserByEmail_WithValidButNonExistingEmail_ShouldThrowResourceNotFoundException() {
        String validNonExistingEmail = "test@nonexistent.com"; // Updated to include TLD
        when(userService.getUserByEmail(validNonExistingEmail)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> userController.getUserByEmail(validNonExistingEmail)
        );
        assertEquals("User not found with email: " + validNonExistingEmail, exception.getMessage());
        verify(userService, times(1)).getUserByEmail(validNonExistingEmail);
    }

    @Test
    void getUserByEmail_WithValidEmailFormats_ShouldCallService() {
        String[] validEmails = {
                "test@gmail.com",
                "user.name@gmail.com",
                "user_name@gmail.com",
                "user123@gmail.com",
                "test.user+tag@gmail.com"
        };
        when(userService.getUserByEmail(anyString())).thenReturn(Optional.of(testUser));

        for (String validEmail : validEmails) {
            assertDoesNotThrow(() -> userController.getUserByEmail(validEmail));
        }

        verify(userService, times(validEmails.length)).getUserByEmail(anyString());
    }

    // UPDATE USER TESTS
    @Test
    void updateUser_WithValidData_ShouldReturnUpdatedUserWithHttpStatusOk() {
        User updatedUser = new User();
        updatedUser.setId(userId);
        updatedUser.setUsername("updateduser");
        updatedUser.setEmail("updated@gmail.com");
        updatedUser.setFirstName("Jane");
        updatedUser.setLastName("Smith");

        when(userService.updateUser(eq(userId), any(User.class))).thenReturn(updatedUser);

        ResponseEntity<User> response = userController.updateUser(userId, updatedUser);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(updatedUser.getUsername(), response.getBody().getUsername());
        assertEquals(updatedUser.getEmail(), response.getBody().getEmail());
        verify(userService, times(1)).updateUser(eq(userId), any(User.class));
    }

    @Test
    void updateUser_WithNullUserDetails_ShouldCallServiceWithNull() {
        when(userService.updateUser(eq(userId), isNull())).thenReturn(testUser);

        ResponseEntity<User> response = userController.updateUser(userId, null);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(userService, times(1)).updateUser(eq(userId), isNull());
    }

    // UPDATE PASSWORD TESTS
    @Test
    void updatePassword_WithValidPassword_ShouldReturnSuccessMessageWithHttpStatusOk() {
        UserController.PasswordUpdateRequest request = new UserController.PasswordUpdateRequest();
        request.setNewPassword("NewPass123!");

        doNothing().when(userService).updatePassword(anyLong(), anyString());

        ResponseEntity<String> response = userController.updatePassword(userId, request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Password updated successfully", response.getBody());
        verify(userService, times(1)).updatePassword(userId, "NewPass123!");
    }

    @Test
    void updatePassword_WithNonExistingUser_ShouldThrowResourceNotFoundException() {
        UserController.PasswordUpdateRequest request = new UserController.PasswordUpdateRequest();
        request.setNewPassword("NewPass123!");
        doThrow(new ResourceNotFoundException("User not found with id: " + nonExistentUserId))
                .when(userService).updatePassword(nonExistentUserId, "NewPass123!");

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> userController.updatePassword(nonExistentUserId, request)
        );

        assertEquals("User not found with id: " + nonExistentUserId, exception.getMessage());
        verify(userService, times(1)).updatePassword(nonExistentUserId, "NewPass123!");
    }

    @Test
    void updatePassword_WithNullRequest_ShouldNotCallService() {
        verify(userService, never()).updatePassword(anyLong(), anyString());
    }

    // DELETE USER TESTS
    @Test
    void deleteUser_WithValidId_ShouldReturnSuccessMessageWithHttpStatusOk() {
        doNothing().when(userService).deleteUser(userId);

        ResponseEntity<String> response = userController.deleteUser(userId);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("User deleted successfully", response.getBody());
        verify(userService, times(1)).deleteUser(userId);
    }

    // SEARCH USERS BY NAME TESTS
    @Test
    void searchUsersByName_WithValidSearchTerm_ShouldReturnMatchingUsersWithHttpStatusOk() {
        String searchName = "John";
        List<User> users = Arrays.asList(testUser);
        when(userService.searchUsersByName(searchName)).thenReturn(users);

        ResponseEntity<List<User>> response = userController.searchUsersByName(searchName);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(testUser, response.getBody().get(0));
        verify(userService, times(1)).searchUsersByName(searchName);
    }

    @Test
    void searchUsersByName_WithMultipleMatchingUsers_ShouldReturnAllMatches() {
        String searchName = "Jo";
        List<User> users = Arrays.asList(testUser, anotherUser);
        when(userService.searchUsersByName(searchName)).thenReturn(users);

        ResponseEntity<List<User>> response = userController.searchUsersByName(searchName);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        verify(userService, times(1)).searchUsersByName(searchName);
    }

    @Test
    void searchUsersByName_WithNoMatches_ShouldReturnEmptyList() {
        String searchName = "Nonexistent";
        when(userService.searchUsersByName(searchName)).thenReturn(Collections.emptyList());

        ResponseEntity<List<User>> response = userController.searchUsersByName(searchName);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
        verify(userService, times(1)).searchUsersByName(searchName);
    }

    @Test
    void searchUsersByName_WithShortSearchTerm_ShouldThrowIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userController.searchUsersByName("J")
        );

        assertEquals("Search term must be at least 2 characters long", exception.getMessage());
        verify(userService, never()).searchUsersByName(anyString());
    }

    @Test
    void searchUsersByName_WithBlankSearchTerm_ShouldThrowIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userController.searchUsersByName("   ")
        );

        assertEquals("Search term must be at least 2 characters long", exception.getMessage());
        verify(userService, never()).searchUsersByName(anyString());
    }

    @Test
    void searchUsersByName_WithNullSearchTerm_ShouldThrowIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userController.searchUsersByName(null)
        );

        assertEquals("Search term must be at least 2 characters long", exception.getMessage());
        verify(userService, never()).searchUsersByName(anyString());
    }

    @Test
    void searchUsersByName_WithExactTwoCharacterTerm_ShouldCallService() {
        String searchName = "Jo";
        when(userService.searchUsersByName(searchName)).thenReturn(Arrays.asList(testUser));

        ResponseEntity<List<User>> response = userController.searchUsersByName(searchName);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(userService, times(1)).searchUsersByName(searchName);
    }

    // PASSWORD VALIDATION TESTS
    @Test
    void passwordUpdateRequest_WithValidPassword_ShouldPassValidation() {
        String[] validPasswords = {
                "ValidPass123!",
                "StrongP@ss1",
                "Test123#",
                "MyP@ssw0rd",
                "Abc123$%"
        };

        for (String validPassword : validPasswords) {
            UserController.PasswordUpdateRequest request = new UserController.PasswordUpdateRequest();
            request.setNewPassword(validPassword);

            Set<ConstraintViolation<UserController.PasswordUpdateRequest>> violations = validator.validate(request);

            assertTrue(violations.isEmpty(), "Password '" + validPassword + "' should be valid but got violations: " + violations);
        }
    }

    @Test
    void passwordUpdateRequest_WithEmptyPassword_ShouldFailValidation() {
        UserController.PasswordUpdateRequest request = new UserController.PasswordUpdateRequest();
        request.setNewPassword("");

        Set<ConstraintViolation<UserController.PasswordUpdateRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("New password is required")));
    }

    @Test
    void passwordUpdateRequest_WithShortPassword_ShouldFailValidation() {
        UserController.PasswordUpdateRequest request = new UserController.PasswordUpdateRequest();
        request.setNewPassword("Short1!");

        Set<ConstraintViolation<UserController.PasswordUpdateRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("at least 8 characters")));
    }

    @Test
    void passwordUpdateRequest_WithoutSpecialCharacter_ShouldFailValidation() {
        UserController.PasswordUpdateRequest request = new UserController.PasswordUpdateRequest();
        request.setNewPassword("Password123");

        Set<ConstraintViolation<UserController.PasswordUpdateRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("special character")));
    }

    @Test
    void passwordUpdateRequest_WithoutUppercase_ShouldFailValidation() {
        UserController.PasswordUpdateRequest request = new UserController.PasswordUpdateRequest();
        request.setNewPassword("password123!");

        Set<ConstraintViolation<UserController.PasswordUpdateRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("uppercase letter")));
    }

    @Test
    void passwordUpdateRequest_WithoutLowercase_ShouldFailValidation() {
        UserController.PasswordUpdateRequest request = new UserController.PasswordUpdateRequest();
        request.setNewPassword("PASSWORD123!");

        Set<ConstraintViolation<UserController.PasswordUpdateRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("lowercase letter")));
    }

    @Test
    void passwordUpdateRequest_WithoutDigit_ShouldFailValidation() {
        UserController.PasswordUpdateRequest request = new UserController.PasswordUpdateRequest();
        request.setNewPassword("Password!");

        Set<ConstraintViolation<UserController.PasswordUpdateRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("digit")));
    }

    @Test
    void passwordUpdateRequest_WithWhitespace_ShouldFailValidation() {
        UserController.PasswordUpdateRequest request = new UserController.PasswordUpdateRequest();
        request.setNewPassword("Pass 123!");

        Set<ConstraintViolation<UserController.PasswordUpdateRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("whitespace")));
    }
}