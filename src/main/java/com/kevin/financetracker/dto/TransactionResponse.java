package com.kevin.financetracker.dto;

import com.kevin.financetracker.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        BigDecimal amount,
        String description,
        TransactionType type,
        String accountName,
        String categoryName,
        LocalDateTime transactionDate,
        LocalDateTime createdAt,
        String notes  // Added to match Transaction entity
) {}
