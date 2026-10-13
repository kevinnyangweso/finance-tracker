package com.financetracker.dto;

import com.financetracker.entity.TransactionType;
import com.financetracker.entity.Category;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class TransactionResponse {

    private Long id;
    private BigDecimal amount;
    private TransactionType type;
    private Long categoryId;
    private String categoryName;
    private LocalDate date;
    private String description;
    private LocalDateTime createdAt;

    public TransactionResponse() {
    }

    public TransactionResponse(Long id, BigDecimal amount, TransactionType type,
                               Long categoryId, String categoryName, LocalDate date, String description,
                               LocalDateTime createdAt) {
        this.id = id;
        this.amount = amount;
        this.type = type;
        this.categoryId = categoryId;
        this.categoryName = categoryName;   
        this.date = date;
        this.description = description;
        this.createdAt = createdAt;
    }

    // === Getters and Setters ===
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}