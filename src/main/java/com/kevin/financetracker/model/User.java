package com.kevin.financetracker.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers, and underscores")
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$",
            message = "Password must contain at least one digit, one lowercase letter, one uppercase letter, one special character, and no whitespace"
    )
    @Column(nullable = false, length = 255)
    private String password;

    @NotBlank(message = "First name is required")
    @Size(min = 1, max = 50, message = "First name must be between 1 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z\\s\\-']+$", message = "First name can only contain letters, spaces, hyphens, and apostrophes")
    @Column(name = "first_name", length = 50)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 1, max = 50, message = "Last name must be between 1 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z\\s\\-']+$", message = "Last name can only contain letters, spaces, hyphens, and apostrophes")
    @Column(name = "last_name", length = 50)
    private String lastName;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relationships
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Account> accounts = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Transaction> transactions = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Budget> budgets = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Category> categories = new ArrayList<>();

    // Constructors
    public User() {
        // Default constructor required by JPA
    }

    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    public User(String username, String email, String password, String firstName, String lastName) {
        this(username, email, password);
        this.firstName = firstName;
        this.lastName = lastName;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // Collections getters and setters
    public List<Account> getAccounts() { return accounts; }
    public void setAccounts(List<Account> accounts) { this.accounts = accounts; }

    public List<Transaction> getTransactions() { return transactions; }
    public void setTransactions(List<Transaction> transactions) { this.transactions = transactions; }

    public List<Budget> getBudgets() { return budgets; }
    public void setBudgets(List<Budget> budgets) { this.budgets = budgets; }

    public List<Category> getCategories() { return categories; }
    public void setCategories(List<Category> categories) { this.categories = categories; }

    // Helper methods for bidirectional relationships
    public void addAccount(Account account) {
        if (account != null) {
            accounts.add(account);
            account.setUser(this);
        }
    }

    public void removeAccount(Account account) {
        if (account != null) {
            accounts.remove(account);
            account.setUser(null);
        }
    }

    public void addTransaction(Transaction transaction) {
        if (transaction != null) {
            transactions.add(transaction);
            transaction.setUser(this);
        }
    }

    public void addBudget(Budget budget) {
        if (budget != null) {
            budgets.add(budget);
            budget.setUser(this);
        }
    }

    public void addCategory(Category category) {
        if (category != null) {
            categories.add(category);
            category.setUser(this);
        }
    }

    // Custom validation methods
    @AssertTrue(message = "Email domain must be valid")
    public boolean isValidEmailDomain() {
        if (email == null) {
            return true; // Let @NotBlank handle null
        }

        // Basic domain validation - you can extend this with more complex rules
        String[] commonInvalidDomains = {"example.com", "test.com", "temp.com"};
        String domain = email.substring(email.lastIndexOf('@') + 1).toLowerCase();

        for (String invalidDomain : commonInvalidDomains) {
            if (domain.equals(invalidDomain)) {
                return false;
            }
        }
        return true;
    }

    @AssertTrue(message = "Username cannot be an email address")
    public boolean isUsernameNotEmail() {
        if (username == null || email == null) {
            return true; // Let other validations handle nulls
        }

        // Simple check to prevent username being an email
        return !username.contains("@");
    }

    @AssertTrue(message = "Password cannot contain username or email")
    public boolean isPasswordSecure() {
        if (password == null || username == null || email == null) {
            return true; // Let other validations handle nulls
        }

        String lowercasePassword = password.toLowerCase();
        return !lowercasePassword.contains(username.toLowerCase()) &&
                !lowercasePassword.contains(email.substring(0, email.indexOf('@')).toLowerCase());
    }

    // Business logic methods
    @Transient
    public String getFullName() {
        if (firstName == null && lastName == null) {
            return null;
        }
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "").trim();
    }

    @Transient
    public boolean isActive() {
        // You can add logic here based on your business rules
        // For example, check if user has been active recently
        return true; // Default implementation
    }

    @Transient
    public int getTotalAccounts() {
        return accounts != null ? accounts.size() : 0;
    }

    @Transient
    public int getTotalTransactions() {
        return transactions != null ? transactions.size() : 0;
    }

    // toString() method (helpful for debugging)
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}