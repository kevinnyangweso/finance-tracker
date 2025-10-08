package com.kevin.financetracker.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Amount is required")
    @Digits(integer = 15, fraction = 2, message = "Amount must have up to 15 integer digits and 2 fraction digits")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @NotBlank(message = "Description is required")
    @Size(min = 1, max = 255, message = "Description must be between 1 and 255 characters")
    @Column(length = 255)
    private String description;

    @NotNull(message = "Transaction date is required")
    @PastOrPresent(message = "Transaction date cannot be in the future")
    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @NotNull(message = "Transaction type is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionType type;

    // Relationships
    @NotNull(message = "Account is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @NotNull(message = "User is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // For transfers between accounts
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfer_to_account_id")
    private Account transferToAccount;

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    @Column(length = 500)
    private String notes;

    @Size(max = 100, message = "Location cannot exceed 100 characters")
    @Column(length = 100)
    private String location;

    @Column(name = "receipt_image_url")
    private String receiptImageUrl;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Constructors
    public Transaction() {
        this.transactionDate = LocalDateTime.now();
    }

    public Transaction(BigDecimal amount, String description, TransactionType type,
                       Account account, Category category, User user) {
        this();
        this.amount = amount;
        this.description = description;
        this.type = type;
        this.account = account;
        this.category = category;
        this.user = user;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }

    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }

    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Account getTransferToAccount() { return transferToAccount; }
    public void setTransferToAccount(Account transferToAccount) { this.transferToAccount = transferToAccount; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getReceiptImageUrl() { return receiptImageUrl; }
    public void setReceiptImageUrl(String receiptImageUrl) { this.receiptImageUrl = receiptImageUrl; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // Custom validation methods
    @AssertTrue(message = "Transfer transactions must specify a destination account")
    public boolean isValidTransfer() {
        if (TransactionType.TRANSFER.equals(this.type)) {
            return transferToAccount != null;
        }
        return true; // Not a transfer, so validation passes
    }

    @AssertTrue(message = "Transfer cannot be to the same account")
    public boolean isValidTransferAccounts() {
        if (TransactionType.TRANSFER.equals(this.type) && transferToAccount != null) {
            return !account.getId().equals(transferToAccount.getId());
        }
        return true;
    }

    @AssertTrue(message = "Income and Expense transactions must have a category")
    public boolean hasValidCategory() {
        if (TransactionType.INCOME.equals(this.type) || TransactionType.EXPENSE.equals(this.type)) {
            return category != null;
        }
        return true; // Transfers don't require categories
    }

    @AssertTrue(message = "Category type must match transaction type")
    public boolean isCategoryTypeValid() {
        if (category == null) {
            return true; // No category to validate
        }

        if (TransactionType.INCOME.equals(this.type)) {
            return CategoryType.INCOME.equals(category.getType());
        } else if (TransactionType.EXPENSE.equals(this.type)) {
            return CategoryType.EXPENSE.equals(category.getType());
        }
        return true; // TRANSFER doesn't need category type matching
    }

    @AssertTrue(message = "Receipt image URL must be a valid URL format")
    public boolean isValidReceiptImageUrl() {
        if (receiptImageUrl == null || receiptImageUrl.trim().isEmpty()) {
            return true; // Optional field
        }

        // Basic URL validation
        return receiptImageUrl.matches("^(https?|ftp)://[^\\s/$.?#].[^\\s]*$");
    }

    // Business logic methods
    public boolean isTransfer() {
        return TransactionType.TRANSFER.equals(this.type);
    }

    public boolean isIncome() {
        return TransactionType.INCOME.equals(this.type);
    }

    public boolean isExpense() {
        return TransactionType.EXPENSE.equals(this.type);
    }

    @Transient
    public boolean isRecent() {
        if (transactionDate == null) {
            return false;
        }
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);
        return transactionDate.isAfter(oneWeekAgo);
    }

    @Transient
    public boolean isLargeTransaction() {
        if (amount == null) {
            return false;
        }
        BigDecimal largeAmountThreshold = new BigDecimal("1000.00");
        return amount.compareTo(largeAmountThreshold) > 0;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", amount=" + amount +
                ", description='" + description + '\'' +
                ", type=" + type +
                ", transactionDate=" + transactionDate +
                '}';
    }
}