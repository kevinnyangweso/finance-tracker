package com.kevin.financetracker.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "budgets")
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Budget name is required")
    @Size(min = 1, max = 100, message = "Budget name must be between 1 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9\\s\\-'.,]+$", message = "Budget name can only contain letters, numbers, spaces, and basic punctuation")
    @Column(nullable = false, length = 100)
    private String name;

    @NotNull(message = "Budget amount is required")
    @Digits(integer = 15, fraction = 2, message = "Amount must have up to 15 integer digits and 2 fraction digits")
    @DecimalMin(value = "0.01", message = "Budget amount must be at least 0.01")
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @NotNull(message = "Spent amount is required")
    @Digits(integer = 15, fraction = 2, message = "Spent amount must have up to 15 integer digits and 2 fraction digits")
    @DecimalMin(value = "0.00", message = "Spent amount cannot be negative")
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal spent = BigDecimal.ZERO;

    @NotNull(message = "Start date is required")
    @FutureOrPresent(message = "Start date cannot be in the past")
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @Future(message = "End date must be in the future")
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @NotNull(message = "Budget period is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BudgetPeriod period;

    // Relationships
    @NotNull(message = "Category is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @NotNull(message = "User is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Constructors
    public Budget() {}

    public Budget(String name, BigDecimal amount, LocalDate startDate,
                  LocalDate endDate, BudgetPeriod period, Category category, User user) {
        this.name = name;
        this.amount = amount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.period = period;
        this.category = category;
        this.user = user;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getSpent() { return spent; }
    public void setSpent(BigDecimal spent) { this.spent = spent; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public BudgetPeriod getPeriod() { return period; }
    public void setPeriod(BudgetPeriod period) { this.period = period; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    // Calculated fields (not persisted in database)
    @Transient
    public BigDecimal getRemaining() {
        return amount != null && spent != null ? amount.subtract(spent) : BigDecimal.ZERO;
    }

    @Transient
    public Double getProgressPercentage() {
        if (amount == null || spent == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        return (spent.doubleValue() / amount.doubleValue()) * 100;
    }

    @Transient
    public boolean isExceeded() {
        return amount != null && spent != null && spent.compareTo(amount) > 0;
    }

    // Business logic methods
    public void addSpentAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Spent amount must be positive");
        }
        this.spent = this.spent.add(amount);
    }

    public void resetSpent() {
        this.spent = BigDecimal.ZERO;
    }

    // Custom validation methods
    @AssertTrue(message = "End date must be after start date")
    public boolean isEndDateAfterStartDate() {
        if (startDate == null || endDate == null) {
            return true; // Let @NotNull handle null cases
        }
        return endDate.isAfter(startDate);
    }

    @AssertTrue(message = "Budget period must be valid for the date range")
    public boolean isPeriodValidForDateRange() {
        if (startDate == null || endDate == null || period == null) {
            return true; // Let @NotNull handle null cases
        }

        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);

        switch (period) {
            case DAILY:
                return daysBetween == 0; // Same day
            case WEEKLY:
                return daysBetween == 6; // 7 days total
            case MONTHLY:
                return daysBetween >= 27 && daysBetween <= 31; // Approximate month
            case QUARTERLY:
                return daysBetween >= 89 && daysBetween <= 92; // Approximate quarter
            case YEARLY:
                return daysBetween >= 364 && daysBetween <= 366; // Approximate year
            case CUSTOM:
                return daysBetween >= 0; // Any positive range
            default:
                return true;
        }
    }

    @AssertTrue(message = "Spent amount cannot exceed budget amount by more than 100%")
    public boolean isSpentAmountReasonable() {
        if (amount == null || spent == null) {
            return true; // Let @NotNull handle null cases
        }

        // Allow some overage but prevent extreme values
        BigDecimal maxReasonableSpent = amount.multiply(new BigDecimal("2")); // 200% of budget
        return spent.compareTo(maxReasonableSpent) <= 0;
    }

    @Override
    public String toString() {
        return "Budget{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", amount=" + amount +
                ", spent=" + spent +
                ", period=" + period +
                '}';
    }
}