package com.kevin.financetracker.controller;

import com.kevin.financetracker.model.Account;
import com.kevin.financetracker.model.AccountType;
import com.kevin.financetracker.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users/{userId}/accounts")
public class AccountController {

    private final AccountService accountService;

    @Autowired
    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    // Create a new account for user
    @PostMapping
    public ResponseEntity<?> createAccount(@PathVariable Long userId, @Valid @RequestBody Account account) {
        try {
            Account createdAccount = accountService.createAccount(userId, account);
            return new ResponseEntity<>(createdAccount, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // Get all accounts for user
    @GetMapping
    public ResponseEntity<List<Account>> getAccountsByUser(@PathVariable Long userId) {
        List<Account> accounts = accountService.getAccountsByUser(userId);
        return new ResponseEntity<>(accounts, HttpStatus.OK);
    }

    // Get account by ID
    @GetMapping("/{accountId}")
    public ResponseEntity<?> getAccountById(@PathVariable Long userId, @PathVariable Long accountId) {
        Optional<Account> account = accountService.getAccountById(accountId);
        if (account.isPresent() && account.get().getUser().getId().equals(userId)) {
            return new ResponseEntity<>(account.get(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Account not found", HttpStatus.NOT_FOUND);
        }
    }

    // Get accounts by type for user
    @GetMapping("/type/{type}")
    public ResponseEntity<List<Account>> getAccountsByType(@PathVariable Long userId, @PathVariable AccountType type) {
        List<Account> accounts = accountService.getAccountsByUserAndType(userId, type);
        return new ResponseEntity<>(accounts, HttpStatus.OK);
    }

    // Update account
    @PutMapping("/{accountId}")
    public ResponseEntity<?> updateAccount(@PathVariable Long userId, @PathVariable Long accountId,
                                           @Valid @RequestBody Account accountDetails) {
        try {
            Account updatedAccount = accountService.updateAccount(accountId, accountDetails);
            return new ResponseEntity<>(updatedAccount, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    // Delete account
    @DeleteMapping("/{accountId}")
    public ResponseEntity<?> deleteAccount(@PathVariable Long userId, @PathVariable Long accountId) {
        try {
            accountService.deleteAccount(accountId);
            return new ResponseEntity<>("Account deleted successfully", HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (IllegalStateException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    // Deposit to account
    @PostMapping("/{accountId}/deposit")
    public ResponseEntity<?> deposit(@PathVariable Long userId, @PathVariable Long accountId,
                                     @RequestBody AmountRequest request) {
        try {
            Account account = accountService.deposit(accountId, request.getAmount());
            return new ResponseEntity<>(account, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // Withdraw from account
    @PostMapping("/{accountId}/withdraw")
    public ResponseEntity<?> withdraw(@PathVariable Long userId, @PathVariable Long accountId,
                                      @RequestBody AmountRequest request) {
        try {
            Account account = accountService.withdraw(accountId, request.getAmount());
            return new ResponseEntity<>(account, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // Transfer between accounts
    @PostMapping("/transfer")
    public ResponseEntity<?> transfer(@PathVariable Long userId, @RequestBody TransferRequest request) {
        try {
            accountService.transfer(request.getFromAccountId(), request.getToAccountId(), request.getAmount());
            return new ResponseEntity<>("Transfer completed successfully", HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // Get total balance for user
    @GetMapping("/total-balance")
    public ResponseEntity<BigDecimal> getTotalBalance(@PathVariable Long userId) {
        BigDecimal totalBalance = accountService.getTotalBalanceByUser(userId);
        return new ResponseEntity<>(totalBalance, HttpStatus.OK);
    }

    // DTOs for request bodies
    public static class AmountRequest {
        private BigDecimal amount;

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
    }

    public static class TransferRequest {
        private Long fromAccountId;
        private Long toAccountId;
        private BigDecimal amount;

        // Getters and setters
        public Long getFromAccountId() { return fromAccountId; }
        public void setFromAccountId(Long fromAccountId) { this.fromAccountId = fromAccountId; }
        public Long getToAccountId() { return toAccountId; }
        public void setToAccountId(Long toAccountId) { this.toAccountId = toAccountId; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
    }
}