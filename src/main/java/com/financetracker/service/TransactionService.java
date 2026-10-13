package com.financetracker.service;

import com.financetracker.dto.TransactionRequest;
import com.financetracker.dto.TransactionResponse;
import com.financetracker.entity.Transaction;
import com.financetracker.entity.TransactionType;
import com.financetracker.entity.User;
import com.financetracker.exception.ResourceNotFoundException;
import com.financetracker.repository.TransactionRepository;
import com.financetracker.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public TransactionService(TransactionRepository transactionRepository,
                              UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    // ============================================
    // CREATE
    // ============================================
    @Transactional
    public TransactionResponse create(TransactionRequest request, String username) {
        User user = getUserByUsername(username);

        Transaction transaction = new Transaction(
                request.getAmount(),
                request.getType(),
                request.getCategory(),
                request.getDate(),
                request.getDescription(),
                user
        );

        Transaction saved = transactionRepository.save(transaction);
        return toResponse(saved);
    }

    // ============================================
    // READ — all for current user
    // ============================================
    @Transactional(readOnly = true)
    public List<TransactionResponse> getAllForUser(String username) {
        User user = getUserByUsername(username);
        return transactionRepository.findByUserOrderByDateDesc(user)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ============================================
    // READ — with filters
    // ============================================
    @Transactional(readOnly = true)
    public List<TransactionResponse> getFiltered(
            String username,
            TransactionType type,
            String category,
            LocalDate startDate,
            LocalDate endDate) {

        User user = getUserByUsername(username);
        List<Transaction> transactions;

        if (type != null) {
            transactions = transactionRepository
                    .findByUserAndTypeOrderByDateDesc(user, type);
        } else if (category != null && !category.isBlank()) {
            transactions = transactionRepository
                    .findByUserAndCategoryOrderByDateDesc(user, category);
        } else if (startDate != null && endDate != null) {
            transactions = transactionRepository
                    .findByUserAndDateBetweenOrderByDateDesc(user, startDate, endDate);
        } else {
            transactions = transactionRepository.findByUserOrderByDateDesc(user);
        }

        return transactions.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ============================================
    // READ — one
    // ============================================
    @Transactional(readOnly = true)
    public TransactionResponse getById(Long id, String username) {
        User user = getUserByUsername(username);
        Transaction transaction = transactionRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaction not found with id: " + id));
        return toResponse(transaction);
    }

    // ============================================
    // UPDATE
    // ============================================
    @Transactional
    public TransactionResponse update(Long id, TransactionRequest request, String username) {
        User user = getUserByUsername(username);
        Transaction transaction = transactionRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaction not found with id: " + id));

        transaction.setAmount(request.getAmount());
        transaction.setType(request.getType());
        transaction.setCategory(request.getCategory());
        transaction.setDate(request.getDate());
        transaction.setDescription(request.getDescription());

        Transaction updated = transactionRepository.save(transaction);
        return toResponse(updated);
    }

    // ============================================
    // DELETE
    // ============================================
    @Transactional
    public void delete(Long id, String username) {
        User user = getUserByUsername(username);
        Transaction transaction = transactionRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaction not found with id: " + id));
        transactionRepository.delete(transaction);
    }

    // ============================================
    // HELPERS
    // ============================================
    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + username));
    }

    private TransactionResponse toResponse(Transaction t) {
        return new TransactionResponse(
                t.getId(),
                t.getAmount(),
                t.getType(),
                t.getCategory(),
                t.getDate(),
                t.getDescription(),
                t.getCreatedAt()
        );
    }
}