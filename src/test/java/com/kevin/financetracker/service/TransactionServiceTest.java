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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BudgetService budgetService;

    @InjectMocks
    private TransactionService transactionService;

    private User testUser;
    private Account testAccount;
    private Category testCategory;
    private Transaction testTransaction;
    private TransactionRequest testTransactionRequest;
    private UpdateTransactionRequest testUpdateTransactionRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@gmail.com");

        testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setName("Test Account");
        testAccount.setBalance(BigDecimal.valueOf(1000.00));
        testAccount.setUser(testUser);

        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Test Category");
        testCategory.setUser(testUser);

        testTransaction = new Transaction();
        testTransaction.setId(1L);
        testTransaction.setAmount(BigDecimal.valueOf(100.00));
        testTransaction.setDescription("Test Transaction");
        testTransaction.setType(TransactionType.EXPENSE);
        testTransaction.setTransactionDate(LocalDateTime.now());
        testTransaction.setUser(testUser);
        testTransaction.setAccount(testAccount);
        testTransaction.setCategory(testCategory);

        testTransactionRequest = new TransactionRequest(
                BigDecimal.valueOf(100.00),
                "Test Transaction",
                TransactionType.EXPENSE,
                1L,
                1L,
                LocalDateTime.now(),
                "Test notes"
        );

        testUpdateTransactionRequest = new UpdateTransactionRequest(
                BigDecimal.valueOf(150.00),
                "Updated Transaction",
                TransactionType.EXPENSE,
                1L,
                LocalDateTime.now(),
                "Updated notes"
        );
    }

    @Test
    void createTransaction_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

        // Act
        TransactionResponse response = transactionService.createTransaction(testTransactionRequest, 1L);

        // Assert
        assertNotNull(response);
        assertEquals(testTransaction.getId(), response.id());
        assertEquals(testTransaction.getAmount(), response.amount());
        assertEquals(testTransaction.getDescription(), response.description());

        verify(accountRepository, times(1)).save(testAccount);
        verify(budgetService, times(1)).updateBudgetSpentAmount(1L, 1L, testTransaction.getAmount());
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void createTransaction_UserNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                transactionService.createTransaction(testTransactionRequest, 1L));
    }

    @Test
    void createTransaction_AccountNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(accountRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                transactionService.createTransaction(testTransactionRequest, 1L));
    }

    @Test
    void createTransaction_AccountDoesNotBelongToUser_ThrowsException() {
        // Arrange
        User differentUser = new User();
        differentUser.setId(2L);
        testAccount.setUser(differentUser);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        // Act & Assert
        assertThrows(ValidationException.class, () ->
                transactionService.createTransaction(testTransactionRequest, 1L));
    }

    @Test
    void createTransaction_CategoryDoesNotBelongToUser_ThrowsException() {
        // Arrange
        User differentUser = new User();
        differentUser.setId(2L);
        testCategory.setUser(differentUser);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));

        // Act & Assert
        assertThrows(ValidationException.class, () ->
                transactionService.createTransaction(testTransactionRequest, 1L));
    }

    @Test
    void createTransaction_IncomeTransaction_UpdatesAccountBalanceCorrectly() {
        // Arrange
        TransactionRequest incomeRequest = new TransactionRequest(
                BigDecimal.valueOf(200.00),
                "Income Transaction",
                TransactionType.INCOME,
                1L,
                1L,
                LocalDateTime.now(),
                "Income notes"
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

        // Act
        transactionService.createTransaction(incomeRequest, 1L);

        // Assert - Verify deposit was called
        verify(accountRepository, times(1)).save(testAccount);
    }

    @Test
    void createTransaction_WithoutCategory_Success() {
        // Arrange
        TransactionRequest requestWithoutCategory = new TransactionRequest(
                BigDecimal.valueOf(100.00),
                "Test Transaction",
                TransactionType.EXPENSE,
                1L,
                null,
                LocalDateTime.now(),
                "Test notes"
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

        // Act
        TransactionResponse response = transactionService.createTransaction(requestWithoutCategory, 1L);

        // Assert
        assertNotNull(response);
        verify(budgetService, never()).updateBudgetSpentAmount(anyLong(), anyLong(), any());
    }

    @Test
    void getTransactionById_Found() {
        // Arrange
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(testTransaction));

        // Act
        Optional<Transaction> result = transactionService.getTransactionById(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testTransaction, result.get());
    }

    @Test
    void getTransactionById_NotFound() {
        // Arrange
        when(transactionRepository.findById(1L)).thenReturn(Optional.empty());

        // Act
        Optional<Transaction> result = transactionService.getTransactionById(1L);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void getTransactionsByUser_Success() {
        // Arrange
        List<Transaction> transactions = List.of(testTransaction);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(transactionRepository.findByUser(testUser)).thenReturn(transactions);

        // Act
        List<Transaction> result = transactionService.getTransactionsByUser(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testTransaction, result.get(0));
    }

    @Test
    void getTransactionsByUser_UserNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                transactionService.getTransactionsByUser(1L));
    }

    @Test
    void getTransactionResponsesByUser_Success() {
        // Arrange
        List<Transaction> transactions = List.of(testTransaction);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(transactionRepository.findByUser(testUser)).thenReturn(transactions);

        // Act
        List<TransactionResponse> result = transactionService.getTransactionResponsesByUser(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testTransaction.getId(), result.get(0).id());
    }

    @Test
    void updateTransaction_Success() {
        // Arrange
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(testTransaction));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

        // Act
        TransactionResponse response = transactionService.updateTransaction(1L, testUpdateTransactionRequest, 1L);

        // Assert
        assertNotNull(response);
        verify(transactionRepository, times(1)).save(testTransaction);
    }

    @Test
    void updateTransaction_TransactionNotFound_ThrowsException() {
        // Arrange
        when(transactionRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                transactionService.updateTransaction(1L, testUpdateTransactionRequest, 1L));
    }

    @Test
    void updateTransaction_TransactionDoesNotBelongToUser_ThrowsException() {
        // Arrange
        User differentUser = new User();
        differentUser.setId(2L);
        testTransaction.setUser(differentUser);

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(testTransaction));

        // Act & Assert
        assertThrows(ValidationException.class, () ->
                transactionService.updateTransaction(1L, testUpdateTransactionRequest, 1L));
    }

    @Test
    void updateTransaction_PartialUpdate_Success() {
        // Arrange
        UpdateTransactionRequest partialUpdate = new UpdateTransactionRequest(
                null, // amount not updated
                "Updated Description",
                null, // type not updated
                null, // category not updated
                null, // date not updated
                "Updated notes"
        );

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(testTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

        // Act
        TransactionResponse response = transactionService.updateTransaction(1L, partialUpdate, 1L);

        // Assert
        assertNotNull(response);
        verify(transactionRepository, times(1)).save(testTransaction);
    }

    @Test
    void deleteTransaction_Success() {
        // Arrange
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(testTransaction));

        // Act
        transactionService.deleteTransaction(1L);

        // Assert
        verify(transactionRepository, times(1)).delete(testTransaction);
        verify(accountRepository, times(1)).save(testAccount); // For balance revert
    }

    @Test
    void deleteTransaction_TransactionNotFound_ThrowsException() {
        // Arrange
        when(transactionRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                transactionService.deleteTransaction(1L));
    }

    @Test
    void getFinancialSummary_Success() {
        // Arrange
        LocalDateTime start = LocalDateTime.now().minusDays(30);
        LocalDateTime end = LocalDateTime.now();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(transactionRepository.getTotalIncomeByUserAndDateRange(testUser, start, end))
                .thenReturn(Optional.of(BigDecimal.valueOf(1000.00)));
        when(transactionRepository.getTotalExpensesByUserAndDateRange(testUser, start, end))
                .thenReturn(Optional.of(BigDecimal.valueOf(300.00)));

        // Act
        TransactionService.FinancialSummary summary = transactionService.getFinancialSummary(1L, start, end);

        // Assert
        assertNotNull(summary);
        assertEquals(BigDecimal.valueOf(1000.00), summary.getTotalIncome());
        assertEquals(BigDecimal.valueOf(300.00), summary.getTotalExpenses());
        assertEquals(BigDecimal.valueOf(700.00), summary.getNetSavings());
    }

    @Test
    void getFinancialSummary_UserNotFound_ThrowsException() {
        // Arrange
        LocalDateTime start = LocalDateTime.now().minusDays(30);
        LocalDateTime end = LocalDateTime.now();

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                transactionService.getFinancialSummary(1L, start, end));
    }

    @Test
    void convertToResponse_Success() {
        // Act
        TransactionResponse response = transactionService.convertToResponse(testTransaction);

        // Assert
        assertNotNull(response);
        assertEquals(testTransaction.getId(), response.id());
        assertEquals(testTransaction.getAmount(), response.amount());
        assertEquals(testTransaction.getDescription(), response.description());
        assertEquals(testTransaction.getType(), response.type());
        assertEquals(testAccount.getName(), response.accountName());
        assertEquals(testCategory.getName(), response.categoryName());
        assertEquals(testTransaction.getTransactionDate(), response.transactionDate());
        assertEquals(testTransaction.getCreatedAt(), response.createdAt());
        assertEquals(testTransaction.getNotes(), response.notes());
    }

    @Test
    void convertToResponse_WithNullCategoryAndAccount() {
        // Arrange
        testTransaction.setCategory(null);
        testTransaction.setAccount(null);

        // Act
        TransactionResponse response = transactionService.convertToResponse(testTransaction);

        // Assert
        assertNotNull(response);
        assertNull(response.accountName());
        assertNull(response.categoryName());
    }
}