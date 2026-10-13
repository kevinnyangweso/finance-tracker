package com.financetracker.controller;

import com.financetracker.dto.MessageResponse;
import com.financetracker.dto.TransactionRequest;
import com.financetracker.dto.TransactionResponse;
import com.financetracker.entity.TransactionType;
import com.financetracker.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    // ============================================
    // POST /api/transactions
    // ============================================
    @PostMapping
    public ResponseEntity<TransactionResponse> create(
            @Valid @RequestBody TransactionRequest request,
            Authentication authentication) {

        String username = authentication.getName();
        TransactionResponse response = transactionService.create(request, username);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // ============================================
    // GET /api/transactions
    // Optional filters: ?type=EXPENSE&category=Food&startDate=2026-01-01&endDate=2026-12-31
    // ============================================
    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getAll(
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) {

        String username = authentication.getName();
        List<TransactionResponse> transactions =
                transactionService.getFiltered(username, type, category, startDate, endDate);
        return ResponseEntity.ok(transactions);
    }

    // ============================================
    // GET /api/transactions/{id}
    // ============================================
    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getById(
            @PathVariable Long id,
            Authentication authentication) {

        String username = authentication.getName();
        return ResponseEntity.ok(transactionService.getById(id, username));
    }

    // ============================================
    // PUT /api/transactions/{id}
    // ============================================
    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody TransactionRequest request,
            Authentication authentication) {

        String username = authentication.getName();
        return ResponseEntity.ok(transactionService.update(id, request, username));
    }

    // ============================================
    // DELETE /api/transactions/{id}
    // ============================================
    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> delete(
            @PathVariable Long id,
            Authentication authentication) {

        String username = authentication.getName();
        transactionService.delete(id, username);
        return ResponseEntity.ok(new MessageResponse("Transaction deleted successfully"));
    }
}