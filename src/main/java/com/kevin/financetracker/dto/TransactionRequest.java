package com.kevin.financetracker.dto;

import com.kevin.financetracker.model.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionRequest(
        @NotNull @Positive BigDecimal amount,
        @NotBlank String description,
        @NotNull TransactionType type,
        @NotNull Long accountId,
        Long categoryId,  // Changed to Long (can be null for some transactions)
        LocalDateTime transactionDate,
        String notes  // Added to match service expectations
) {}
