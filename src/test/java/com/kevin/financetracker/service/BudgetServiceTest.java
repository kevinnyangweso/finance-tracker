package com.kevin.financetracker.service;

import com.kevin.financetracker.exception.DuplicateResourceException;
import com.kevin.financetracker.exception.ResourceNotFoundException;
import com.kevin.financetracker.exception.ValidationException;
import com.kevin.financetracker.model.*;
import com.kevin.financetracker.repository.BudgetRepository;
import com.kevin.financetracker.repository.CategoryRepository;
import com.kevin.financetracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BudgetService budgetService;

    private User testUser;
    private Category testCategory;
    private Budget testBudget;
    private LocalDate currentDate;

    @BeforeEach
    void setUp() {
        currentDate = LocalDate.now();

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Groceries");
        testCategory.setType(CategoryType.valueOf("EXPENSE"));
        testCategory.setUser(testUser);

        testBudget = new Budget();
        testBudget.setId(1L);
        testBudget.setName("Monthly Groceries");
        testBudget.setAmount(new BigDecimal("500.00"));
        testBudget.setSpent(new BigDecimal("350.00"));
        testBudget.setPeriod(BudgetPeriod.MONTHLY);
        testBudget.setStartDate(currentDate.minusDays(10));
        testBudget.setEndDate(currentDate.plusDays(20));
        testBudget.setUser(testUser);
        testBudget.setCategory(testCategory);
    }

    // Create Budget Tests
    @Test
    void createBudget_ShouldCreateBudgetSuccessfully() {
        Long userId = 1L;
        Budget newBudget = new Budget();
        newBudget.setName("New Budget");
        newBudget.setAmount(new BigDecimal("1000.00"));
        newBudget.setStartDate(currentDate);
        newBudget.setEndDate(currentDate.plusDays(30));
        newBudget.setPeriod(BudgetPeriod.MONTHLY);
        newBudget.setCategory(testCategory);

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(categoryRepository.findById(testCategory.getId())).thenReturn(Optional.of(testCategory));
        when(budgetRepository.existsByUserAndCategoryAndDateRange(testUser, testCategory,
                newBudget.getStartDate(), newBudget.getEndDate())).thenReturn(false);
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> {
            Budget savedBudget = invocation.getArgument(0);
            savedBudget.setId(2L);
            return savedBudget;
        });

        Budget result = budgetService.createBudget(userId, newBudget);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(2L);
        assertThat(result.getUser()).isEqualTo(testUser);
        assertThat(result.getCategory()).isEqualTo(testCategory);
        assertThat(result.getSpent()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(userRepository, times(1)).findById(userId);
        verify(categoryRepository, times(1)).findById(testCategory.getId());
        verify(budgetRepository, times(1)).existsByUserAndCategoryAndDateRange(
                testUser, testCategory, newBudget.getStartDate(), newBudget.getEndDate());
        verify(budgetRepository, times(1)).save(any(Budget.class));
    }

    @Test
    void createBudget_ShouldThrowExceptionWhenUserNotFound() {
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                budgetService.createBudget(userId, testBudget));
        verify(userRepository, times(1)).findById(userId);
        verifyNoInteractions(categoryRepository, budgetRepository);
    }

    @Test
    void createBudget_ShouldThrowExceptionWhenCategoryNotFound() {
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(categoryRepository.findById(testCategory.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                budgetService.createBudget(userId, testBudget));
        verify(userRepository, times(1)).findById(userId);
        verify(categoryRepository, times(1)).findById(testCategory.getId());
        verifyNoInteractions(budgetRepository);
    }

    @Test
    void createBudget_ShouldThrowExceptionWhenCategoryDoesNotBelongToUser() {
        Long userId = 1L;
        User differentUser = new User();
        differentUser.setId(2L);

        Category categoryWithDifferentUser = new Category();
        categoryWithDifferentUser.setId(1L);
        categoryWithDifferentUser.setUser(differentUser);

        Budget budgetWithWrongCategory = new Budget();
        budgetWithWrongCategory.setCategory(categoryWithDifferentUser);

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(categoryRepository.findById(categoryWithDifferentUser.getId())).thenReturn(Optional.of(categoryWithDifferentUser));

        assertThrows(ValidationException.class, () ->
                budgetService.createBudget(userId, budgetWithWrongCategory));
        verify(userRepository, times(1)).findById(userId);
        verify(categoryRepository, times(1)).findById(categoryWithDifferentUser.getId());
        verifyNoInteractions(budgetRepository);
    }

    @Test
    void createBudget_ShouldThrowExceptionWhenDuplicateBudgetExists() {
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(categoryRepository.findById(testCategory.getId())).thenReturn(Optional.of(testCategory));
        when(budgetRepository.existsByUserAndCategoryAndDateRange(testUser, testCategory,
                testBudget.getStartDate(), testBudget.getEndDate())).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () ->
                budgetService.createBudget(userId, testBudget));
        verify(userRepository, times(1)).findById(userId);
        verify(categoryRepository, times(1)).findById(testCategory.getId());
        verify(budgetRepository, times(1)).existsByUserAndCategoryAndDateRange(
                testUser, testCategory, testBudget.getStartDate(), testBudget.getEndDate());
        verify(budgetRepository, never()).save(any(Budget.class));
    }

    // Get Budget Tests
    @Test
    void getBudgetById_ShouldReturnBudgetWhenExists() {
        Long budgetId = 1L;
        when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(testBudget));

        Optional<Budget> result = budgetService.getBudgetById(budgetId);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testBudget);
        verify(budgetRepository, times(1)).findById(budgetId);
    }

    @Test
    void getBudgetById_ShouldReturnEmptyWhenNotFound() {
        Long budgetId = 999L;
        when(budgetRepository.findById(budgetId)).thenReturn(Optional.empty());

        Optional<Budget> result = budgetService.getBudgetById(budgetId);

        assertThat(result).isEmpty();
        verify(budgetRepository, times(1)).findById(budgetId);
    }

    @Test
    void getBudgetsByUser_ShouldReturnUserBudgets() {
        Long userId = 1L;
        List<Budget> expectedBudgets = Arrays.asList(testBudget);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(budgetRepository.findByUser(testUser)).thenReturn(expectedBudgets);

        List<Budget> result = budgetService.getBudgetsByUser(userId);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testBudget);
        verify(userRepository, times(1)).findById(userId);
        verify(budgetRepository, times(1)).findByUser(testUser);
    }

    @Test
    void getBudgetsByUser_ShouldThrowExceptionWhenUserNotFound() {
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                budgetService.getBudgetsByUser(userId));
        verify(userRepository, times(1)).findById(userId);
        verifyNoInteractions(budgetRepository);
    }

    @Test
    void getActiveBudgetsByUser_ShouldReturnCurrentBudgets() {
        Long userId = 1L;
        List<Budget> expectedBudgets = Arrays.asList(testBudget);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(budgetRepository.findActiveBudgetsByUser(testUser, currentDate)).thenReturn(expectedBudgets);

        List<Budget> result = budgetService.getActiveBudgetsByUser(userId);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testBudget);
        verify(userRepository, times(1)).findById(userId);
        verify(budgetRepository, times(1)).findActiveBudgetsByUser(testUser, currentDate);
    }

    @Test
    void getExceededBudgetsByUser_ShouldReturnOverBudgetItems() {
        Long userId = 1L;
        Budget exceededBudget = new Budget();
        exceededBudget.setId(2L);
        exceededBudget.setAmount(new BigDecimal("300.00"));
        exceededBudget.setSpent(new BigDecimal("450.00"));
        exceededBudget.setUser(testUser);

        List<Budget> expectedBudgets = Arrays.asList(exceededBudget);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(budgetRepository.findExceededBudgetsByUser(testUser)).thenReturn(expectedBudgets);

        List<Budget> result = budgetService.getExceededBudgetsByUser(userId);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSpent()).isGreaterThan(result.get(0).getAmount());
        verify(userRepository, times(1)).findById(userId);
        verify(budgetRepository, times(1)).findExceededBudgetsByUser(testUser);
    }

    // Update Budget Tests
    @Test
    void updateBudget_ShouldUpdateBudgetSuccessfully() {
        Long budgetId = 1L;
        BudgetUpdateDTO budgetDetails = new BudgetUpdateDTO();
        budgetDetails.setName("Updated Budget");
        budgetDetails.setAmount(new BigDecimal("600.00"));
        budgetDetails.setStartDate(currentDate.plusDays(1));
        budgetDetails.setEndDate(currentDate.plusDays(31));
        budgetDetails.setPeriod(BudgetPeriod.MONTHLY);

        when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(testBudget));
        when(budgetRepository.save(testBudget)).thenReturn(testBudget);

        Budget result = budgetService.updateBudget(budgetId, budgetDetails);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Updated Budget");
        assertThat(result.getAmount()).isEqualByComparingTo("600.00");
        assertThat(result.getStartDate()).isEqualTo(currentDate.plusDays(1));
        assertThat(result.getEndDate()).isEqualTo(currentDate.plusDays(31));
        assertThat(result.getPeriod()).isEqualTo(BudgetPeriod.MONTHLY);
        verify(budgetRepository, times(1)).findById(budgetId);
        verify(budgetRepository, times(1)).save(testBudget);
    }

    @Test
    void updateBudget_ShouldUpdatePartialFields() {
        Long budgetId = 1L;
        BudgetUpdateDTO budgetDetails = new BudgetUpdateDTO();
        budgetDetails.setName("Updated Name Only");

        when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(testBudget));
        when(budgetRepository.save(testBudget)).thenReturn(testBudget);

        Budget result = budgetService.updateBudget(budgetId, budgetDetails);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Updated Name Only");
        assertThat(result.getAmount()).isEqualByComparingTo("500.00");
        assertThat(result.getPeriod()).isEqualTo(BudgetPeriod.MONTHLY);
        verify(budgetRepository, times(1)).findById(budgetId);
        verify(budgetRepository, times(1)).save(testBudget);
    }

    @Test
    void updateBudget_ShouldThrowExceptionWhenBudgetNotFound() {
        Long budgetId = 999L;
        BudgetUpdateDTO budgetDetails = new BudgetUpdateDTO();

        when(budgetRepository.findById(budgetId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                budgetService.updateBudget(budgetId, budgetDetails));
        verify(budgetRepository, times(1)).findById(budgetId);
        verify(budgetRepository, never()).save(any(Budget.class));
    }

    // Delete Budget Tests
    @Test
    void deleteBudget_ShouldDeleteBudgetSuccessfully() {
        Long budgetId = 1L;
        when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(testBudget));
        doNothing().when(budgetRepository).delete(testBudget);

        budgetService.deleteBudget(budgetId);

        verify(budgetRepository, times(1)).findById(budgetId);
        verify(budgetRepository, times(1)).delete(testBudget);
    }

    @Test
    void deleteBudget_ShouldThrowExceptionWhenBudgetNotFound() {
        Long budgetId = 999L;
        when(budgetRepository.findById(budgetId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                budgetService.deleteBudget(budgetId));
        verify(budgetRepository, times(1)).findById(budgetId);
        verify(budgetRepository, never()).delete(any(Budget.class));
    }

    // Budget Spent Amount Tests
    @Test
    void updateBudgetSpentAmount_ShouldUpdateSpentAmountWhenActiveBudgetExists() {
        Long userId = 1L;
        Long categoryId = 1L;
        BigDecimal amountToAdd = new BigDecimal("50.00");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(testCategory));
        when(budgetRepository.findActiveBudgetsByUser(testUser, currentDate))
                .thenReturn(Arrays.asList(testBudget));
        when(budgetRepository.save(testBudget)).thenReturn(testBudget);

        budgetService.updateBudgetSpentAmount(userId, categoryId, amountToAdd);

        assertThat(testBudget.getSpent()).isEqualByComparingTo("400.00");
        verify(userRepository, times(1)).findById(userId);
        verify(categoryRepository, times(1)).findById(categoryId);
        verify(budgetRepository, times(1)).findActiveBudgetsByUser(testUser, currentDate);
        verify(budgetRepository, times(1)).save(testBudget);
    }

    @Test
    void updateBudgetSpentAmount_ShouldNotUpdateWhenNoActiveBudget() {
        Long userId = 1L;
        Long categoryId = 1L;
        BigDecimal amountToAdd = new BigDecimal("50.00");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(testCategory));
        when(budgetRepository.findActiveBudgetsByUser(testUser, currentDate))
                .thenReturn(Collections.emptyList());

        budgetService.updateBudgetSpentAmount(userId, categoryId, amountToAdd);

        verify(userRepository, times(1)).findById(userId);
        verify(categoryRepository, times(1)).findById(categoryId);
        verify(budgetRepository, times(1)).findActiveBudgetsByUser(testUser, currentDate);
        verify(budgetRepository, never()).save(any(Budget.class));
    }

    @Test
    void resetBudgetSpentAmount_ShouldResetSpentToZero() {
        Long budgetId = 1L;
        when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(testBudget));
        when(budgetRepository.save(testBudget)).thenReturn(testBudget);

        budgetService.resetBudgetSpentAmount(budgetId);

        assertThat(testBudget.getSpent()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(budgetRepository, times(1)).findById(budgetId);
        verify(budgetRepository, times(1)).save(testBudget);
    }

    // Budget Ending Soon Tests
    @Test
    void getBudgetsEndingSoon_ShouldReturnEndingBudgets() {
        Long userId = 1L;
        int days = 7;
        LocalDate start = currentDate;
        LocalDate end = currentDate.plusDays(days);

        List<Budget> expectedBudgets = Arrays.asList(testBudget);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(budgetRepository.findBudgetsEndingSoon(testUser, start, end))
                .thenReturn(expectedBudgets);

        List<Budget> result = budgetService.getBudgetsEndingSoon(userId, days);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        verify(userRepository, times(1)).findById(userId);
        verify(budgetRepository, times(1)).findBudgetsEndingSoon(testUser, start, end);
    }

    // Budget Utilization Tests
    @Test
    void calculateBudgetUtilization_ShouldReturnCorrectPercentage() {
        Long budgetId = 1L;
        testBudget.setAmount(new BigDecimal("500.00"));
        testBudget.setSpent(new BigDecimal("250.00"));

        when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(testBudget));

        double utilization = budgetService.calculateBudgetUtilization(budgetId);

        assertThat(utilization).isEqualTo(50.0);
        verify(budgetRepository, times(1)).findById(budgetId);
    }

    @Test
    void calculateBudgetUtilization_ShouldReturnZeroWhenBudgetNotFound() {
        Long budgetId = 999L;
        when(budgetRepository.findById(budgetId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                budgetService.calculateBudgetUtilization(budgetId));
        verify(budgetRepository, times(1)).findById(budgetId);
    }

    // Edge Cases and Additional Scenarios
    @Test
    void createBudget_ShouldInitializeSpentToZero() {
        Long userId = 1L;
        Budget newBudget = new Budget();
        newBudget.setName("Test Budget");
        newBudget.setAmount(new BigDecimal("1000.00"));
        newBudget.setStartDate(currentDate);
        newBudget.setEndDate(currentDate.plusDays(30));
        newBudget.setPeriod(BudgetPeriod.MONTHLY);
        newBudget.setCategory(testCategory);
        newBudget.setSpent(new BigDecimal("100.00"));

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(categoryRepository.findById(testCategory.getId())).thenReturn(Optional.of(testCategory));
        when(budgetRepository.existsByUserAndCategoryAndDateRange(testUser, testCategory,
                newBudget.getStartDate(), newBudget.getEndDate())).thenReturn(false);
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Budget result = budgetService.createBudget(userId, newBudget);

        assertThat(result.getSpent()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void updateBudgetSpentAmount_ShouldHandleMultipleActiveBudgets() {
        Long userId = 1L;
        Long categoryId = 1L;
        BigDecimal amountToAdd = new BigDecimal("25.00");

        Category differentCategory = new Category();
        differentCategory.setId(2L);
        differentCategory.setName("Entertainment");
        differentCategory.setUser(testUser);

        Budget otherBudget = new Budget();
        otherBudget.setId(2L);
        otherBudget.setCategory(differentCategory);
        otherBudget.setUser(testUser);

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(testCategory));
        when(budgetRepository.findActiveBudgetsByUser(testUser, currentDate))
                .thenReturn(Arrays.asList(otherBudget, testBudget));
        when(budgetRepository.save(testBudget)).thenReturn(testBudget);

        budgetService.updateBudgetSpentAmount(userId, categoryId, amountToAdd);

        assertThat(testBudget.getSpent()).isEqualByComparingTo("375.00");
        verify(budgetRepository, times(1)).save(testBudget);
    }

    @Test
    void getBudgetsByUser_ShouldReturnEmptyListWhenNoBudgets() {
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(budgetRepository.findByUser(testUser)).thenReturn(Collections.emptyList());

        List<Budget> result = budgetService.getBudgetsByUser(userId);

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(userRepository, times(1)).findById(userId);
        verify(budgetRepository, times(1)).findByUser(testUser);
    }

    @Test
    void updateBudget_ShouldNotUpdateNullFields() {
        Long budgetId = 1L;
        BudgetUpdateDTO budgetDetails = new BudgetUpdateDTO();

        when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(testBudget));
        when(budgetRepository.save(testBudget)).thenReturn(testBudget);

        Budget result = budgetService.updateBudget(budgetId, budgetDetails);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Monthly Groceries");
        assertThat(result.getAmount()).isEqualByComparingTo("500.00");
        assertThat(result.getPeriod()).isEqualTo(BudgetPeriod.MONTHLY);
        verify(budgetRepository, times(1)).findById(budgetId);
        verify(budgetRepository, times(1)).save(testBudget);
    }
}
