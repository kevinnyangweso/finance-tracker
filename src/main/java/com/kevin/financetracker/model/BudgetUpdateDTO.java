package com.kevin.financetracker.model;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Future;
import java.math.BigDecimal;
import java.time.LocalDate;

public class BudgetUpdateDTO {
    @NotEmpty(message = "Name cannot be empty")
    private String name;

    @PositiveOrZero(message = "Amount must be positive or zero")
    private BigDecimal amount;

    @PositiveOrZero(message = "Spent must be positive or zero")
    private BigDecimal spent;

    private BudgetPeriod period;

    @FutureOrPresent(message = "Start date must be today or in the future")
    private LocalDate startDate;

    @Future(message = "End date must be in the future")
    private LocalDate endDate;

    private Category category;

    private User user;

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getSpent() { return spent; }
    public void setSpent(BigDecimal spent) { this.spent = spent; }
    public BudgetPeriod getPeriod() { return period; }
    public void setPeriod(BudgetPeriod period) { this.period = period; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}