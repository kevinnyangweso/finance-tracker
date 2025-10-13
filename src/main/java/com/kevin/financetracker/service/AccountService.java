package com.kevin.financetracker.service;

import com.kevin.financetracker.exception.BusinessException;
import com.kevin.financetracker.exception.DuplicateResourceException;
import com.kevin.financetracker.exception.ResourceNotFoundException;
import com.kevin.financetracker.exception.ValidationException;
import com.kevin.financetracker.model.Account;
import com.kevin.financetracker.model.AccountType;
import com.kevin.financetracker.model.User;
import com.kevin.financetracker.repository.AccountRepository;
import com.kevin.financetracker.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    @Autowired
    public AccountService(AccountRepository accountRepository, UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    // Create a new account for a user
    public Account createAccount(Long userId, @Valid Account account) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Check if account name already exists for this user
        if (accountRepository.existsByUserAndName(user, account.getName())) {
            throw new DuplicateResourceException("Account name already exists for this user: " + account.getName());
        }

        account.setUser(user);
        return accountRepository.save(account);
    }

    // Get account by ID
    @Transactional(readOnly = true)
    public Optional<Account> getAccountById(Long id) {
        return accountRepository.findById(id);
    }

    // Get all accounts for a user
    @Transactional(readOnly = true)
    public List<Account> getAccountsByUser(Long userId) {
        return accountRepository.findByUserId(userId);
    }

    // Get accounts by type for a user
    @Transactional(readOnly = true)
    public List<Account> getAccountsByUserAndType(Long userId, AccountType type) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return accountRepository.findByUserAndType(user, type);
    }

    // Update account
    public Account updateAccount(Long accountId, @Valid Account accountDetails) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));

        // Update fields if provided
        if (accountDetails.getName() != null) {
            // Check if new name is unique for this user
            if (!account.getName().equals(accountDetails.getName()) &&
                    accountRepository.existsByUserAndName(account.getUser(), accountDetails.getName())) {
                throw new DuplicateResourceException("Account name already exists: " + accountDetails.getName());
            }
            account.setName(accountDetails.getName());
        }
        if (accountDetails.getDescription() != null) {
            account.setDescription(accountDetails.getDescription());
        }
        if (accountDetails.getType() != null) {
            account.setType(accountDetails.getType());
        }
        if (accountDetails.getCurrency() != null) {
            account.setCurrency(accountDetails.getCurrency());
        }

        return accountRepository.save(account);
    }

    // Delete account
    public void deleteAccount(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + id));

        // Check if account has transactions
        if (!account.getTransactions().isEmpty()) {
            throw new BusinessException("Cannot delete account with existing transactions");
        }

        accountRepository.delete(account);
    }

    // Deposit money to account
    public Account deposit(Long accountId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Deposit amount must be positive");
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));

        account.deposit(amount);
        return accountRepository.save(account);
    }

    // Withdraw money from account
    public Account withdraw(Long accountId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Withdrawal amount must be positive");
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));

        account.withdraw(amount);
        return accountRepository.save(account);
    }

    // Get total balance for a user
    @Transactional(readOnly = true)
    public BigDecimal getTotalBalanceByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return accountRepository.getTotalBalanceByUser(user).orElse(BigDecimal.ZERO);
    }

    // Transfer money between accounts
    @Transactional
    public void transfer(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Transfer amount must be positive");
        }

        Account fromAccount = accountRepository.findById(fromAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Source account not found with id: " + fromAccountId));
        Account toAccount = accountRepository.findById(toAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Destination account not found with id: " + toAccountId));

        if (fromAccount.getId().equals(toAccount.getId())) {
            throw new BusinessException("Cannot transfer to the same account");
        }

        // Withdraw from source account
        fromAccount.withdraw(amount);

        // Deposit to destination account
        toAccount.deposit(amount);

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);
    }

    // Get account by ID and verify user ownership
    @Transactional(readOnly = true)
    public Account getAccountByIdAndUserId(Long accountId, Long userId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));

        if (!account.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Account not found for this user");
        }

        return account;
    }
}