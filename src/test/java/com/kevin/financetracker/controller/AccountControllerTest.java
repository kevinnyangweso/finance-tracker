package com.kevin.financetracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kevin.financetracker.exception.ResourceNotFoundException;
import com.kevin.financetracker.model.*;
import com.kevin.financetracker.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AccountService accountService;

    @InjectMocks
    private AccountController accountController;

    private ObjectMapper objectMapper;

    private User testUser;
    private User differentUser;
    private Account testAccount;
    private Account savingsAccount;
    private Account creditCardAccount;
    private Long userId = 1L;
    private Long differentUserId = 2L;
    private Long accountId = 100L;
    private Long savingsAccountId = 101L;
    private Long creditCardAccountId = 102L;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(accountController)
                .setControllerAdvice(new com.kevin.financetracker.exception.GlobalExceptionHandler())
                .build();

        // Setup test data
        testUser = new User();
        testUser.setId(userId);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        differentUser = new User();
        differentUser.setId(differentUserId);
        differentUser.setUsername("differentuser");
        differentUser.setEmail("different@example.com");

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

        creditCardAccount = new Account();
        creditCardAccount.setId(creditCardAccountId);
        creditCardAccount.setName("Credit Card");
        creditCardAccount.setType(AccountType.CREDIT_CARD);
        creditCardAccount.setBalance(new BigDecimal("-500.00"));
        creditCardAccount.setCurrency("USD");
        creditCardAccount.setDescription("Primary credit card");
        creditCardAccount.setUser(testUser);
    }

    // ========== UNIT TESTS (Direct Controller Method Calls) ==========

    @Test
    @DisplayName("Create account - success - unit test")
    void createAccount_Success_UnitTest() {
        // Given - Use CreateAccountRequest instead of Account
        CreateAccountRequest request = new CreateAccountRequest();
        request.setName("Test Account");
        request.setType(AccountType.SAVINGS);
        request.setBalance(new BigDecimal("1000.00"));
        request.setCurrency("USD");
        request.setDescription("Test account");

        when(accountService.createAccount(eq(userId), any(Account.class))).thenReturn(testAccount);

        // When
        ResponseEntity<Account> response = accountController.createAccount(userId, request);

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(testAccount, response.getBody());
        verify(accountService).createAccount(eq(userId), any(Account.class));
    }

    @Test
    @DisplayName("Get accounts by user - success - unit test")
    void getAccountsByUser_Success_UnitTest() {
        // Given
        List<Account> accounts = Arrays.asList(testAccount, savingsAccount);
        when(accountService.getAccountsByUser(userId)).thenReturn(accounts);

        // When
        ResponseEntity<List<Account>> response = accountController.getAccountsByUser(userId);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(accounts, response.getBody());
        verify(accountService).getAccountsByUser(userId);
    }

    @Test
    @DisplayName("Get account by ID - success - unit test")
    void getAccountById_Success_UnitTest() {
        // Given
        when(accountService.getAccountByIdAndUserId(accountId, userId)).thenReturn(testAccount);

        // When
        ResponseEntity<Account> response = accountController.getAccountById(userId, accountId);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testAccount, response.getBody());
        verify(accountService).getAccountByIdAndUserId(accountId, userId);
    }

    @Test
    @DisplayName("Get account by ID - account not found - unit test")
    void getAccountById_AccountNotFound_UnitTest() {
        // Given
        when(accountService.getAccountByIdAndUserId(accountId, userId))
                .thenThrow(new ResourceNotFoundException("Account not found with id: " + accountId));

        // When & Then
        assertThrows(ResourceNotFoundException.class, () ->
                accountController.getAccountById(userId, accountId));
        verify(accountService).getAccountByIdAndUserId(accountId, userId);
    }

    @Test
    @DisplayName("Get account by ID - wrong user - unit test")
    void getAccountById_WrongUser_UnitTest() {
        // Given
        when(accountService.getAccountByIdAndUserId(accountId, userId))
                .thenThrow(new ResourceNotFoundException("Account not found for this user"));

        // When & Then
        assertThrows(ResourceNotFoundException.class, () ->
                accountController.getAccountById(userId, accountId));
        verify(accountService).getAccountByIdAndUserId(accountId, userId);
    }

    @Test
    @DisplayName("Get accounts by type - success - unit test")
    void getAccountsByType_Success_UnitTest() {
        // Given
        List<Account> accounts = Arrays.asList(testAccount);
        when(accountService.getAccountsByUserAndType(userId, AccountType.CHECKING))
                .thenReturn(accounts);

        // When
        ResponseEntity<List<Account>> response = accountController.getAccountsByType(userId, AccountType.CHECKING);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(accounts, response.getBody());
        verify(accountService).getAccountsByUserAndType(userId, AccountType.CHECKING);
    }

    @Test
    @DisplayName("Update account - success - unit test")
    void updateAccount_Success_UnitTest() {
        // Given - Use UpdateAccountRequest instead of Account
        UpdateAccountRequest request = new UpdateAccountRequest();
        request.setName("Updated Account");
        request.setType(AccountType.CHECKING);
        request.setBalance(new BigDecimal("1500.00"));
        request.setCurrency("USD");
        request.setDescription("Updated description");

        when(accountService.getAccountByIdAndUserId(accountId, userId)).thenReturn(testAccount);
        when(accountService.updateAccount(eq(accountId), any(Account.class))).thenReturn(testAccount);

        // When
        ResponseEntity<Account> response = accountController.updateAccount(userId, accountId, request);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testAccount, response.getBody());
        verify(accountService).getAccountByIdAndUserId(accountId, userId);
        verify(accountService).updateAccount(eq(accountId), any(Account.class));
    }

    @Test
    @DisplayName("Update account - account not found - unit test")
    void updateAccount_AccountNotFound_UnitTest() {
        // Given
        UpdateAccountRequest request = new UpdateAccountRequest();
        request.setName("Updated Account");
        request.setType(AccountType.CHECKING);
        request.setBalance(new BigDecimal("1500.00"));
        request.setCurrency("USD");
        request.setDescription("Updated description");

        when(accountService.getAccountByIdAndUserId(accountId, userId))
                .thenThrow(new ResourceNotFoundException("Account not found with id: " + accountId));

        // When & Then
        assertThrows(ResourceNotFoundException.class, () ->
                accountController.updateAccount(userId, accountId, request));
        verify(accountService).getAccountByIdAndUserId(accountId, userId);
        verify(accountService, never()).updateAccount(anyLong(), any(Account.class));
    }

    @Test
    @DisplayName("Delete account - success - unit test")
    void deleteAccount_Success_UnitTest() {
        // Given
        when(accountService.getAccountByIdAndUserId(accountId, userId)).thenReturn(testAccount);
        doNothing().when(accountService).deleteAccount(accountId);

        // When
        ResponseEntity<String> response = accountController.deleteAccount(userId, accountId);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Account deleted successfully", response.getBody());
        verify(accountService).getAccountByIdAndUserId(accountId, userId);
        verify(accountService).deleteAccount(accountId);
    }

    @Test
    @DisplayName("Deposit - success - unit test")
    void deposit_Success_UnitTest() {
        // Given
        AccountController.AmountRequest request = new AccountController.AmountRequest();
        request.setAmount(new BigDecimal("500.00"));

        when(accountService.getAccountByIdAndUserId(accountId, userId)).thenReturn(testAccount);
        when(accountService.deposit(accountId, request.getAmount())).thenReturn(testAccount);

        // When
        ResponseEntity<Account> response = accountController.deposit(userId, accountId, request);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testAccount, response.getBody());
        verify(accountService).getAccountByIdAndUserId(accountId, userId);
        verify(accountService).deposit(accountId, request.getAmount());
    }

    @Test
    @DisplayName("Withdraw - success - unit test")
    void withdraw_Success_UnitTest() {
        // Given
        AccountController.AmountRequest request = new AccountController.AmountRequest();
        request.setAmount(new BigDecimal("200.00"));

        when(accountService.getAccountByIdAndUserId(accountId, userId)).thenReturn(testAccount);
        when(accountService.withdraw(accountId, request.getAmount())).thenReturn(testAccount);

        // When
        ResponseEntity<Account> response = accountController.withdraw(userId, accountId, request);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testAccount, response.getBody());
        verify(accountService).getAccountByIdAndUserId(accountId, userId);
        verify(accountService).withdraw(accountId, request.getAmount());
    }

    @Test
    @DisplayName("Transfer - success - unit test")
    void transfer_Success_UnitTest() {
        // Given
        AccountController.TransferRequest request = new AccountController.TransferRequest();
        request.setFromAccountId(accountId);
        request.setToAccountId(savingsAccountId);
        request.setAmount(new BigDecimal("300.00"));

        when(accountService.getAccountByIdAndUserId(accountId, userId)).thenReturn(testAccount);
        when(accountService.getAccountByIdAndUserId(savingsAccountId, userId)).thenReturn(savingsAccount);
        doNothing().when(accountService).transfer(accountId, savingsAccountId, request.getAmount());

        // When
        ResponseEntity<String> response = accountController.transfer(userId, request);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Transfer completed successfully", response.getBody());
        verify(accountService).getAccountByIdAndUserId(accountId, userId);
        verify(accountService).getAccountByIdAndUserId(savingsAccountId, userId);
        verify(accountService).transfer(accountId, savingsAccountId, request.getAmount());
    }

    @Test
    @DisplayName("Get total balance - success - unit test")
    void getTotalBalance_Success_UnitTest() {
        // Given
        BigDecimal totalBalance = new BigDecimal("5500.00");
        when(accountService.getTotalBalanceByUser(userId)).thenReturn(totalBalance);

        // When
        ResponseEntity<BigDecimal> response = accountController.getTotalBalance(userId);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(totalBalance, response.getBody());
        verify(accountService).getTotalBalanceByUser(userId);
    }

    // ========== INTEGRATION TESTS (MockMvc HTTP Calls) ==========

    @Test
    @DisplayName("Create account - success - integration test")
    void createAccount_Success_IntegrationTest() throws Exception {
        // Given - Create valid account data that matches all validation constraints
        String accountJson = """
    {
        "name": "New Savings Account",
        "type": "SAVINGS",
        "balance": 500.00,
        "currency": "USD",
        "description": "Test savings account"
    }
    """;

        // Create the account that will be returned by the service (with ID and user set)
        Account savedAccount = new Account();
        savedAccount.setId(1L);
        savedAccount.setName("New Savings Account");
        savedAccount.setType(AccountType.SAVINGS);
        savedAccount.setBalance(new BigDecimal("500.00"));
        savedAccount.setCurrency("USD");
        savedAccount.setDescription("Test savings account");
        savedAccount.setUser(testUser);

        when(accountService.createAccount(eq(userId), any(Account.class))).thenReturn(savedAccount);

        // When & Then
        mockMvc.perform(post("/api/users/{userId}/accounts", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accountJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("New Savings Account"))
                .andExpect(jsonPath("$.type").value("SAVINGS"))
                .andExpect(jsonPath("$.balance").value(500.00));

        verify(accountService).createAccount(eq(userId), any(Account.class));
    }

    @Test
    @DisplayName("Create account - invalid account data returns bad request - integration test")
    void createAccount_InvalidData_ReturnsBadRequest_IntegrationTest() throws Exception {
        // Given - Invalid data (empty name, negative balance)
        String invalidAccountJson = """
    {
        "name": "",
        "type": "SAVINGS",
        "balance": -100.00,
        "currency": "USD"
    }
    """;

        // When & Then
        mockMvc.perform(post("/api/users/{userId}/accounts", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidAccountJson))
                .andDo(print()) // Debug output
                .andExpect(status().isBadRequest());

        verify(accountService, never()).createAccount(anyLong(), any(Account.class));
    }

    @Test
    @DisplayName("Create account - missing required fields returns bad request - integration test")
    void createAccount_MissingRequiredFields_ReturnsBadRequest_IntegrationTest() throws Exception {
        // Given - Missing required fields (no type, no balance)
        String invalidAccountJson = """
    {
        "name": "Test Account",
        "currency": "USD"
    }
    """;

        // When & Then
        mockMvc.perform(post("/api/users/{userId}/accounts", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidAccountJson))
                .andDo(print()) // Debug output
                .andExpect(status().isBadRequest());

        verify(accountService, never()).createAccount(anyLong(), any(Account.class));
    }

    @Test
    @DisplayName("Get accounts by user - success with accounts - integration test")
    void getAccountsByUser_Success_IntegrationTest() throws Exception {
        // Given
        List<Account> accounts = Arrays.asList(testAccount, savingsAccount);
        when(accountService.getAccountsByUser(userId)).thenReturn(accounts);

        // When & Then
        mockMvc.perform(get("/api/users/{userId}/accounts", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(accountId))
                .andExpect(jsonPath("$[0].name").value("Checking Account"))
                .andExpect(jsonPath("$[1].id").value(savingsAccountId))
                .andExpect(jsonPath("$[1].name").value("Savings Account"));

        verify(accountService).getAccountsByUser(userId);
    }

    @Test
    @DisplayName("Get accounts by user - no accounts returns empty list - integration test")
    void getAccountsByUser_NoAccounts_ReturnsEmptyList_IntegrationTest() throws Exception {
        // Given
        when(accountService.getAccountsByUser(userId)).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/users/{userId}/accounts", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(accountService).getAccountsByUser(userId);
    }

    @Test
    @DisplayName("Get account by ID - success - integration test")
    void getAccountById_Success_IntegrationTest() throws Exception {
        // Given
        when(accountService.getAccountByIdAndUserId(accountId, userId)).thenReturn(testAccount);

        // When & Then
        mockMvc.perform(get("/api/users/{userId}/accounts/{accountId}", userId, accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(accountId))
                .andExpect(jsonPath("$.name").value("Checking Account"))
                .andExpect(jsonPath("$.type").value("CHECKING"))
                .andExpect(jsonPath("$.balance").value(1000.00));

        verify(accountService).getAccountByIdAndUserId(accountId, userId);
    }

    @Test
    @DisplayName("Get account by ID - account not found - integration test")
    void getAccountById_NotFound_IntegrationTest() throws Exception {
        // Given
        when(accountService.getAccountByIdAndUserId(accountId, userId))
                .thenThrow(new ResourceNotFoundException("Account not found with id: " + accountId));

        // When & Then
        mockMvc.perform(get("/api/users/{userId}/accounts/{accountId}", userId, accountId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Account not found with id: " + accountId));

        verify(accountService).getAccountByIdAndUserId(accountId, userId);
    }

    @Test
    @DisplayName("Get account by ID - account belongs to different user - integration test")
    void getAccountById_WrongUser_IntegrationTest() throws Exception {
        // Given
        when(accountService.getAccountByIdAndUserId(accountId, userId))
                .thenThrow(new ResourceNotFoundException("Account not found for this user"));

        // When & Then
        mockMvc.perform(get("/api/users/{userId}/accounts/{accountId}", userId, accountId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Account not found for this user"));

        verify(accountService).getAccountByIdAndUserId(accountId, userId);
    }

    @Test
    @DisplayName("Get accounts by type - success - integration test")
    void getAccountsByType_Success_IntegrationTest() throws Exception {
        // Given
        List<Account> accounts = Arrays.asList(testAccount);
        when(accountService.getAccountsByUserAndType(userId, AccountType.CHECKING)).thenReturn(accounts);

        // When & Then
        mockMvc.perform(get("/api/users/{userId}/accounts/type/{type}", userId, AccountType.CHECKING))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(accountId))
                .andExpect(jsonPath("$[0].type").value("CHECKING"));

        verify(accountService).getAccountsByUserAndType(userId, AccountType.CHECKING);
    }

    @Test
    @DisplayName("Get accounts by type - no accounts of type returns empty list - integration test")
    void getAccountsByType_NoAccounts_ReturnsEmptyList_IntegrationTest() throws Exception {
        // Given
        when(accountService.getAccountsByUserAndType(userId, AccountType.INVESTMENT))
                .thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/users/{userId}/accounts/type/{type}", userId, AccountType.INVESTMENT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(accountService).getAccountsByUserAndType(userId, AccountType.INVESTMENT);
    }

    @Test
    @DisplayName("Update account - success - integration test")
    void updateAccount_Success_IntegrationTest() throws Exception {
        // Given
        Account updatedAccount = new Account();
        updatedAccount.setId(accountId);
        updatedAccount.setName("Updated Checking");
        updatedAccount.setType(AccountType.CHECKING);
        updatedAccount.setBalance(new BigDecimal("1500.00"));
        updatedAccount.setCurrency("USD");
        updatedAccount.setDescription("Updated description");

        when(accountService.getAccountByIdAndUserId(accountId, userId)).thenReturn(testAccount);
        when(accountService.updateAccount(eq(accountId), any(Account.class))).thenReturn(updatedAccount);

        String accountJson = """
        {
            "name": "Updated Checking",
            "type": "CHECKING",
            "balance": 1500.00,
            "currency": "USD",
            "description": "Updated description"
        }
        """;

        // When & Then
        mockMvc.perform(put("/api/users/{userId}/accounts/{accountId}", userId, accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accountJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Checking"))
                .andExpect(jsonPath("$.balance").value(1500.00));

        verify(accountService).getAccountByIdAndUserId(accountId, userId);
        verify(accountService).updateAccount(eq(accountId), any(Account.class));
    }

    @Test
    @DisplayName("Update account - partial update success - integration test")
    void updateAccount_PartialUpdate_Success_IntegrationTest() throws Exception {
        // Given
        when(accountService.getAccountByIdAndUserId(accountId, userId)).thenReturn(testAccount);
        when(accountService.updateAccount(eq(accountId), any(Account.class))).thenReturn(testAccount);

        // For partial updates, still include all required fields but only change some
        String accountJson = """
        {
            "name": "Partially Updated Checking",
            "type": "CHECKING",
            "balance": 1000.00,
            "currency": "USD",
            "description": "Primary checking account"
        }
        """;

        // When & Then
        mockMvc.perform(put("/api/users/{userId}/accounts/{accountId}", userId, accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accountJson))
                .andExpect(status().isOk());

        verify(accountService).getAccountByIdAndUserId(accountId, userId);
        verify(accountService).updateAccount(eq(accountId), any(Account.class));
    }

    @Test
    @DisplayName("Update account - account not found - integration test")
    void updateAccount_NotFound_IntegrationTest() throws Exception {
        // Given
        when(accountService.getAccountByIdAndUserId(accountId, userId))
                .thenThrow(new ResourceNotFoundException("Account not found with id: " + accountId));

        String accountJson = """
        {
            "name": "Updated Checking",
            "type": "CHECKING",
            "balance": 1500.00,
            "currency": "USD",
            "description": "Updated description"
        }
        """;

        // When & Then
        mockMvc.perform(put("/api/users/{userId}/accounts/{accountId}", userId, accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accountJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Account not found with id: " + accountId));

        verify(accountService).getAccountByIdAndUserId(accountId, userId);
        verify(accountService, never()).updateAccount(anyLong(), any(Account.class));
    }

    @Test
    @DisplayName("Update account - invalid data returns bad request - integration test")
    void updateAccount_InvalidData_ReturnsBadRequest_IntegrationTest() throws Exception {
        // Given - Invalid data (empty name, missing required fields)
        String invalidAccountJson = """
        {
            "name": "",
            "type": "CHECKING"
            // Missing balance and currency which are required
        }
        """;

        // When & Then
        mockMvc.perform(put("/api/users/{userId}/accounts/{accountId}", userId, accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidAccountJson))
                .andDo(print()) // Debug output
                .andExpect(status().isBadRequest());

        verify(accountService, never()).getAccountByIdAndUserId(anyLong(), anyLong());
        verify(accountService, never()).updateAccount(anyLong(), any(Account.class));
    }

    @Test
    @DisplayName("Delete account - success - integration test")
    void deleteAccount_Success_IntegrationTest() throws Exception {
        // Given
        when(accountService.getAccountByIdAndUserId(accountId, userId)).thenReturn(testAccount);
        doNothing().when(accountService).deleteAccount(accountId);

        // When & Then
        mockMvc.perform(delete("/api/users/{userId}/accounts/{accountId}", userId, accountId))
                .andExpect(status().isOk())
                .andExpect(content().string("Account deleted successfully"));

        verify(accountService).getAccountByIdAndUserId(accountId, userId);
        verify(accountService).deleteAccount(accountId);
    }

    @Test
    @DisplayName("Delete account - account not found - integration test")
    void deleteAccount_NotFound_IntegrationTest() throws Exception {
        // Given
        when(accountService.getAccountByIdAndUserId(accountId, userId))
                .thenThrow(new ResourceNotFoundException("Account not found with id: " + accountId));

        // When & Then
        mockMvc.perform(delete("/api/users/{userId}/accounts/{accountId}", userId, accountId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Account not found with id: " + accountId));

        verify(accountService).getAccountByIdAndUserId(accountId, userId);
        verify(accountService, never()).deleteAccount(anyLong());
    }

    @Test
    @DisplayName("Deposit - success - integration test")
    void deposit_Success_IntegrationTest() throws Exception {
        // Given
        Account updatedAccount = new Account();
        updatedAccount.setId(accountId);
        updatedAccount.setName("Checking Account");
        updatedAccount.setBalance(new BigDecimal("1500.00")); // 1000 + 500

        when(accountService.getAccountByIdAndUserId(accountId, userId)).thenReturn(testAccount);
        when(accountService.deposit(eq(accountId), eq(new BigDecimal("500.00")))).thenReturn(updatedAccount);

        String amountJson = """
        {
            "amount": 500.00
        }
        """;

        // When & Then
        mockMvc.perform(post("/api/users/{userId}/accounts/{accountId}/deposit", userId, accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(amountJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(1500.00));

        verify(accountService).getAccountByIdAndUserId(accountId, userId);
        verify(accountService).deposit(eq(accountId), eq(new BigDecimal("500.00")));
    }

    @Test
    @DisplayName("Deposit - invalid amount returns bad request - integration test")
    void deposit_InvalidAmount_ReturnsBadRequest_IntegrationTest() throws Exception {
        // Given - Invalid amount (zero)
        String invalidAmountJson = """
        {
            "amount": 0.00
        }
        """;

        // When & Then
        mockMvc.perform(post("/api/users/{userId}/accounts/{accountId}/deposit", userId, accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidAmountJson))
                .andDo(print()) // Debug output
                .andExpect(status().isBadRequest());

        verify(accountService, never()).getAccountByIdAndUserId(anyLong(), anyLong());
        verify(accountService, never()).deposit(anyLong(), any(BigDecimal.class));
    }

    @Test
    @DisplayName("Withdraw - success - integration test")
    void withdraw_Success_IntegrationTest() throws Exception {
        // Given
        Account updatedAccount = new Account();
        updatedAccount.setId(accountId);
        updatedAccount.setName("Checking Account");
        updatedAccount.setBalance(new BigDecimal("500.00")); // 1000 - 500

        when(accountService.getAccountByIdAndUserId(accountId, userId)).thenReturn(testAccount);
        when(accountService.withdraw(eq(accountId), eq(new BigDecimal("500.00")))).thenReturn(updatedAccount);

        String amountJson = """
        {
            "amount": 500.00
        }
        """;

        // When & Then
        mockMvc.perform(post("/api/users/{userId}/accounts/{accountId}/withdraw", userId, accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(amountJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(500.00));

        verify(accountService).getAccountByIdAndUserId(accountId, userId);
        verify(accountService).withdraw(eq(accountId), eq(new BigDecimal("500.00")));
    }

    @Test
    @DisplayName("Withdraw - invalid amount returns bad request - integration test")
    void withdraw_InvalidAmount_ReturnsBadRequest_IntegrationTest() throws Exception {
        // Given - Invalid amount (zero)
        String invalidAmountJson = """
        {
            "amount": 0.00
        }
        """;

        // When & Then
        mockMvc.perform(post("/api/users/{userId}/accounts/{accountId}/withdraw", userId, accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidAmountJson))
                .andDo(print()) // Debug output
                .andExpect(status().isBadRequest());

        verify(accountService, never()).getAccountByIdAndUserId(anyLong(), anyLong());
        verify(accountService, never()).withdraw(anyLong(), any(BigDecimal.class));
    }

    @Test
    @DisplayName("Transfer - success - integration test")
    void transfer_Success_IntegrationTest() throws Exception {
        // Given
        when(accountService.getAccountByIdAndUserId(accountId, userId)).thenReturn(testAccount);
        when(accountService.getAccountByIdAndUserId(savingsAccountId, userId)).thenReturn(savingsAccount);
        doNothing().when(accountService).transfer(eq(accountId), eq(savingsAccountId), eq(new BigDecimal("200.00")));

        String transferJson = """
        {
            "fromAccountId": %d,
            "toAccountId": %d,
            "amount": 200.00
        }
        """.formatted(accountId, savingsAccountId);

        // When & Then
        mockMvc.perform(post("/api/users/{userId}/accounts/transfer", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferJson))
                .andExpect(status().isOk())
                .andExpect(content().string("Transfer completed successfully"));

        verify(accountService).getAccountByIdAndUserId(accountId, userId);
        verify(accountService).getAccountByIdAndUserId(savingsAccountId, userId);
        verify(accountService).transfer(eq(accountId), eq(savingsAccountId), eq(new BigDecimal("200.00")));
    }

    @Test
    @DisplayName("Transfer - invalid data returns bad request - integration test")
    void transfer_InvalidData_ReturnsBadRequest_IntegrationTest() throws Exception {
        // Given - Invalid data (negative amount)
        String invalidTransferJson = """
        {
            "fromAccountId": %d,
            "toAccountId": %d,
            "amount": -100.00
        }
        """.formatted(accountId, savingsAccountId);

        // When & Then
        mockMvc.perform(post("/api/users/{userId}/accounts/transfer", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidTransferJson))
                .andDo(print()) // Debug output
                .andExpect(status().isBadRequest());

        verify(accountService, never()).getAccountByIdAndUserId(anyLong(), anyLong());
        verify(accountService, never()).transfer(anyLong(), anyLong(), any(BigDecimal.class));
    }

    @Test
    @DisplayName("Get total balance - success - integration test")
    void getTotalBalance_Success_IntegrationTest() throws Exception {
        // Given
        BigDecimal totalBalance = new BigDecimal("5500.00"); // 1000 + 5000 - 500
        when(accountService.getTotalBalanceByUser(userId)).thenReturn(totalBalance);

        // When & Then
        mockMvc.perform(get("/api/users/{userId}/accounts/total-balance", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(5500.00));

        verify(accountService).getTotalBalanceByUser(userId);
    }

    // ========== SECURITY AND EDGE CASE TESTS ==========

    @Test
    @DisplayName("Security - GET account by ID verifies ownership")
    void security_GetAccount_VerifiesOwnership() throws Exception {
        when(accountService.getAccountByIdAndUserId(accountId, userId))
                .thenThrow(new ResourceNotFoundException("Account not found for this user"));

        mockMvc.perform(get("/api/users/{userId}/accounts/{accountId}", userId, accountId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Account not found for this user"));

        verify(accountService).getAccountByIdAndUserId(accountId, userId);
    }

    @Test
    @DisplayName("Security - UPDATE account verifies ownership")
    void security_UpdateAccount_VerifiesOwnership() throws Exception {
        when(accountService.getAccountByIdAndUserId(accountId, userId))
                .thenThrow(new ResourceNotFoundException("Account not found for this user"));

        // Include all required fields for UpdateAccountRequest validation
        String accountJson = """
    {
        "name": "Updated Account",
        "type": "CHECKING",
        "balance": 1500.00,
        "currency": "USD",
        "description": "Updated account"
    }
    """;

        mockMvc.perform(put("/api/users/{userId}/accounts/{accountId}", userId, accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accountJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Account not found for this user"));

        verify(accountService).getAccountByIdAndUserId(accountId, userId);
    }

    @Test
    @DisplayName("Security - DELETE account verifies ownership")
    void security_DeleteAccount_VerifiesOwnership() throws Exception {
        when(accountService.getAccountByIdAndUserId(accountId, userId))
                .thenThrow(new ResourceNotFoundException("Account not found for this user"));

        mockMvc.perform(delete("/api/users/{userId}/accounts/{accountId}", userId, accountId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Account not found for this user"));

        verify(accountService).getAccountByIdAndUserId(accountId, userId);
    }

    @Test
    @DisplayName("Security - DEPOSIT verifies ownership")
    void security_Deposit_VerifiesOwnership() throws Exception {
        when(accountService.getAccountByIdAndUserId(accountId, userId))
                .thenThrow(new ResourceNotFoundException("Account not found for this user"));

        String depositJson = """
    {
        "amount": 500.00
    }
    """;
        mockMvc.perform(post("/api/users/{userId}/accounts/{accountId}/deposit", userId, accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(depositJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Account not found for this user"));

        verify(accountService).getAccountByIdAndUserId(accountId, userId);
    }

    @Test
    @DisplayName("Security - WITHDRAW verifies ownership")
    void security_Withdraw_VerifiesOwnership() throws Exception {
        when(accountService.getAccountByIdAndUserId(accountId, userId))
                .thenThrow(new ResourceNotFoundException("Account not found for this user"));

        String withdrawJson = """
    {
        "amount": 500.00
    }
    """;
        mockMvc.perform(post("/api/users/{userId}/accounts/{accountId}/withdraw", userId, accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(withdrawJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Account not found for this user"));

        verify(accountService).getAccountByIdAndUserId(accountId, userId);
    }

    @Test
    @DisplayName("Edge case - very small amount validation")
    void edgeCase_VerySmallAmount() throws Exception {
        // Given
        when(accountService.getAccountByIdAndUserId(accountId, userId)).thenReturn(testAccount);
        when(accountService.deposit(eq(accountId), eq(new BigDecimal("0.01")))).thenReturn(testAccount);

        String amountJson = """
        {
            "amount": 0.01
        }
        """;

        // When & Then - Should accept the minimum valid amount
        mockMvc.perform(post("/api/users/{userId}/accounts/{accountId}/deposit", userId, accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(amountJson))
                .andExpect(status().isOk());

        verify(accountService).getAccountByIdAndUserId(accountId, userId);
        verify(accountService).deposit(eq(accountId), eq(new BigDecimal("0.01")));
    }

    // ========== COMPREHENSIVE INTEGRATION TEST ==========

    @Test
    @DisplayName("Integration - complete account lifecycle")
    void integration_CompleteAccountLifecycle() throws Exception {
        // 1. Create account
        String createAccountJson = """
    {
        "name": "Test Account",
        "type": "SAVINGS",
        "balance": 1000.00,
        "currency": "USD",
        "description": "Test account"
    }
    """;

        Account newAccount = new Account();
        newAccount.setId(1L);
        newAccount.setName("Test Account");
        newAccount.setType(AccountType.SAVINGS);
        newAccount.setBalance(new BigDecimal("1000.00"));
        newAccount.setCurrency("USD");
        newAccount.setDescription("Test account");
        newAccount.setUser(testUser);

        when(accountService.createAccount(eq(userId), any(Account.class))).thenReturn(newAccount);

        mockMvc.perform(post("/api/users/{userId}/accounts", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createAccountJson))
                .andExpect(status().isCreated());

        // 2. Get accounts
        when(accountService.getAccountsByUser(userId)).thenReturn(Collections.singletonList(newAccount));

        mockMvc.perform(get("/api/users/{userId}/accounts", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        // 3. Update account
        when(accountService.getAccountByIdAndUserId(1L, userId)).thenReturn(newAccount);
        when(accountService.updateAccount(eq(1L), any(Account.class))).thenReturn(newAccount);

        String updateJson = """
        {
            "name": "Updated Test Account",
            "type": "SAVINGS",
            "balance": 1000.00,
            "currency": "USD",
            "description": "Updated description"
        }
        """;

        mockMvc.perform(put("/api/users/{userId}/accounts/{accountId}", userId, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk());

        // 4. Delete account
        when(accountService.getAccountByIdAndUserId(1L, userId)).thenReturn(newAccount);
        doNothing().when(accountService).deleteAccount(1L);

        mockMvc.perform(delete("/api/users/{userId}/accounts/{accountId}", userId, 1L))
                .andExpect(status().isOk())
                .andExpect(content().string("Account deleted successfully"));

        verify(accountService).createAccount(eq(userId), any(Account.class));
        verify(accountService).getAccountsByUser(userId);
        verify(accountService, times(2)).getAccountByIdAndUserId(1L, userId); // For update and delete
        verify(accountService).updateAccount(eq(1L), any(Account.class));
        verify(accountService).deleteAccount(1L);
    }
}