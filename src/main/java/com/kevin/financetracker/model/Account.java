package com.kevin.financetracker.model;

import com.kevin.financetracker.exception.ValidationException;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Account name is required")
    @Size(min = 1, max = 100, message = "Account name must be between 1 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9\\s\\-'.,]+$", message = "Account name can only contain letters, numbers, spaces, and basic punctuation")
    @Column(nullable = false, length = 100)
    private String name;

    @NotNull(message = "Account type is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountType type;

    @NotNull(message = "Balance is required")
    @Digits(integer = 15, fraction = 2, message = "Balance must have up to 15 integer digits and 2 fraction digits")
    @DecimalMin(value = "0.00", message = "Balance cannot be negative")
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be exactly 3 characters")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid 3-letter uppercase ISO code")
    @Column(length = 3)
    private String currency = "USD";

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    @Column(length = 255)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Relationships
    @NotNull(message = "User is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Transaction> transactions = new ArrayList<>();

    // Constructors
    public Account() {
        this.balance = BigDecimal.ZERO;
    }

    public Account(String name, AccountType type, User user) {
        this();
        this.name = name;
        this.type = type;
        this.user = user;
    }

    public Account(String name, AccountType type, BigDecimal balance, User user) {
        this(name, type, user);
        this.balance = balance;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public AccountType getType() { return type; }
    public void setType(AccountType type) { this.type = type; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public List<Transaction> getTransactions() { return transactions; }
    public void setTransactions(List<Transaction> transactions) { this.transactions = transactions; }

    // Helper methods
    public void addTransaction(Transaction transaction) {
        transactions.add(transaction);
        transaction.setAccount(this);
    }

    public void removeTransaction(Transaction transaction) {
        transactions.remove(transaction);
        transaction.setAccount(null);
    }

    // Business logic methods
    public void deposit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Deposit amount must be positive");
        }
        this.balance = this.balance.add(amount);
    }

    public void withdraw(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Withdrawal amount must be positive");
        }
        if (this.balance.compareTo(amount) < 0) {
            throw new ValidationException("Insufficient funds");
        }
        this.balance = this.balance.subtract(amount);
    }

    // Custom validation method for business rules
    @AssertTrue(message = "Account balance must be appropriate for the account type")
    public boolean isBalanceValidForAccountType() {
        if (type == null || balance == null) {
            return true; // Let @NotNull handle these cases
        }

        // Business rules based on account type
        switch (type) {
            case CREDIT_CARD:
            case LOAN:
                // Credit cards and loans can have negative balances (representing debt)
                return true;
            case CHECKING:
            case SAVINGS:
            case CASH:
            case INVESTMENT:
                // These account types should not have negative balances
                return balance.compareTo(BigDecimal.ZERO) >= 0;
            default:
                return true;
        }
    }

    @Override
    public String toString() {
        return "Account{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", balance=" + balance +
                ", currency='" + currency + '\'' +
                '}';
    }
}