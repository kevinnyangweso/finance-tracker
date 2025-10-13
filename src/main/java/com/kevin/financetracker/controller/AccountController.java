package com.kevin.financetracker.controller;

import com.kevin.financetracker.model.Account;
import com.kevin.financetracker.model.AccountType;
import com.kevin.financetracker.model.CreateAccountRequest;
import com.kevin.financetracker.model.UpdateAccountRequest;
import com.kevin.financetracker.service.AccountService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/accounts")
public class AccountController {

    private final AccountService accountService;

    @Autowired
    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<Account> createAccount(@PathVariable Long userId,
                                                 @Valid @RequestBody CreateAccountRequest request) {
        // Convert DTO to Entity
        Account account = new Account();
        account.setName(request.getName());
        account.setType(request.getType());
        account.setBalance(request.getBalance());
        account.setCurrency(request.getCurrency());
        account.setDescription(request.getDescription());

        Account createdAccount = accountService.createAccount(userId, account);
        return new ResponseEntity<>(createdAccount, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Account>> getAccountsByUser(@PathVariable Long userId) {
        List<Account> accounts = accountService.getAccountsByUser(userId);
        return new ResponseEntity<>(accounts, HttpStatus.OK);
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<Account> getAccountById(@PathVariable Long userId, @PathVariable Long accountId) {
        Account account = accountService.getAccountByIdAndUserId(accountId, userId);
        return new ResponseEntity<>(account, HttpStatus.OK);
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<Account>> getAccountsByType(@PathVariable Long userId, @PathVariable AccountType type) {
        List<Account> accounts = accountService.getAccountsByUserAndType(userId, type);
        return new ResponseEntity<>(accounts, HttpStatus.OK);
    }

    @PutMapping("/{accountId}")
    public ResponseEntity<Account> updateAccount(@PathVariable Long userId, @PathVariable Long accountId,
                                                 @RequestBody UpdateAccountRequest request) {
        // Verify ownership
        accountService.getAccountByIdAndUserId(accountId, userId);

        // Convert DTO to Entity for update
        Account accountDetails = new Account();
        accountDetails.setName(request.getName());
        accountDetails.setType(request.getType());
        accountDetails.setBalance(request.getBalance());
        accountDetails.setCurrency(request.getCurrency());
        accountDetails.setDescription(request.getDescription());

        Account updatedAccount = accountService.updateAccount(accountId, accountDetails);
        return new ResponseEntity<>(updatedAccount, HttpStatus.OK);
    }

    @DeleteMapping("/{accountId}")
    public ResponseEntity<String> deleteAccount(@PathVariable Long userId, @PathVariable Long accountId) {
        // Verify ownership
        accountService.getAccountByIdAndUserId(accountId, userId);
        accountService.deleteAccount(accountId);
        return new ResponseEntity<>("Account deleted successfully", HttpStatus.OK);
    }

    @PostMapping("/{accountId}/deposit")
    public ResponseEntity<Account> deposit(@PathVariable Long userId, @PathVariable Long accountId,
                                           @Valid @RequestBody AmountRequest request) {
        // Verify ownership
        accountService.getAccountByIdAndUserId(accountId, userId);
        Account account = accountService.deposit(accountId, request.getAmount());
        return new ResponseEntity<>(account, HttpStatus.OK);
    }

    @PostMapping("/{accountId}/withdraw")
    public ResponseEntity<Account> withdraw(@PathVariable Long userId, @PathVariable Long accountId,
                                            @Valid @RequestBody AmountRequest request) {
        // Verify ownership
        accountService.getAccountByIdAndUserId(accountId, userId);
        Account account = accountService.withdraw(accountId, request.getAmount());
        return new ResponseEntity<>(account, HttpStatus.OK);
    }

    @PostMapping("/transfer")
    public ResponseEntity<String> transfer(@PathVariable Long userId,
                                           @Valid @RequestBody TransferRequest request) {
        // Verify both accounts belong to the user
        accountService.getAccountByIdAndUserId(request.getFromAccountId(), userId);
        accountService.getAccountByIdAndUserId(request.getToAccountId(), userId);

        accountService.transfer(request.getFromAccountId(), request.getToAccountId(), request.getAmount());
        return new ResponseEntity<>("Transfer completed successfully", HttpStatus.OK);
    }

    @GetMapping("/total-balance")
    public ResponseEntity<BigDecimal> getTotalBalance(@PathVariable Long userId) {
        BigDecimal totalBalance = accountService.getTotalBalanceByUser(userId);
        return new ResponseEntity<>(totalBalance, HttpStatus.OK);
    }

    // Request DTOs with validation
    public static class AmountRequest {
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
        @Digits(integer = 15, fraction = 2, message = "Amount must have up to 15 integer digits and 2 fraction digits")
        private BigDecimal amount;

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
    }

    public static class TransferRequest {
        @NotNull(message = "Source account ID is required")
        private Long fromAccountId;

        @NotNull(message = "Destination account ID is required")
        private Long toAccountId;

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
        @Digits(integer = 15, fraction = 2, message = "Amount must have up to 15 integer digits and 2 fraction digits")
        private BigDecimal amount;

        public Long getFromAccountId() { return fromAccountId; }
        public void setFromAccountId(Long fromAccountId) { this.fromAccountId = fromAccountId; }
        public Long getToAccountId() { return toAccountId; }
        public void setToAccountId(Long toAccountId) { this.toAccountId = toAccountId; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
    }
}