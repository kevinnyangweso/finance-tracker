package com.kevin.financetracker.repository;

import com.kevin.financetracker.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User user1;
    private User user2;
    private User user3;

    @BeforeEach
    void setUp() {
        // Clear any existing data
        userRepository.deleteAll();

        // Define a fixed base time for consistency
        LocalDateTime baseTime = LocalDateTime.of(2025, 10, 11, 2, 20);

        // Create test users
        user1 = new User("john_doe", "john@gmail.com", "password123", "John", "Doe");
        user1.setRole(User.Role.USER);

        user2 = new User("jane_smith", "jane@gmail.com", "password456", "Jane", "Smith");
        user2.setRole(User.Role.USER);

        user3 = new User("bob_johnson", "bob@gmail.com", "password789", "Bob", "Johnson");
        user3.setRole(User.Role.USER);

        // Persist users without setting createdAt to avoid @CreationTimestamp override
        user1 = entityManager.persist(user1);
        user2 = entityManager.persist(user2);
        user3 = entityManager.persist(user3);

        // Explicitly set createdAt after persistence using JPQL to bypass @CreationTimestamp
        entityManager.getEntityManager()
                .createQuery("UPDATE User u SET u.createdAt = :createdAt WHERE u.id = :id")
                .setParameter("createdAt", baseTime.minusDays(5))
                .setParameter("id", user1.getId())
                .executeUpdate();

        entityManager.getEntityManager()
                .createQuery("UPDATE User u SET u.createdAt = :createdAt WHERE u.id = :id")
                .setParameter("createdAt", baseTime.minusDays(1))
                .setParameter("id", user2.getId())
                .executeUpdate();

        entityManager.getEntityManager()
                .createQuery("UPDATE User u SET u.createdAt = :createdAt WHERE u.id = :id")
                .setParameter("createdAt", baseTime)
                .setParameter("id", user3.getId())
                .executeUpdate();

        entityManager.flush();
    }

    @Test
    @DisplayName("Find user by username - should return user when username exists")
    void findByUsername_WhenUsernameExists_ShouldReturnUser() {
        // When
        Optional<User> foundUser = userRepository.findByUsername("john_doe");

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("john_doe");
        assertThat(foundUser.get().getEmail()).isEqualTo("john@gmail.com");
    }

    @Test
    @DisplayName("Find user by username - should return empty when username does not exist")
    void findByUsername_WhenUsernameDoesNotExist_ShouldReturnEmpty() {
        // When
        Optional<User> foundUser = userRepository.findByUsername("nonexistent");

        // Then
        assertThat(foundUser).isEmpty();
    }

    @Test
    @DisplayName("Find user by email - should return user when email exists")
    void findByEmail_WhenEmailExists_ShouldReturnUser() {
        // When
        Optional<User> foundUser = userRepository.findByEmail("jane@gmail.com");

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo("jane@gmail.com");
        assertThat(foundUser.get().getUsername()).isEqualTo("jane_smith");
    }

    @Test
    @DisplayName("Find user by email - should return empty when email does not exist")
    void findByEmail_WhenEmailDoesNotExist_ShouldReturnEmpty() {
        // When
        Optional<User> foundUser = userRepository.findByEmail("nonexistent@gmmail.com");

        // Then
        assertThat(foundUser).isEmpty();
    }

    @Test
    @DisplayName("Check if username exists - should return true when username exists")
    void existsByUsername_WhenUsernameExists_ShouldReturnTrue() {
        // When
        boolean exists = userRepository.existsByUsername("bob_johnson");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Check if username exists - should return false when username does not exist")
    void existsByUsername_WhenUsernameDoesNotExist_ShouldReturnFalse() {
        // When
        boolean exists = userRepository.existsByUsername("nonexistent");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Check if email exists - should return true when email exists")
    void existsByEmail_WhenEmailExists_ShouldReturnTrue() {
        // When
        boolean exists = userRepository.existsByEmail("bob@gmail.com");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Check if email exists - should return false when email does not exist")
    void existsByEmail_WhenEmailDoesNotExist_ShouldReturnFalse() {
        // When
        boolean exists = userRepository.existsByEmail("nonexistent@gmail.com");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Find users by first name containing ignore case - should return matching users")
    void findByFirstNameContainingIgnoreCase_ShouldReturnMatchingUsers() {
        // When
        List<User> users = userRepository.findByFirstNameContainingIgnoreCase("jo");

        // Then
        assertThat(users).hasSize(1);
        assertThat(users.get(0).getFirstName()).isEqualTo("John");
    }

    @Test
    @DisplayName("Find users by first name containing ignore case - should be case insensitive")
    void findByFirstNameContainingIgnoreCase_ShouldBeCaseInsensitive() {
        // When
        List<User> users = userRepository.findByFirstNameContainingIgnoreCase("JOHN");

        // Then
        assertThat(users).hasSize(1);
        assertThat(users.get(0).getFirstName()).isEqualTo("John");
    }

    @Test
    @DisplayName("Find users by first name containing - should return empty when no matches")
    void findByFirstNameContainingIgnoreCase_WhenNoMatches_ShouldReturnEmpty() {
        // When
        List<User> users = userRepository.findByFirstNameContainingIgnoreCase("xyz");

        // Then
        assertThat(users).isEmpty();
    }

    @Test
    @DisplayName("Find users by last name containing ignore case - should return matching users")
    void findByLastNameContainingIgnoreCase_ShouldReturnMatchingUsers() {
        // When
        List<User> users = userRepository.findByLastNameContainingIgnoreCase("smi");

        // Then
        assertThat(users).hasSize(1);
        assertThat(users.get(0).getLastName()).isEqualTo("Smith");
    }

    @Test
    @DisplayName("Find users by last name containing ignore case - should be case insensitive")
    void findByLastNameContainingIgnoreCase_ShouldBeCaseInsensitive() {
        // When
        List<User> users = userRepository.findByLastNameContainingIgnoreCase("SMITH");

        // Then
        assertThat(users).hasSize(1);
        assertThat(users.get(0).getLastName()).isEqualTo("Smith");
    }

    @Test
    @DisplayName("Find users by last name containing - should return empty when no matches")
    void findByLastNameContainingIgnoreCase_WhenNoMatches_ShouldReturnEmpty() {
        // When
        List<User> users = userRepository.findByLastNameContainingIgnoreCase("xyz");

        // Then
        assertThat(users).isEmpty();
    }

    @Test
    @DisplayName("Find users created after date - should return users created after specified date")
    void findUsersCreatedAfter_ShouldReturnUsersCreatedAfterDate() {
        // Given
        LocalDateTime cutoffDate = LocalDateTime.of(2025, 10, 9, 2, 20); // 2 days before base time

        // When
        List<User> users = userRepository.findUsersCreatedAfter(cutoffDate);

        // Then
        assertThat(users).hasSize(2); // Should return user2 and user3
        assertThat(users).extracting(User::getUsername)
                .containsExactlyInAnyOrder("jane_smith", "bob_johnson");
    }

    @Test
    @DisplayName("Find users created after date - should return empty when no users created after date")
    void findUsersCreatedAfter_WhenNoUsersCreatedAfter_ShouldReturnEmpty() {
        // Given
        LocalDateTime futureDate = LocalDateTime.now().plusDays(1);

        // When
        List<User> users = userRepository.findUsersCreatedAfter(futureDate);

        // Then
        assertThat(users).isEmpty();
    }

    @Test
    @DisplayName("Save new user - should persist user successfully")
    void save_NewUser_ShouldPersistSuccessfully() {
        // Given
        User newUser = new User("new_user", "new@gmail.com", "password", "New", "User");
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setRole(User.Role.USER);

        // When
        User savedUser = userRepository.save(newUser);

        // Then
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getUsername()).isEqualTo("new_user");

        // Verify it can be retrieved
        Optional<User> retrievedUser = userRepository.findByUsername("new_user");
        assertThat(retrievedUser).isPresent();
    }

    @Test
    @DisplayName("Update existing user - should update user successfully")
    void update_ExistingUser_ShouldUpdateSuccessfully() {
        // Given
        User user = userRepository.findByUsername("john_doe").get();
        user.setFirstName("Jonathan");

        // When
        User updatedUser = userRepository.save(user);

        // Then
        assertThat(updatedUser.getFirstName()).isEqualTo("Jonathan");

        // Verify the update persisted
        User retrievedUser = entityManager.find(User.class, user.getId());
        assertThat(retrievedUser.getFirstName()).isEqualTo("Jonathan");
    }

    @Test
    @DisplayName("Delete user - should remove user successfully")
    void delete_User_ShouldRemoveSuccessfully() {
        // Given
        User user = userRepository.findByUsername("john_doe").get();

        // When
        userRepository.delete(user);

        // Then
        assertThat(userRepository.findByUsername("john_doe")).isEmpty();
        assertThat(userRepository.existsByUsername("john_doe")).isFalse();
    }

    @Test
    @DisplayName("Find all users - should return all persisted users")
    void findAll_ShouldReturnAllUsers() {
        // When
        List<User> users = userRepository.findAll();

        // Then
        assertThat(users).hasSize(3);
        assertThat(users).extracting(User::getUsername)
                .containsExactlyInAnyOrder("john_doe", "jane_smith", "bob_johnson");
    }

    @Test
    @DisplayName("Find user by ID - should return user when ID exists")
    void findById_WhenIdExists_ShouldReturnUser() {
        // When
        Optional<User> foundUser = userRepository.findById(user1.getId());

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("john_doe");
    }

    @Test
    @DisplayName("Find user by ID - should return empty when ID does not exist")
    void findById_WhenIdDoesNotExist_ShouldReturnEmpty() {
        // When
        Optional<User> foundUser = userRepository.findById(999L);

        // Then
        assertThat(foundUser).isEmpty();
    }

    @Test
    @DisplayName("Username should be unique - should throw exception when duplicate username")
    void save_DuplicateUsername_ShouldThrowException() {
        // Given
        User duplicateUser = new User("john_doe", "different@gmail.com", "password", "Different", "User");
        duplicateUser.setRole(User.Role.USER);

        // When & Then
        assertThatThrownBy(() -> userRepository.saveAndFlush(duplicateUser))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Email should be unique - should throw exception when duplicate email")
    void save_DuplicateEmail_ShouldThrowException() {
        // Given
        User duplicateUser = new User("different_username", "john@gmail.com", "password", "Different", "User");
        duplicateUser.setRole(User.Role.USER);

        // When & Then
        assertThatThrownBy(() -> userRepository.saveAndFlush(duplicateUser))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Find users with different roles - should work correctly")
    void findByUsername_WithDifferentRoles_ShouldWork() {
        // Given
        User adminUser = new User("admin_user", "admin@gmail.com", "password", "Admin", "User");
        adminUser.setRole(User.Role.ADMIN);
        userRepository.save(adminUser);

        // When
        Optional<User> foundAdmin = userRepository.findByUsername("admin_user");

        // Then
        assertThat(foundAdmin).isPresent();
        assertThat(foundAdmin.get().getRole()).isEqualTo(User.Role.ADMIN);
    }

    @Test
    @DisplayName("Test user account status flags - should persist correctly")
    void save_UserWithAccountStatus_ShouldPersistCorrectly() {
        // Given
        User user = new User("test_user", "test@gmail.com", "password", "Test", "User");
        user.setRole(User.Role.USER);
        user.setEnabled(false);
        user.setAccountNonLocked(false);
        user.setAccountNonExpired(false);
        user.setCredentialsNonExpired(false);

        // When
        User savedUser = userRepository.save(user);
        User retrievedUser = userRepository.findByUsername("test_user").get();

        // Then
        assertThat(retrievedUser.isEnabled()).isFalse();
        assertThat(retrievedUser.isAccountNonLocked()).isFalse();
        assertThat(retrievedUser.isAccountNonExpired()).isFalse();
        assertThat(retrievedUser.isCredentialsNonExpired()).isFalse();
    }

    @Test
    @DisplayName("Find users with partial name matches - should return correct results")
    void findByFirstNameContaining_WithPartialMatches_ShouldReturnCorrectResults() {
        // When
        List<User> usersWithJ = userRepository.findByFirstNameContainingIgnoreCase("J");

        // Then
        assertThat(usersWithJ).hasSize(2); // John and Jane
        assertThat(usersWithJ).extracting(User::getFirstName)
                .containsExactlyInAnyOrder("John", "Jane");
    }

    @Test
    @DisplayName("Find users with last name containing - should handle multiple matches")
    void findByLastNameContaining_WithMultipleMatches_ShouldReturnAll() {
        // Given - Add another user with similar last name
        User user4 = new User("alice_smith", "alice@gmail.com", "password", "Alice", "Smith");
        user4.setRole(User.Role.USER);
        userRepository.save(user4);

        // When
        List<User> smithUsers = userRepository.findByLastNameContainingIgnoreCase("smith");

        // Then
        assertThat(smithUsers).hasSize(2); // Jane Smith and Alice Smith
        assertThat(smithUsers).extracting(User::getUsername)
                .containsExactlyInAnyOrder("jane_smith", "alice_smith");
    }
}