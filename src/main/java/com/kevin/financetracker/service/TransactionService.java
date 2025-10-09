package com.kevin.financetracker.service;

import com.kevin.financetracker.dto.TransactionRequest;
import com.kevin.financetracker.dto.TransactionResponse;
import com.kevin.financetracker.dto.UpdateTransactionRequest;
import com.kevin.financetracker.exception.ResourceNotFoundException;
import com.kevin.financetracker.exception.ValidationException;
import com.kevin.financetracker.model.*;
import com.kevin.financetracker.repository.TransactionRepository;
import com.kevin.financetracker.repository.AccountRepository;
import com.kevin.financetracker.repository.CategoryRepository;
import com.kevin.financetracker.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final BudgetService budgetService;

    @Autowired
    public TransactionService(TransactionRepository transactionRepository,
                              AccountRepository accountRepository,
                              CategoryRepository categoryRepository,
                              UserRepository userRepository,
                              BudgetService budgetService) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.budgetService = budgetService;
    }

    // Create a new transaction
    public TransactionResponse createTransaction(TransactionRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Validate and get account
        Account account = accountRepository.findById(request.accountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        // Verify account belongs to user
        if (!account.getUser().getId().equals(userId)) {
            throw new ValidationException("Account does not belong to user");
        }

        // Validate and get category if provided
        Category category = null;
        if (request.categoryId() != null) {
            category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

            // Verify category belongs to user
            if (!category.getUser().getId().equals(userId)) {
                throw new ValidationException("Category does not belong to user");
            }
        }

        // Create transaction entity from request
        Transaction transaction = new Transaction();
        transaction.setAmount(request.amount());
        transaction.setDescription(request.description());
        transaction.setType(request.type());
        transaction.setTransactionDate(request.transactionDate() != null ?
                request.transactionDate() : LocalDateTime.now());
        transaction.setNotes(request.notes());
        transaction.setUser(user);
        transaction.setAccount(account);
        transaction.setCategory(category);

        // Update account balance based on transaction type
        updateAccountBalance(account, transaction.getType(), transaction.getAmount());

        Transaction savedTransaction = transactionRepository.save(transaction);

        // Update budget if this is an expense
        if (transaction.getType() == TransactionType.EXPENSE && category != null) {
            budgetService.updateBudgetSpentAmount(userId, category.getId(), transaction.getAmount());
        }

        return convertToResponse(savedTransaction);
    }

    // Get transaction by ID
    @Transactional(readOnly = true)
    public Optional<Transaction> getTransactionById(Long id) {
        return transactionRepository.findById(id);
    }

    // Get all transactions for a user
    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return transactionRepository.findByUser(user);
    }

    // Get transactions for a user within date range
    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsByUserAndDateRange(Long userId, LocalDateTime start, LocalDateTime end) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return transactionRepository.findByUserAndTransactionDateBetween(user, start, end);
    }

    // Get transactions by type for a user
    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsByUserAndType(Long userId, TransactionType type) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return transactionRepository.findByUserAndType(user, type);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionResponsesByUser(Long userId) {
        return getTransactionsByUser(userId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // Update transaction using DTO
    public TransactionResponse updateTransaction(Long transactionId, UpdateTransactionRequest request, Long userId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId));

        // Verify transaction belongs to user
        if (!transaction.getUser().getId().equals(userId)) {
            throw new ValidationException("Transaction does not belong to user");
        }

        // Store old values for balance adjustment
        BigDecimal oldAmount = transaction.getAmount();
        TransactionType oldType = transaction.getType();
        Account oldAccount = transaction.getAccount();

        // Update fields if provided in request
        if (request.amount() != null) {
            transaction.setAmount(request.amount());
        }
        if (request.description() != null) {
            transaction.setDescription(request.description());
        }
        if (request.transactionDate() != null) {
            transaction.setTransactionDate(request.transactionDate());
        }
        if (request.type() != null) {
            transaction.setType(request.type());
        }
        if (request.notes() != null) {
            transaction.setNotes(request.notes());
        }

        // Handle category update if provided
        if (request.categoryId() != null) {
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

            // Verify category belongs to user
            if (!category.getUser().getId().equals(userId)) {
                throw new ValidationException("Category does not belong to user");
            }
            transaction.setCategory(category);
        }

        // Revert old balance and apply new balance
        revertAccountBalance(oldAccount, oldType, oldAmount);
        updateAccountBalance(transaction.getAccount(), transaction.getType(), transaction.getAmount());

        Transaction updatedTransaction = transactionRepository.save(transaction);
        return convertToResponse(updatedTransaction);
    }

    // Delete transaction
    public void deleteTransaction(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));

        // Revert account balance
        revertAccountBalance(transaction.getAccount(), transaction.getType(), transaction.getAmount());

        transactionRepository.delete(transaction);
    }

    // Get financial summary for a user
    @Transactional(readOnly = true)
    public FinancialSummary getFinancialSummary(Long userId, LocalDateTime start, LocalDateTime end) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        BigDecimal totalIncome = transactionRepository.getTotalIncomeByUserAndDateRange(user, start, end)
                .orElse(BigDecimal.ZERO);
        BigDecimal totalExpenses = transactionRepository.getTotalExpensesByUserAndDateRange(user, start, end)
                .orElse(BigDecimal.ZERO);
        BigDecimal netSavings = totalIncome.subtract(totalExpenses);

        return new FinancialSummary(totalIncome, totalExpenses, netSavings);
    }

    // Helper method to update account balance
    private void updateAccountBalance(Account account, TransactionType type, BigDecimal amount) {
        switch (type) {
            case INCOME:
                account.deposit(amount);
                break;
            case EXPENSE:
                account.withdraw(amount);
                break;
            case TRANSFER:
                // For transfers, we handle both accounts in the transfer method
                break;
        }
        accountRepository.save(account);
    }

    // Helper method to revert account balance
    private void revertAccountBalance(Account account, TransactionType type, BigDecimal amount) {
        switch (type) {
            case INCOME:
                account.withdraw(amount); // Revert income
                break;
            case EXPENSE:
                account.deposit(amount); // Revert expense
                break;
            case TRANSFER:
                // For transfers, we handle both accounts
                break;
        }
        accountRepository.save(account);
    }

    public TransactionResponse convertToResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAmount(),
                transaction.getDescription(),
                transaction.getType(),
                transaction.getAccount() != null ? transaction.getAccount().getName() : null,
                transaction.getCategory() != null ? transaction.getCategory().getName() : null,
                transaction.getTransactionDate(),
                transaction.getCreatedAt(),
                transaction.getNotes()
        );
    }

    // DTO for financial summary
    public static class FinancialSummary {
        private final BigDecimal totalIncome;
        private final BigDecimal totalExpenses;
        private final BigDecimal netSavings;

        public FinancialSummary(BigDecimal totalIncome, BigDecimal totalExpenses, BigDecimal netSavings) {
            this.totalIncome = totalIncome;
            this.totalExpenses = totalExpenses;
            this.netSavings = netSavings;
        }

        // Getters
        public BigDecimal getTotalIncome() { return totalIncome; }
        public BigDecimal getTotalExpenses() { return totalExpenses; }
        public BigDecimal getNetSavings() { return netSavings; }
    }
}