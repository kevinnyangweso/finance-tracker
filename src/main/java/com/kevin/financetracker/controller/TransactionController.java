package com.kevin.financetracker.controller;

import com.kevin.financetracker.dto.TransactionRequest;
import com.kevin.financetracker.dto.TransactionResponse;
import com.kevin.financetracker.dto.UpdateTransactionRequest;
import com.kevin.financetracker.exception.ResourceNotFoundException;
import com.kevin.financetracker.exception.ValidationException;
import com.kevin.financetracker.model.Transaction;
import com.kevin.financetracker.model.TransactionType;
import com.kevin.financetracker.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    @Autowired
    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    // Create a new transaction - UPDATED to use TransactionRequest
    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(
            @PathVariable Long userId,
            @Valid @RequestBody TransactionRequest request) {

        TransactionResponse response = transactionService.createTransaction(request, userId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // Get all transactions for user - ENHANCED to return responses
    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getTransactionsByUser(@PathVariable Long userId) {
        List<Transaction> transactions = transactionService.getTransactionsByUser(userId);
        List<TransactionResponse> responses = transactions.stream()
                .map(transactionService::convertToResponse)
                .toList();
        return new ResponseEntity<>(responses, HttpStatus.OK);
    }

    // Get transaction by ID - ENHANCED to return response
    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransactionById(
            @PathVariable Long userId,
            @PathVariable Long transactionId) {

        Transaction transaction = transactionService.getTransactionById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId));

        verifyTransactionOwnership(userId, transaction);
        TransactionResponse response = transactionService.convertToResponse(transaction);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // Get transactions within date range - ENHANCED to return responses
    @GetMapping("/date-range")
    public ResponseEntity<List<TransactionResponse>> getTransactionsByDateRange(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        validateDateRange(start, end);
        List<Transaction> transactions = transactionService.getTransactionsByUserAndDateRange(userId, start, end);
        List<TransactionResponse> responses = transactions.stream()
                .map(transactionService::convertToResponse)
                .toList();
        return new ResponseEntity<>(responses, HttpStatus.OK);
    }

    // Get transactions by type - ENHANCED to return responses
    @GetMapping("/type/{type}")
    public ResponseEntity<List<TransactionResponse>> getTransactionsByType(
            @PathVariable Long userId,
            @PathVariable TransactionType type) {

        List<Transaction> transactions = transactionService.getTransactionsByUserAndType(userId, type);
        List<TransactionResponse> responses = transactions.stream()
                .map(transactionService::convertToResponse)
                .toList();
        return new ResponseEntity<>(responses, HttpStatus.OK);
    }

    // Update transaction - KEEP as-is for now (still uses Transaction entity)
    // Update transaction - UPDATED to use UpdateTransactionRequest
    @PutMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> updateTransaction(
            @PathVariable Long userId,
            @PathVariable Long transactionId,
            @Valid @RequestBody UpdateTransactionRequest request) {

        TransactionResponse response = transactionService.updateTransaction(transactionId, request, userId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // Delete transaction - KEEP as-is
    @DeleteMapping("/{transactionId}")
    public ResponseEntity<String> deleteTransaction(
            @PathVariable Long userId,
            @PathVariable Long transactionId) {

        Transaction transaction = transactionService.getTransactionById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId));

        verifyTransactionOwnership(userId, transaction);
        transactionService.deleteTransaction(transactionId);
        return new ResponseEntity<>("Transaction deleted successfully", HttpStatus.OK);
    }

    // Get financial summary - KEEP as-is
    @GetMapping("/financial-summary")
    public ResponseEntity<TransactionService.FinancialSummary> getFinancialSummary(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        validateDateRange(start, end);
        TransactionService.FinancialSummary summary = transactionService.getFinancialSummary(userId, start, end);
        return new ResponseEntity<>(summary, HttpStatus.OK);
    }

    // Get recent transactions - ENHANCED to return responses
    @GetMapping("/recent")
    public ResponseEntity<List<TransactionResponse>> getRecentTransactions(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") int limit) {

        if (limit <= 0 || limit > 100) {
            throw new ValidationException("Limit must be between 1 and 100");
        }

        List<Transaction> transactions = transactionService.getTransactionsByUser(userId);
        if (transactions.size() > limit) {
            transactions = transactions.subList(0, limit);
        }

        List<TransactionResponse> responses = transactions.stream()
                .map(transactionService::convertToResponse)
                .toList();
        return new ResponseEntity<>(responses, HttpStatus.OK);
    }

    // Security helper method
    private void verifyTransactionOwnership(Long userId, Transaction transaction) {
        if (!transaction.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Transaction not found for this user");
        }
    }

    // Date validation helper method
    private void validateDateRange(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new ValidationException("Both start and end dates are required");
        }

        if (start.isAfter(end)) {
            throw new ValidationException("Start date cannot be after end date");
        }

        // Prevent querying too large date ranges
        if (start.plusYears(1).isBefore(end)) {
            throw new ValidationException("Date range cannot exceed 1 year");
        }
    }
}