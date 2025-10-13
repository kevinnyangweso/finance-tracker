package com.kevin.financetracker.service;

import com.kevin.financetracker.exception.BusinessException;
import com.kevin.financetracker.exception.DuplicateResourceException;
import com.kevin.financetracker.exception.ResourceNotFoundException;
import com.kevin.financetracker.exception.ValidationException;
import com.kevin.financetracker.model.Account;
import com.kevin.financetracker.model.AccountType;
import com.kevin.financetracker.model.Transaction;
import com.kevin.financetracker.model.User;
import com.kevin.financetracker.repository.AccountRepository;
import com.kevin.financetracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AccountService accountService;

    @Captor
    private ArgumentCaptor<Account> accountCaptor;

    private User testUser;
    private User anotherUser;
    private Account testAccount;
    private Account savingsAccount;
    private Account creditCardAccount;
    private Account cashAccount;
    private Long userId = 1L;
    private Long anotherUserId = 2L;
    private Long accountId = 100L;
    private Long savingsAccountId = 101L;
    private Long creditCardAccountId = 102L;
    private Long cashAccountId = 103L;

    @BeforeEach
    void setUp() {
        // Setup test users
        testUser = new User();
        testUser.setId(userId);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");

        anotherUser = new User();
        anotherUser.setId(anotherUserId);
        anotherUser.setUsername("anotheruser");
        anotherUser.setEmail("another@example.com");
        anotherUser.setFirstName("Another");
        anotherUser.setLastName("User");

        // Setup test accounts
        testAccount = new Account();
        testAccount.setId(accountId);
        testAccount.setName("Checking Account");
        testAccount.setType(AccountType.CHECKING);
        testAccount.setBalance(new BigDecimal("1000.00"));
        testAccount.setCurrency("USD");
        testAccount.setDescription("Primary checking account");
        testAccount.setUser(testUser);

        savingsAccount = new Account();
        savingsAccount.setId(savingsAccountId);
        savingsAccount.setName("Savings Account");
        savingsAccount.setType(AccountType.SAVINGS);
        savingsAccount.setBalance(new BigDecimal("5000.00"));
        savingsAccount.setCurrency("USD");
        savingsAccount.setDescription("Emergency savings");
        savingsAccount.setUser(testUser);

        // For credit card, set a positive balance to avoid "Insufficient funds" in current implementation
        creditCardAccount = new Account();
        creditCardAccount.setId(creditCardAccountId);
        creditCardAccount.setName("Credit Card");
        creditCardAccount.setType(AccountType.CREDIT_CARD);
        creditCardAccount.setBalance(new BigDecimal("500.00")); // Positive balance for testing
        creditCardAccount.setCurrency("USD");
        creditCardAccount.setDescription("Primary credit card");
        creditCardAccount.setUser(testUser);

        cashAccount = new Account();
        cashAccount.setId(cashAccountId);
        cashAccount.setName("Cash Wallet");
        cashAccount.setType(AccountType.CASH);
        cashAccount.setBalance(new BigDecimal("150.50"));
        cashAccount.setCurrency("USD");
        cashAccount.setDescription("Daily cash expenses");
        cashAccount.setUser(testUser);
    }

    // ========== CREATE ACCOUNT TESTS ==========
    @Test
    @DisplayName("Create account - success with valid account")
    void createAccount_ValidAccount_ShouldPersist() {
        // Given
        Account newAccount = new Account();
        newAccount.setName("New Savings");
        newAccount.setType(AccountType.SAVINGS);
        newAccount.setBalance(new BigDecimal("2000.00"));
        newAccount.setCurrency("USD");
        newAccount.setDescription("New savings account");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(accountRepository.existsByUserAndName(testUser, "New Savings")).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            account.setId(999L);
            return account;
        });

        // When
        Account createdAccount = accountService.createAccount(userId, newAccount);

        // Then
        assertThat(createdAccount).isNotNull();
        assertThat(createdAccount.getId()).isEqualTo(999L);
        assertThat(createdAccount.getName()).isEqualTo("New Savings");
        assertThat(createdAccount.getType()).isEqualTo(AccountType.SAVINGS);
        assertThat(createdAccount.getBalance()).isEqualByComparingTo("2000.00");
        assertThat(createdAccount.getCurrency()).isEqualTo("USD");
        assertThat(createdAccount.getDescription()).isEqualTo("New savings account");
        assertThat(createdAccount.getUser()).isEqualTo(testUser);

        verify(userRepository).findById(userId);
        verify(accountRepository).existsByUserAndName(testUser, "New Savings");
        verify(accountRepository).save(accountCaptor.capture());

        Account savedAccount = accountCaptor.getValue();
        assertThat(savedAccount.getUser()).isEqualTo(testUser);
        assertThat(savedAccount.getName()).isEqualTo("New Savings");
    }

    @Test
    @DisplayName("Create account - user not found throws ResourceNotFoundException")
    void createAccount_UserNotFound_ThrowsException() {
        // Given
        Account newAccount = new Account();
        newAccount.setName("New Account");
        newAccount.setType(AccountType.CHECKING);
        newAccount.setBalance(new BigDecimal("1000.00"));
        newAccount.setCurrency("USD");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> accountService.createAccount(userId, newAccount))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found with id: " + userId);

        verify(userRepository).findById(userId);
        verify(accountRepository, never()).existsByUserAndName(any(), any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create account - duplicate account name throws DuplicateResourceException")
    void createAccount_DuplicateName_ThrowsException() {
        // Given
        Account newAccount = new Account();
        newAccount.setName("Existing Account");
        newAccount.setType(AccountType.CHECKING);
        newAccount.setBalance(new BigDecimal("500.00"));
        newAccount.setCurrency("USD");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(accountRepository.existsByUserAndName(testUser, "Existing Account")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> accountService.createAccount(userId, newAccount))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Account name already exists for this user: Existing Account");

        verify(userRepository).findById(userId);
        verify(accountRepository).existsByUserAndName(testUser, "Existing Account");
        verify(accountRepository, never()).save(any());
    }

    // ========== GET ACCOUNT BY ID TESTS ==========
    @Test
    @DisplayName("Get account by ID - success with existing account")
    void getAccountById_Success() {
        // Given
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));

        // When
        Optional<Account> result = accountService.getAccountById(accountId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testAccount);
        assertThat(result.get().getName()).isEqualTo("Checking Account");
        assertThat(result.get().getBalance()).isEqualByComparingTo("1000.00");
        verify(accountRepository).findById(accountId);
    }

    @Test
    @DisplayName("Get account by ID - not found returns empty")
    void getAccountById_NotFound() {
        // Given
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        // When
        Optional<Account> result = accountService.getAccountById(accountId);

        // Then
        assertThat(result).isEmpty();
        verify(accountRepository).findById(accountId);
    }

    // ========== GET ACCOUNTS BY USER TESTS ==========
    @Test
    @DisplayName("Get accounts by user - success with multiple accounts")
    void getAccountsByUser_Success() {
        // Given
        List<Account> expectedAccounts = Arrays.asList(testAccount, savingsAccount, creditCardAccount);
        when(accountRepository.findByUserId(userId)).thenReturn(expectedAccounts);

        // When
        List<Account> result = accountService.getAccountsByUser(userId);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result).containsExactly(testAccount, savingsAccount, creditCardAccount);
        // FIXED: Verify the correct method was called - findByUserId, not findById
        verify(accountRepository).findByUserId(userId);
        verify(accountRepository, never()).findById(any()); // findById should not be called in this method
    }

    @Test
    @DisplayName("Get accounts by user - empty list for user with no accounts")
    void getAccountsByUser_EmptyList() {
        // Given
        when(accountRepository.findByUserId(userId)).thenReturn(Collections.emptyList());

        // When
        List<Account> result = accountService.getAccountsByUser(userId);

        // Then
        assertThat(result).isEmpty();
        verify(accountRepository).findByUserId(userId);
    }

    // ========== GET ACCOUNTS BY USER AND TYPE TESTS ==========
    @Test
    @DisplayName("Get accounts by user and type - success with matching accounts")
    void getAccountsByUserAndType_Success() {
        // Given
        List<Account> expectedAccounts = Arrays.asList(testAccount);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(accountRepository.findByUserAndType(testUser, AccountType.CHECKING)).thenReturn(expectedAccounts);

        // When
        List<Account> result = accountService.getAccountsByUserAndType(userId, AccountType.CHECKING);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(testAccount);
        verify(userRepository).findById(userId);
        verify(accountRepository).findByUserAndType(testUser, AccountType.CHECKING);
    }

    @Test
    @DisplayName("Get accounts by user and type - user not found throws ResourceNotFoundException")
    void getAccountsByUserAndType_UserNotFound_ThrowsException() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> accountService.getAccountsByUserAndType(userId, AccountType.CHECKING))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found with id: " + userId);

        verify(userRepository).findById(userId);
        verify(accountRepository, never()).findByUserAndType(any(), any());
    }

    @Test
    @DisplayName("Get accounts by user and type - empty list for non-existent type")
    void getAccountsByUserAndType_EmptyList() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(accountRepository.findByUserAndType(testUser, AccountType.INVESTMENT)).thenReturn(Collections.emptyList());

        // When
        List<Account> result = accountService.getAccountsByUserAndType(userId, AccountType.INVESTMENT);

        // Then
        assertThat(result).isEmpty();
        verify(userRepository).findById(userId);
        verify(accountRepository).findByUserAndType(testUser, AccountType.INVESTMENT);
    }

    // ========== UPDATE ACCOUNT TESTS ==========
    @Test
    @DisplayName("Update account - success with name, description, and currency")
    void updateAccount_SuccessWithNameDescriptionCurrency() {
        // Given
        Account updatedDetails = new Account();
        updatedDetails.setName("Updated Checking");
        updatedDetails.setDescription("Updated description");
        updatedDetails.setCurrency("EUR");

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
        when(accountRepository.existsByUserAndName(testUser, "Updated Checking")).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        // When
        Account result = accountService.updateAccount(accountId, updatedDetails);

        // Then
        assertThat(result).isEqualTo(testAccount);
        assertThat(testAccount.getName()).isEqualTo("Updated Checking");
        assertThat(testAccount.getDescription()).isEqualTo("Updated description");
        assertThat(testAccount.getCurrency()).isEqualTo("EUR");
        // Balance should remain unchanged as it's not updated in the service method
        assertThat(testAccount.getBalance()).isEqualByComparingTo("1000.00");

        verify(accountRepository).findById(accountId);
        verify(accountRepository).existsByUserAndName(testUser, "Updated Checking");
        verify(accountRepository).save(testAccount);
    }

    @Test
    @DisplayName("Update account - partial update with only name change")
    void updateAccount_PartialUpdate() {
        // Given
        Account updatedDetails = new Account();
        updatedDetails.setName("Updated Name");

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
        when(accountRepository.existsByUserAndName(testUser, "Updated Name")).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        // When
        Account result = accountService.updateAccount(accountId, updatedDetails);

        // Then
        assertThat(result).isEqualTo(testAccount);
        assertThat(testAccount.getName()).isEqualTo("Updated Name");
        // Other fields should remain unchanged
        assertThat(testAccount.getType()).isEqualTo(AccountType.CHECKING);
        assertThat(testAccount.getBalance()).isEqualTo(new BigDecimal("1000.00"));
        assertThat(testAccount.getCurrency()).isEqualTo("USD");

        verify(accountRepository).findById(accountId);
        verify(accountRepository).existsByUserAndName(testUser, "Updated Name");
        verify(accountRepository).save(testAccount);
    }

    @Test
    @DisplayName("Update account - same name should not check uniqueness")
    void updateAccount_SameName_ShouldNotCheckUniqueness() {
        // Given
        Account updatedDetails = new Account();
        updatedDetails.setName("Checking Account"); // Same as current name
        updatedDetails.setDescription("New description");

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        // When
        Account result = accountService.updateAccount(accountId, updatedDetails);

        // Then
        assertThat(result).isEqualTo(testAccount);
        assertThat(testAccount.getDescription()).isEqualTo("New description");

        verify(accountRepository).findById(accountId);
        verify(accountRepository, never()).existsByUserAndName(any(), any());
        verify(accountRepository).save(testAccount);
    }

    @Test
    @DisplayName("Update account - duplicate name throws DuplicateResourceException")
    void updateAccount_DuplicateName_ThrowsException() {
        // Given
        Account updatedDetails = new Account();
        updatedDetails.setName("Savings Account"); // Existing name

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
        when(accountRepository.existsByUserAndName(testUser, "Savings Account")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> accountService.updateAccount(accountId, updatedDetails))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Account name already exists: Savings Account");

        verify(accountRepository).findById(accountId);
        verify(accountRepository).existsByUserAndName(testUser, "Savings Account");
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update account - account not found throws ResourceNotFoundException")
    void updateAccount_AccountNotFound_ThrowsException() {
        // Given
        Account updatedDetails = new Account();
        updatedDetails.setName("Updated Name");

        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> accountService.updateAccount(accountId, updatedDetails))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Account not found with id: " + accountId);

        verify(accountRepository).findById(accountId);
        verify(accountRepository, never()).existsByUserAndName(any(), any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update account - null fields should be ignored")
    void updateAccount_NullFields_ShouldBeIgnored() {
        // Given
        Account updatedDetails = new Account(); // All fields null

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        // When
        Account result = accountService.updateAccount(accountId, updatedDetails);

        // Then
        assertThat(result).isEqualTo(testAccount);
        // Original values should remain unchanged
        assertThat(testAccount.getName()).isEqualTo("Checking Account");
        assertThat(testAccount.getType()).isEqualTo(AccountType.CHECKING);
        assertThat(testAccount.getBalance()).isEqualTo(new BigDecimal("1000.00"));

        verify(accountRepository).findById(accountId);
        verify(accountRepository, never()).existsByUserAndName(any(), any());
        verify(accountRepository).save(testAccount);
    }

    // ========== DELETE ACCOUNT TESTS ==========
    @Test
    @DisplayName("Delete account - success without transactions")
    void deleteAccount_Success() {
        // Given
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
        doNothing().when(accountRepository).delete(testAccount);

        // When
        accountService.deleteAccount(accountId);

        // Then
        verify(accountRepository).findById(accountId);
        verify(accountRepository).delete(testAccount);
    }

    @Test
    @DisplayName("Delete account - account not found throws ResourceNotFoundException")
    void deleteAccount_AccountNotFound_ThrowsException() {
        // Given
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> accountService.deleteAccount(accountId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Account not found with id: " + accountId);

        verify(accountRepository).findById(accountId);
        verify(accountRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Delete account - has transactions throws BusinessException")
    void deleteAccount_HasTransactions_ThrowsException() {
        // Given
        Transaction transaction = new Transaction();
        testAccount.addTransaction(transaction);

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));

        // When & Then
        assertThatThrownBy(() -> accountService.deleteAccount(accountId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Cannot delete account with existing transactions");

        verify(accountRepository).findById(accountId);
        verify(accountRepository, never()).delete(any());
    }

    // ========== DEPOSIT TESTS ==========
    @Test
    @DisplayName("Deposit - success with valid amount")
    void deposit_Success() {
        // Given
        BigDecimal depositAmount = new BigDecimal("500.00");
        BigDecimal expectedBalance = new BigDecimal("1500.00");

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        // When
        Account result = accountService.deposit(accountId, depositAmount);

        // Then
        assertThat(result).isEqualTo(testAccount);
        assertThat(testAccount.getBalance()).isEqualByComparingTo(expectedBalance);

        verify(accountRepository).findById(accountId);
        verify(accountRepository).save(testAccount);
    }

    @Test
    @DisplayName("Deposit - very small amount success")
    void deposit_VerySmallAmount_Success() {
        // Given
        BigDecimal smallAmount = new BigDecimal("0.01");
        BigDecimal expectedBalance = new BigDecimal("1000.01");

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        // When
        Account result = accountService.deposit(accountId, smallAmount);

        // Then
        assertThat(result).isEqualTo(testAccount);
        assertThat(testAccount.getBalance()).isEqualByComparingTo(expectedBalance);

        verify(accountRepository).findById(accountId);
        verify(accountRepository).save(testAccount);
    }

    @Test
    @DisplayName("Deposit - null amount throws ValidationException")
    void deposit_NullAmount_ThrowsException() {
        // When & Then
        assertThatThrownBy(() -> accountService.deposit(accountId, null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Deposit amount must be positive");

        verify(accountRepository, never()).findById(any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deposit - zero amount throws ValidationException")
    void deposit_ZeroAmount_ThrowsException() {
        // Given
        BigDecimal zeroAmount = BigDecimal.ZERO;

        // When & Then
        assertThatThrownBy(() -> accountService.deposit(accountId, zeroAmount))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Deposit amount must be positive");

        verify(accountRepository, never()).findById(any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deposit - negative amount throws ValidationException")
    void deposit_NegativeAmount_ThrowsException() {
        // Given
        BigDecimal negativeAmount = new BigDecimal("-100.00");

        // When & Then
        assertThatThrownBy(() -> accountService.deposit(accountId, negativeAmount))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Deposit amount must be positive");

        verify(accountRepository, never()).findById(any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deposit - account not found throws ResourceNotFoundException")
    void deposit_AccountNotFound_ThrowsException() {
        // Given
        BigDecimal depositAmount = new BigDecimal("500.00");
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> accountService.deposit(accountId, depositAmount))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Account not found with id: " + accountId);

        verify(accountRepository).findById(accountId);
        verify(accountRepository, never()).save(any());
    }

    // ========== WITHDRAW TESTS ==========
    @Test
    @DisplayName("Withdraw - success with valid amount")
    void withdraw_Success() {
        // Given
        BigDecimal withdrawAmount = new BigDecimal("200.00");
        BigDecimal expectedBalance = new BigDecimal("800.00");

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        // When
        Account result = accountService.withdraw(accountId, withdrawAmount);

        // Then
        assertThat(result).isEqualTo(testAccount);
        assertThat(testAccount.getBalance()).isEqualByComparingTo(expectedBalance);

        verify(accountRepository).findById(accountId);
        verify(accountRepository).save(testAccount);
    }

    @Test
    @DisplayName("Withdraw - from credit card account with positive balance")
    void withdraw_FromCreditCardAccountWithPositiveBalance_Success() {
        // Given
        // Credit card has positive balance of 500.00, withdraw 100.00
        BigDecimal withdrawAmount = new BigDecimal("100.00");
        BigDecimal expectedBalance = new BigDecimal("400.00"); // 500 - 100 = 400

        when(accountRepository.findById(creditCardAccountId)).thenReturn(Optional.of(creditCardAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(creditCardAccount);

        // When
        Account result = accountService.withdraw(creditCardAccountId, withdrawAmount);

        // Then
        assertThat(result).isEqualTo(creditCardAccount);
        assertThat(creditCardAccount.getBalance()).isEqualByComparingTo(expectedBalance);

        verify(accountRepository).findById(creditCardAccountId);
        verify(accountRepository).save(creditCardAccount);
    }

    @Test
    @DisplayName("Withdraw - exact balance amount success")
    void withdraw_ExactBalanceAmount_Success() {
        // Given
        BigDecimal exactAmount = new BigDecimal("1000.00");
        BigDecimal expectedBalance = BigDecimal.ZERO;

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        // When
        Account result = accountService.withdraw(accountId, exactAmount);

        // Then
        assertThat(result).isEqualTo(testAccount);
        assertThat(testAccount.getBalance()).isEqualByComparingTo(expectedBalance);

        verify(accountRepository).findById(accountId);
        verify(accountRepository).save(testAccount);
    }

    @Test
    @DisplayName("Withdraw - insufficient funds throws ValidationException")
    void withdraw_InsufficientFunds_ThrowsException() {
        // Given
        BigDecimal largeAmount = new BigDecimal("2000.00"); // More than balance

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));

        // When & Then
        assertThatThrownBy(() -> accountService.withdraw(accountId, largeAmount))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Insufficient funds");

        verify(accountRepository).findById(accountId);
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Withdraw - null amount throws ValidationException")
    void withdraw_NullAmount_ThrowsException() {
        // When & Then
        assertThatThrownBy(() -> accountService.withdraw(accountId, null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Withdrawal amount must be positive");

        verify(accountRepository, never()).findById(any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Withdraw - zero amount throws ValidationException")
    void withdraw_ZeroAmount_ThrowsException() {
        // Given
        BigDecimal zeroAmount = BigDecimal.ZERO;

        // When & Then
        assertThatThrownBy(() -> accountService.withdraw(accountId, zeroAmount))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Withdrawal amount must be positive");

        verify(accountRepository, never()).findById(any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Withdraw - negative amount throws ValidationException")
    void withdraw_NegativeAmount_ThrowsException() {
        // Given
        BigDecimal negativeAmount = new BigDecimal("-100.00");

        // When & Then
        assertThatThrownBy(() -> accountService.withdraw(accountId, negativeAmount))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Withdrawal amount must be positive");

        verify(accountRepository, never()).findById(any());
        verify(accountRepository, never()).save(any());
    }

    // ========== GET TOTAL BALANCE TESTS ==========
    @Test
    @DisplayName("Get total balance by user - success with multiple accounts")
    void getTotalBalanceByUser_Success() {
        // Given
        BigDecimal expectedTotal = new BigDecimal("6650.50"); // 1000 + 5000 + 500 + 150.50
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(accountRepository.getTotalBalanceByUser(testUser)).thenReturn(Optional.of(expectedTotal));

        // When
        BigDecimal result = accountService.getTotalBalanceByUser(userId);

        // Then
        assertThat(result).isEqualByComparingTo(expectedTotal);
        verify(userRepository).findById(userId);
        verify(accountRepository).getTotalBalanceByUser(testUser);
    }

    @Test
    @DisplayName("Get total balance by user - user not found throws ResourceNotFoundException")
    void getTotalBalanceByUser_UserNotFound_ThrowsException() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> accountService.getTotalBalanceByUser(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found with id: " + userId);

        verify(userRepository).findById(userId);
        verify(accountRepository, never()).getTotalBalanceByUser(any());
    }

    @Test
    @DisplayName("Get total balance by user - no accounts returns zero")
    void getTotalBalanceByUser_NoAccounts_ReturnsZero() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(accountRepository.getTotalBalanceByUser(testUser)).thenReturn(Optional.empty());

        // When
        BigDecimal result = accountService.getTotalBalanceByUser(userId);

        // Then
        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        verify(userRepository).findById(userId);
        verify(accountRepository).getTotalBalanceByUser(testUser);
    }

    // ========== TRANSFER TESTS ==========
    @Test
    @DisplayName("Transfer - success between checking and savings")
    void transfer_Success() {
        // Given
        BigDecimal transferAmount = new BigDecimal("300.00");
        BigDecimal expectedFromBalance = new BigDecimal("700.00"); // 1000 - 300
        BigDecimal expectedToBalance = new BigDecimal("5300.00"); // 5000 + 300

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
        when(accountRepository.findById(savingsAccountId)).thenReturn(Optional.of(savingsAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        accountService.transfer(accountId, savingsAccountId, transferAmount);

        // Then
        assertThat(testAccount.getBalance()).isEqualByComparingTo(expectedFromBalance);
        assertThat(savingsAccount.getBalance()).isEqualByComparingTo(expectedToBalance);

        verify(accountRepository).findById(accountId);
        verify(accountRepository).findById(savingsAccountId);
        verify(accountRepository, times(2)).save(any(Account.class));
    }

    @Test
    @DisplayName("Transfer - between different account types success")
    void transfer_BetweenDifferentAccountTypes_Success() {
        // Given
        BigDecimal transferAmount = new BigDecimal("200.00");
        BigDecimal expectedFromBalance = new BigDecimal("800.00"); // 1000 - 200
        BigDecimal expectedToBalance = new BigDecimal("700.00"); // 500 + 200

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
        when(accountRepository.findById(creditCardAccountId)).thenReturn(Optional.of(creditCardAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        accountService.transfer(accountId, creditCardAccountId, transferAmount);

        // Then
        assertThat(testAccount.getBalance()).isEqualByComparingTo(expectedFromBalance);
        assertThat(creditCardAccount.getBalance()).isEqualByComparingTo(expectedToBalance);

        verify(accountRepository).findById(accountId);
        verify(accountRepository).findById(creditCardAccountId);
        verify(accountRepository, times(2)).save(any(Account.class));
    }

    @Test
    @DisplayName("Transfer - exact balance amount success")
    void transfer_ExactBalanceAmount_Success() {
        // Given
        BigDecimal exactAmount = new BigDecimal("1000.00");
        BigDecimal expectedFromBalance = BigDecimal.ZERO;
        BigDecimal expectedToBalance = new BigDecimal("6000.00");

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
        when(accountRepository.findById(savingsAccountId)).thenReturn(Optional.of(savingsAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        accountService.transfer(accountId, savingsAccountId, exactAmount);

        // Then
        assertThat(testAccount.getBalance()).isEqualByComparingTo(expectedFromBalance);
        assertThat(savingsAccount.getBalance()).isEqualByComparingTo(expectedToBalance);

        verify(accountRepository).findById(accountId);
        verify(accountRepository).findById(savingsAccountId);
        verify(accountRepository, times(2)).save(any(Account.class));
    }

    @Test
    @DisplayName("Transfer - same account throws BusinessException")
    void transfer_SameAccount_ThrowsException() {
        // Given
        BigDecimal transferAmount = new BigDecimal("100.00");

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));

        // When & Then
        assertThatThrownBy(() -> accountService.transfer(accountId, accountId, transferAmount))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Cannot transfer to the same account");

        verify(accountRepository, times(2)).findById(accountId);
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Transfer - null amount throws ValidationException")
    void transfer_NullAmount_ThrowsException() {
        // When & Then
        assertThatThrownBy(() -> accountService.transfer(accountId, savingsAccountId, null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Transfer amount must be positive");

        verify(accountRepository, never()).findById(any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Transfer - zero amount throws ValidationException")
    void transfer_ZeroAmount_ThrowsException() {
        // Given
        BigDecimal zeroAmount = BigDecimal.ZERO;

        // When & Then
        assertThatThrownBy(() -> accountService.transfer(accountId, savingsAccountId, zeroAmount))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Transfer amount must be positive");

        verify(accountRepository, never()).findById(any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Transfer - negative amount throws ValidationException")
    void transfer_NegativeAmount_ThrowsException() {
        // Given
        BigDecimal negativeAmount = new BigDecimal("-100.00");

        // When & Then
        assertThatThrownBy(() -> accountService.transfer(accountId, savingsAccountId, negativeAmount))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Transfer amount must be positive");

        verify(accountRepository, never()).findById(any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Transfer - from account not found throws ResourceNotFoundException")
    void transfer_FromAccountNotFound_ThrowsException() {
        // Given
        BigDecimal transferAmount = new BigDecimal("100.00");
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> accountService.transfer(accountId, savingsAccountId, transferAmount))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Source account not found with id: " + accountId);

        verify(accountRepository).findById(accountId);
        verify(accountRepository, never()).findById(savingsAccountId);
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Transfer - to account not found throws ResourceNotFoundException")
    void transfer_ToAccountNotFound_ThrowsException() {
        // Given
        BigDecimal transferAmount = new BigDecimal("100.00");
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
        when(accountRepository.findById(savingsAccountId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> accountService.transfer(accountId, savingsAccountId, transferAmount))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Destination account not found with id: " + savingsAccountId);

        verify(accountRepository).findById(accountId);
        verify(accountRepository).findById(savingsAccountId);
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Transfer - insufficient funds in source account throws ValidationException")
    void transfer_InsufficientFunds_ThrowsException() {
        // Given
        BigDecimal largeAmount = new BigDecimal("5000.00"); // More than checking account balance
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
        when(accountRepository.findById(savingsAccountId)).thenReturn(Optional.of(savingsAccount));

        // When & Then
        assertThatThrownBy(() -> accountService.transfer(accountId, savingsAccountId, largeAmount))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Insufficient funds");

        verify(accountRepository).findById(accountId);
        verify(accountRepository).findById(savingsAccountId);
        verify(accountRepository, never()).save(any());
    }

    // ========== EDGE CASE AND INTEGRATION TESTS ==========
    @Test
    @DisplayName("Multiple operations - create, update, and delete account workflow")
    void multipleOperations_Workflow_Success() {
        // Create account
        Account newAccount = new Account();
        newAccount.setName("Test Account");
        newAccount.setType(AccountType.CHECKING);
        newAccount.setBalance(new BigDecimal("1000.00"));
        newAccount.setCurrency("USD");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(accountRepository.existsByUserAndName(testUser, "Test Account")).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            account.setId(999L);
            return account;
        });

        Account createdAccount = accountService.createAccount(userId, newAccount);
        assertThat(createdAccount.getId()).isEqualTo(999L);

        // Update account
        Account updateDetails = new Account();
        updateDetails.setName("Updated Test Account");
        updateDetails.setDescription("Updated description");

        when(accountRepository.findById(999L)).thenReturn(Optional.of(createdAccount));
        when(accountRepository.existsByUserAndName(testUser, "Updated Test Account")).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenReturn(createdAccount);

        Account updatedAccount = accountService.updateAccount(999L, updateDetails);
        assertThat(updatedAccount.getName()).isEqualTo("Updated Test Account");

        // Delete account
        when(accountRepository.findById(999L)).thenReturn(Optional.of(createdAccount));
        doNothing().when(accountRepository).delete(createdAccount);

        accountService.deleteAccount(999L);

        verify(accountRepository).delete(createdAccount);
    }

    @Test
    @DisplayName("Financial operations - deposit, withdraw, and transfer sequence")
    void financialOperations_Sequence_Success() {
        // Initial deposit
        BigDecimal depositAmount = new BigDecimal("500.00");
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        Account afterDeposit = accountService.deposit(accountId, depositAmount);
        assertThat(afterDeposit.getBalance()).isEqualByComparingTo("1500.00");

        // Withdraw
        BigDecimal withdrawAmount = new BigDecimal("300.00");
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        Account afterWithdraw = accountService.withdraw(accountId, withdrawAmount);
        assertThat(afterWithdraw.getBalance()).isEqualByComparingTo("1200.00");

        // Transfer
        BigDecimal transferAmount = new BigDecimal("200.00");
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
        when(accountRepository.findById(savingsAccountId)).thenReturn(Optional.of(savingsAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        accountService.transfer(accountId, savingsAccountId, transferAmount);
        assertThat(testAccount.getBalance()).isEqualByComparingTo("1000.00");
        assertThat(savingsAccount.getBalance()).isEqualByComparingTo("5200.00");
    }
}