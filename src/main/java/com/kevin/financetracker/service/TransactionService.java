package com.kevin.financetracker.service;

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
    public Transaction createTransaction(Long userId, Transaction transaction) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Account account = accountRepository.findById(transaction.getAccount().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        // Verify account belongs to user
        if (!account.getUser().getId().equals(userId)) {
            throw new ValidationException("Account does not belong to user");
        }

        Category category = null;
        if (transaction.getCategory() != null && transaction.getCategory().getId() != null) {
            category = categoryRepository.findById(transaction.getCategory().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

            // Verify category belongs to user
            if (!category.getUser().getId().equals(userId)) {
                throw new ValidationException("Category does not belong to user");
            }
        }

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

        return savedTransaction;
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

    // Update transaction
    public Transaction updateTransaction(Long transactionId, Transaction transactionDetails) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId));

        // Store old values for balance adjustment
        BigDecimal oldAmount = transaction.getAmount();
        TransactionType oldType = transaction.getType();
        Account oldAccount = transaction.getAccount();

        // Update fields if provided
        if (transactionDetails.getAmount() != null) {
            transaction.setAmount(transactionDetails.getAmount());
        }
        if (transactionDetails.getDescription() != null) {
            transaction.setDescription(transactionDetails.getDescription());
        }
        if (transactionDetails.getTransactionDate() != null) {
            transaction.setTransactionDate(transactionDetails.getTransactionDate());
        }
        if (transactionDetails.getType() != null) {
            transaction.setType(transactionDetails.getType());
        }
        if (transactionDetails.getCategory() != null && transactionDetails.getCategory().getId() != null) {
            Category category = categoryRepository.findById(transactionDetails.getCategory().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
            transaction.setCategory(category);
        }
        if (transactionDetails.getNotes() != null) {
            transaction.setNotes(transactionDetails.getNotes());
        }

        // Revert old balance and apply new balance
        revertAccountBalance(oldAccount, oldType, oldAmount);
        updateAccountBalance(transaction.getAccount(), transaction.getType(), transaction.getAmount());

        return transactionRepository.save(transaction);
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