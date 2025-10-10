package com.kevin.financetracker.controller;

import com.kevin.financetracker.dto.TransactionRequest;
import com.kevin.financetracker.dto.TransactionResponse;
import com.kevin.financetracker.dto.UpdateTransactionRequest;
import com.kevin.financetracker.exception.ResourceNotFoundException;
import com.kevin.financetracker.exception.ValidationException;
import com.kevin.financetracker.model.Transaction;
import com.kevin.financetracker.model.TransactionType;
import com.kevin.financetracker.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private TransactionController transactionController;

    private Transaction testTransaction;
    private TransactionResponse testTransactionResponse;
    private TransactionRequest testTransactionRequest;
    private UpdateTransactionRequest testUpdateTransactionRequest;
    private TransactionService.FinancialSummary testFinancialSummary;

    @BeforeEach
    void setUp() {
        com.kevin.financetracker.model.User user = new com.kevin.financetracker.model.User();
        user.setId(1L);

        com.kevin.financetracker.model.Account account = new com.kevin.financetracker.model.Account();
        account.setId(1L);

        testTransaction = new Transaction();
        testTransaction.setId(1L);
        testTransaction.setAmount(BigDecimal.valueOf(100.00));
        testTransaction.setDescription("Test Transaction");
        testTransaction.setType(TransactionType.EXPENSE);
        testTransaction.setTransactionDate(LocalDateTime.now());
        testTransaction.setUser(user);
        testTransaction.setAccount(account);

        testTransactionResponse = new TransactionResponse(
                1L,
                BigDecimal.valueOf(100.00),
                "Test Transaction",
                TransactionType.EXPENSE,
                "Test Account",
                "Test Category",
                LocalDateTime.now(),
                LocalDateTime.now().minusHours(1),
                "Test notes"
        );

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

        testFinancialSummary = new TransactionService.FinancialSummary(
                BigDecimal.valueOf(1000.00),
                BigDecimal.valueOf(300.00),
                BigDecimal.valueOf(700.00)
        );
    }

    // Create Transaction Tests
    @Test
    void createTransaction_Success() {
        when(transactionService.createTransaction(any(TransactionRequest.class), anyLong()))
                .thenReturn(testTransactionResponse);

        ResponseEntity<TransactionResponse> response =
                transactionController.createTransaction(1L, testTransactionRequest);

        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(testTransactionResponse, response.getBody());
        verify(transactionService, times(1)).createTransaction(testTransactionRequest, 1L);
    }

    // Get All Transactions Tests
    @Test
    void getTransactionsByUser_Success() {
        List<Transaction> transactions = List.of(testTransaction);
        when(transactionService.getTransactionsByUser(1L)).thenReturn(transactions);
        when(transactionService.convertToResponse(any(Transaction.class)))
                .thenReturn(testTransactionResponse);

        ResponseEntity<List<TransactionResponse>> response =
                transactionController.getTransactionsByUser(1L);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(testTransactionResponse, response.getBody().get(0));
        verify(transactionService, times(1)).getTransactionsByUser(1L);
        verify(transactionService, times(1)).convertToResponse(testTransaction);
    }

    @Test
    void getTransactionsByUser_EmptyList_Success() {
        when(transactionService.getTransactionsByUser(1L)).thenReturn(List.of());

        ResponseEntity<List<TransactionResponse>> response =
                transactionController.getTransactionsByUser(1L);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
        verify(transactionService, times(1)).getTransactionsByUser(1L);
    }

    // Get Transaction By ID Tests
    @Test
    void getTransactionById_Success() {
        when(transactionService.getTransactionById(1L)).thenReturn(Optional.of(testTransaction));
        when(transactionService.convertToResponse(testTransaction)).thenReturn(testTransactionResponse);

        ResponseEntity<TransactionResponse> response =
                transactionController.getTransactionById(1L, 1L);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testTransactionResponse, response.getBody());
        verify(transactionService, times(1)).getTransactionById(1L);
        verify(transactionService, times(1)).convertToResponse(testTransaction);
    }

    @Test
    void getTransactionById_NotFound_ThrowsException() {
        when(transactionService.getTransactionById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                transactionController.getTransactionById(1L, 1L));
        verify(transactionService, times(1)).getTransactionById(1L);
    }

    @Test
    void getTransactionById_TransactionDoesNotBelongToUser_ThrowsException() {
        com.kevin.financetracker.model.User differentUser = new com.kevin.financetracker.model.User();
        differentUser.setId(2L);
        Transaction differentUserTransaction = new Transaction();
        differentUserTransaction.setId(1L);
        differentUserTransaction.setUser(differentUser);
        differentUserTransaction.setAccount(new com.kevin.financetracker.model.Account());

        when(transactionService.getTransactionById(1L)).thenReturn(Optional.of(differentUserTransaction));

        assertThrows(ResourceNotFoundException.class, () ->
                transactionController.getTransactionById(1L, 1L));
        verify(transactionService, times(1)).getTransactionById(1L);
    }

    // Get Transactions By Date Range Tests
    @Test
    void getTransactionsByDateRange_Success() {
        LocalDateTime start = LocalDateTime.now().minusDays(30);
        LocalDateTime end = LocalDateTime.now();
        List<Transaction> transactions = List.of(testTransaction);

        when(transactionService.getTransactionsByUserAndDateRange(1L, start, end))
                .thenReturn(transactions);
        when(transactionService.convertToResponse(any(Transaction.class)))
                .thenReturn(testTransactionResponse);

        ResponseEntity<List<TransactionResponse>> response =
                transactionController.getTransactionsByDateRange(1L, start, end);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(transactionService, times(1)).getTransactionsByUserAndDateRange(1L, start, end);
        verify(transactionService, times(1)).convertToResponse(testTransaction);
    }

    @Test
    void getTransactionsByDateRange_InvalidDateRange_ThrowsException() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = LocalDateTime.now().minusDays(1);

        assertThrows(ValidationException.class, () ->
                transactionController.getTransactionsByDateRange(1L, start, end));
    }

    @Test
    void getTransactionsByDateRange_DateRangeTooLarge_ThrowsException() {
        LocalDateTime start = LocalDateTime.now().minusYears(2);
        LocalDateTime end = LocalDateTime.now();

        assertThrows(ValidationException.class, () ->
                transactionController.getTransactionsByDateRange(1L, start, end));
    }

    @Test
    void getTransactionsByDateRange_NullDates_ThrowsException() {
        assertThrows(ValidationException.class, () ->
                transactionController.getTransactionsByDateRange(1L, null, null));
    }

    // Get Transactions By Type Tests
    @Test
    void getTransactionsByType_Success() {
        List<Transaction> transactions = List.of(testTransaction);
        when(transactionService.getTransactionsByUserAndType(1L, TransactionType.EXPENSE))
                .thenReturn(transactions);
        when(transactionService.convertToResponse(any(Transaction.class)))
                .thenReturn(testTransactionResponse);

        ResponseEntity<List<TransactionResponse>> response =
                transactionController.getTransactionsByType(1L, TransactionType.EXPENSE);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(transactionService, times(1)).getTransactionsByUserAndType(1L, TransactionType.EXPENSE);
        verify(transactionService, times(1)).convertToResponse(testTransaction);
    }

    // Update Transaction Tests
    @Test
    void updateTransaction_Success() {
        when(transactionService.updateTransaction(1L, testUpdateTransactionRequest, 1L))
                .thenReturn(testTransactionResponse);

        ResponseEntity<TransactionResponse> response =
                transactionController.updateTransaction(1L, 1L, testUpdateTransactionRequest);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testTransactionResponse, response.getBody());
        verify(transactionService, times(1)).updateTransaction(1L, testUpdateTransactionRequest, 1L);
    }

    // Delete Transaction Tests
    @Test
    void deleteTransaction_Success() {
        when(transactionService.getTransactionById(1L)).thenReturn(Optional.of(testTransaction));
        doNothing().when(transactionService).deleteTransaction(1L);

        ResponseEntity<String> response = transactionController.deleteTransaction(1L, 1L);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Transaction deleted successfully", response.getBody());
        verify(transactionService, times(1)).getTransactionById(1L);
        verify(transactionService, times(1)).deleteTransaction(1L);
    }

    @Test
    void deleteTransaction_NotFound_ThrowsException() {
        when(transactionService.getTransactionById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                transactionController.deleteTransaction(1L, 1L));
        verify(transactionService, times(1)).getTransactionById(1L);
    }

    // Get Financial Summary Tests
    @Test
    void getFinancialSummary_Success() {
        LocalDateTime start = LocalDateTime.now().minusDays(30);
        LocalDateTime end = LocalDateTime.now();
        when(transactionService.getFinancialSummary(1L, start, end))
                .thenReturn(testFinancialSummary);

        ResponseEntity<TransactionService.FinancialSummary> response =
                transactionController.getFinancialSummary(1L, start, end);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testFinancialSummary, response.getBody());
        verify(transactionService, times(1)).getFinancialSummary(1L, start, end);
    }

    // Get Recent Transactions Tests
    @Test
    void getRecentTransactions_Success() {
        List<Transaction> transactions = List.of(testTransaction, testTransaction, testTransaction);
        when(transactionService.getTransactionsByUser(1L)).thenReturn(transactions);
        when(transactionService.convertToResponse(any(Transaction.class)))
                .thenReturn(testTransactionResponse);

        ResponseEntity<List<TransactionResponse>> response =
                transactionController.getRecentTransactions(1L, 2);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        verify(transactionService, times(1)).getTransactionsByUser(1L);
        verify(transactionService, times(2)).convertToResponse(testTransaction);
    }

    @Test
    void getRecentTransactions_DefaultLimit_Success() {
        List<Transaction> transactions = List.of(testTransaction);
        when(transactionService.getTransactionsByUser(1L)).thenReturn(transactions);
        when(transactionService.convertToResponse(any(Transaction.class)))
                .thenReturn(testTransactionResponse);

        ResponseEntity<List<TransactionResponse>> response =
                transactionController.getRecentTransactions(1L, 10);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(transactionService, times(1)).getTransactionsByUser(1L);
        verify(transactionService, times(1)).convertToResponse(testTransaction);
    }

    @Test
    void getRecentTransactions_InvalidLimit_ThrowsException() {
        assertThrows(ValidationException.class, () ->
                transactionController.getRecentTransactions(1L, 0));

        assertThrows(ValidationException.class, () ->
                transactionController.getRecentTransactions(1L, 101));
    }

    // Helper Method Tests
    @Test
    void verifyTransactionOwnership_Success() {
        when(transactionService.getTransactionById(1L)).thenReturn(Optional.of(testTransaction));
        when(transactionService.convertToResponse(testTransaction)).thenReturn(testTransactionResponse);

        assertDoesNotThrow(() -> transactionController.getTransactionById(1L, 1L));
        verify(transactionService, times(1)).getTransactionById(1L);
        verify(transactionService, times(1)).convertToResponse(testTransaction);
    }

    @Test
    void validateDateRange_ValidRange_DoesNotThrow() {
        LocalDateTime start = LocalDateTime.now().minusDays(10);
        LocalDateTime end = LocalDateTime.now();

        assertDoesNotThrow(() ->
                transactionController.getTransactionsByDateRange(1L, start, end));
    }

    // Edge Case Tests
    @Test
    void updateTransaction_WithNullFields_Success() {
        UpdateTransactionRequest partialUpdate = new UpdateTransactionRequest(
                null, null, null, null, null, null
        );
        when(transactionService.updateTransaction(1L, partialUpdate, 1L))
                .thenReturn(testTransactionResponse);

        ResponseEntity<TransactionResponse> response =
                transactionController.updateTransaction(1L, 1L, partialUpdate);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(transactionService, times(1)).updateTransaction(1L, partialUpdate, 1L);
    }
}