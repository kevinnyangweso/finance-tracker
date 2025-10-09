package com.kevin.financetracker.dto;

import com.kevin.financetracker.model.TransactionType;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UpdateTransactionRequest(
        @Positive BigDecimal amount,
        @Size(max = 255) String description,
        TransactionType type,
        Long categoryId,
        @PastOrPresent LocalDateTime transactionDate,
        @Size(max = 1000) String notes
) {}
