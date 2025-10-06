package com.kevin.financetracker.controller;

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
import java.util.Optional;

@RestController
@RequestMapping("/api/users/{userId}/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    @Autowired
    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    // Create a new transaction
    @PostMapping
    public ResponseEntity<?> createTransaction(@PathVariable Long userId, @Valid @RequestBody Transaction transaction) {
        try {
            Transaction createdTransaction = transactionService.createTransaction(userId, transaction);
            return new ResponseEntity<>(createdTransaction, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // Get all transactions for user
    @GetMapping
    public ResponseEntity<List<Transaction>> getTransactionsByUser(@PathVariable Long userId) {
        List<Transaction> transactions = transactionService.getTransactionsByUser(userId);
        return new ResponseEntity<>(transactions, HttpStatus.OK);
    }

    // Get transaction by ID
    @GetMapping("/{transactionId}")
    public ResponseEntity<?> getTransactionById(@PathVariable Long userId, @PathVariable Long transactionId) {
        Optional<Transaction> transaction = transactionService.getTransactionById(transactionId);
        if (transaction.isPresent() && transaction.get().getUser().getId().equals(userId)) {
            return new ResponseEntity<>(transaction.get(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Transaction not found", HttpStatus.NOT_FOUND);
        }
    }

    // Get transactions within date range
    @GetMapping("/date-range")
    public ResponseEntity<List<Transaction>> getTransactionsByDateRange(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        List<Transaction> transactions = transactionService.getTransactionsByUserAndDateRange(userId, start, end);
        return new ResponseEntity<>(transactions, HttpStatus.OK);
    }

    // Get transactions by type
    @GetMapping("/type/{type}")
    public ResponseEntity<List<Transaction>> getTransactionsByType(@PathVariable Long userId, @PathVariable TransactionType type) {
        List<Transaction> transactions = transactionService.getTransactionsByUserAndType(userId, type);
        return new ResponseEntity<>(transactions, HttpStatus.OK);
    }

    // Update transaction
    @PutMapping("/{transactionId}")
    public ResponseEntity<?> updateTransaction(@PathVariable Long userId, @PathVariable Long transactionId,
                                               @Valid @RequestBody Transaction transactionDetails) {
        try {
            Transaction updatedTransaction = transactionService.updateTransaction(transactionId, transactionDetails);
            return new ResponseEntity<>(updatedTransaction, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    // Delete transaction
    @DeleteMapping("/{transactionId}")
    public ResponseEntity<?> deleteTransaction(@PathVariable Long userId, @PathVariable Long transactionId) {
        try {
            transactionService.deleteTransaction(transactionId);
            return new ResponseEntity<>("Transaction deleted successfully", HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    // Get financial summary
    @GetMapping("/financial-summary")
    public ResponseEntity<?> getFinancialSummary(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        try {
            TransactionService.FinancialSummary summary = transactionService.getFinancialSummary(userId, start, end);
            return new ResponseEntity<>(summary, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // Get recent transactions
    @GetMapping("/recent")
    public ResponseEntity<List<Transaction>> getRecentTransactions(@PathVariable Long userId,
                                                                   @RequestParam(defaultValue = "10") int limit) {
        // Note: You'll need to add this method to your service
        List<Transaction> transactions = transactionService.getTransactionsByUser(userId);
        if (transactions.size() > limit) {
            transactions = transactions.subList(0, limit);
        }
        return new ResponseEntity<>(transactions, HttpStatus.OK);
    }
}