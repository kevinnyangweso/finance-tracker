package com.kevin.financetracker.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kevin.financetracker.exception.DuplicateResourceException;
import com.kevin.financetracker.exception.GlobalExceptionHandler;
import com.kevin.financetracker.exception.ResourceNotFoundException;
import com.kevin.financetracker.exception.ValidationException;
import com.kevin.financetracker.model.*;
import com.kevin.financetracker.repository.UserRepository;
import com.kevin.financetracker.repository.CategoryRepository;
import com.kevin.financetracker.repository.BudgetRepository;
import com.kevin.financetracker.service.BudgetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class BudgetControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BudgetService budgetService;

    @InjectMocks
    private BudgetController budgetController;

    private ObjectMapper objectMapper;

    private User testUser;
    private User differentUser;
    private Category testCategory;
    private Budget testBudget;
    private Budget exceededBudget;
    private Long userId = 1L;
    private Long differentUserId = 2L;
    private Long budgetId = 1L;
    private Long categoryId = 1L;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        mockMvc = MockMvcBuilders.standaloneSetup(budgetController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        testUser = new User();
        testUser.setId(userId);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        differentUser = new User();
        differentUser.setId(differentUserId);
        differentUser.setUsername("differentuser");
        differentUser.setEmail("different@example.com");

        testCategory = new Category();
        testCategory.setId(categoryId);
        testCategory.setName("Groceries");
        testCategory.setType(CategoryType.EXPENSE);
        testCategory.setUser(testUser);

        testBudget = new Budget();
        testBudget.setId(budgetId);
        testBudget.setName("Monthly Groceries");
        testBudget.setAmount(new BigDecimal("500.00"));
        testBudget.setSpent(new BigDecimal("350.00"));
        testBudget.setPeriod(BudgetPeriod.MONTHLY);
        testBudget.setStartDate(LocalDate.now());
        testBudget.setEndDate(LocalDate.now().plusDays(30));
        testBudget.setUser(testUser);
        testBudget.setCategory(testCategory);

        exceededBudget = new Budget();
        exceededBudget.setId(2L);
        exceededBudget.setName("Exceeded Budget");
        exceededBudget.setAmount(new BigDecimal("300.00"));
        exceededBudget.setSpent(new BigDecimal("450.00"));
        exceededBudget.setPeriod(BudgetPeriod.MONTHLY);
        exceededBudget.setStartDate(LocalDate.now());
        exceededBudget.setEndDate(LocalDate.now().plusDays(30));
        exceededBudget.setUser(testUser);
        exceededBudget.setCategory(testCategory);
    }

    // CREATE BUDGET TESTS
    @Test
    void createBudget_PastStartDate_ReturnsBadRequest() throws Exception {
        Budget pastDateBudget = new Budget();
        pastDateBudget.setName("Monthly Groceries");
        pastDateBudget.setAmount(new BigDecimal("500.00"));
        pastDateBudget.setSpent(new BigDecimal("350.00"));
        pastDateBudget.setPeriod(BudgetPeriod.MONTHLY);
        pastDateBudget.setStartDate(LocalDate.now().minusDays(10));
        pastDateBudget.setEndDate(LocalDate.now().plusDays(20));
        pastDateBudget.setUser(testUser);
        pastDateBudget.setCategory(testCategory);

        mockMvc.perform(post("/api/users/{userId}/budgets", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pastDateBudget)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("One or more fields failed validation"))
                .andExpect(jsonPath("$.details.startDate").value("Start date cannot be in the past"));

        verify(budgetService, never()).createBudget(anyLong(), any(Budget.class));
    }

    @Test
    void createBudget_InvalidBudget_ReturnsBadRequest() throws Exception {
        Budget invalidBudget = new Budget(); // Missing required fields

        mockMvc.perform(post("/api/users/{userId}/budgets", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidBudget)))
                .andExpect(status().isBadRequest());

        verify(budgetService, never()).createBudget(anyLong(), any(Budget.class));
    }

    @Test
    void createBudget_UserNotFound_ReturnsNotFound() throws Exception {
        Budget validBudget = new Budget();
        validBudget.setName("Monthly Groceries");
        validBudget.setAmount(new BigDecimal("500.00"));
        validBudget.setSpent(new BigDecimal("350.00"));
        validBudget.setPeriod(BudgetPeriod.MONTHLY);
        validBudget.setStartDate(LocalDate.now());
        validBudget.setEndDate(LocalDate.now().plusDays(30));
        validBudget.setUser(testUser);
        validBudget.setCategory(testCategory);

        when(budgetService.createBudget(eq(userId), any(Budget.class)))
                .thenThrow(new ResourceNotFoundException("User not found with id: " + userId));

        mockMvc.perform(post("/api/users/{userId}/budgets", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBudget)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found with id: " + userId));

        verify(budgetService).createBudget(eq(userId), any(Budget.class));
    }

    @Test
    void createBudget_CategoryNotFound_ReturnsNotFound() throws Exception {
        when(budgetService.createBudget(eq(userId), any(Budget.class)))
                .thenThrow(new ResourceNotFoundException("Category not found"));

        mockMvc.perform(post("/api/users/{userId}/budgets", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testBudget)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Category not found"));

        verify(budgetService).createBudget(eq(userId), any(Budget.class));
    }

    @Test
    void createBudget_CategoryDoesNotBelongToUser_ReturnsBadRequest() throws Exception {
        when(budgetService.createBudget(eq(userId), any(Budget.class)))
                .thenThrow(new ValidationException("Category does not belong to user"));

        mockMvc.perform(post("/api/users/{userId}/budgets", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testBudget)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Category does not belong to user"));

        verify(budgetService).createBudget(eq(userId), any(Budget.class));
    }

    @Test
    void createBudget_DuplicateBudget_ReturnsConflict() throws Exception {
        when(budgetService.createBudget(eq(userId), any(Budget.class)))
                .thenThrow(new DuplicateResourceException("Budget already exists for this category and date range"));

        mockMvc.perform(post("/api/users/{userId}/budgets", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testBudget)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Budget already exists for this category and date range"));

        verify(budgetService).createBudget(eq(userId), any(Budget.class));
    }

    @Test
    void createBudget_InvalidJson_ReturnsBadRequest() throws Exception {
        // Use valid JSON structure with missing required fields to trigger validation error
        String invalidJson = """
        {
            "name": "",
            "amount": null,
            "period": null,
            "startDate": null,
            "endDate": null,
            "category": null,
            "user": null
        }
        """;

        mockMvc.perform(post("/api/users/{userId}/budgets", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("One or more fields failed validation"));

        verify(budgetService, never()).createBudget(anyLong(), any(Budget.class));
    }

    // GET ALL BUDGETS TESTS
    @Test
    void getBudgetsByUser_Success_ReturnsBudgets() throws Exception {
        List<Budget> budgets = Arrays.asList(testBudget, exceededBudget);
        when(budgetService.getBudgetsByUser(userId)).thenReturn(budgets);

        mockMvc.perform(get("/api/users/{userId}/budgets", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(budgetId))
                .andExpect(jsonPath("$[0].name").value("Monthly Groceries"))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].name").value("Exceeded Budget"));

        verify(budgetService).getBudgetsByUser(userId);
    }

    @Test
    void getBudgetsByUser_NoBudgets_ReturnsEmptyList() throws Exception {
        when(budgetService.getBudgetsByUser(userId)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/users/{userId}/budgets", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(budgetService).getBudgetsByUser(userId);
    }

    @Test
    void getBudgetsByUser_UserNotFound_ReturnsNotFound() throws Exception {
        when(budgetService.getBudgetsByUser(userId))
                .thenThrow(new ResourceNotFoundException("User not found with id: " + userId));

        mockMvc.perform(get("/api/users/{userId}/budgets", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found with id: " + userId));

        verify(budgetService).getBudgetsByUser(userId);
    }

    @Test
    void getBudgetById_NotFound_ReturnsNotFound() throws Exception {
        when(budgetService.getBudgetById(budgetId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/{userId}/budgets/{budgetId}", userId, budgetId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Budget not found with id: " + budgetId));

        verify(budgetService).getBudgetById(budgetId);
    }

    @Test
    void getBudgetById_WrongUser_ReturnsNotFound() throws Exception {
        testBudget.setUser(differentUser);
        when(budgetService.getBudgetById(budgetId)).thenReturn(Optional.of(testBudget));

        mockMvc.perform(get("/api/users/{userId}/budgets/{budgetId}", userId, budgetId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Budget not found for this user"));

        verify(budgetService).getBudgetById(budgetId);
    }

    // GET ACTIVE BUDGETS TESTS
    @Test
    void getActiveBudgets_Success_ReturnsActiveBudgets() throws Exception {
        List<Budget> activeBudgets = Arrays.asList(testBudget);
        when(budgetService.getActiveBudgetsByUser(userId)).thenReturn(activeBudgets);

        mockMvc.perform(get("/api/users/{userId}/budgets/active", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(budgetId))
                .andExpect(jsonPath("$[0].name").value("Monthly Groceries"));

        verify(budgetService).getActiveBudgetsByUser(userId);
    }

    @Test
    void getActiveBudgets_NoActiveBudgets_ReturnsEmptyList() throws Exception {
        when(budgetService.getActiveBudgetsByUser(userId)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/users/{userId}/budgets/active", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(budgetService).getActiveBudgetsByUser(userId);
    }

    // GET EXCEEDED BUDGETS TESTS
    @Test
    void getExceededBudgets_Success_ReturnsExceededBudgets() throws Exception {
        List<Budget> exceededBudgets = Arrays.asList(exceededBudget);
        when(budgetService.getExceededBudgetsByUser(userId)).thenReturn(exceededBudgets);

        mockMvc.perform(get("/api/users/{userId}/budgets/exceeded", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(2L))
                .andExpect(jsonPath("$[0].name").value("Exceeded Budget"))
                .andExpect(jsonPath("$[0].spent").value(450.00))
                .andExpect(jsonPath("$[0].amount").value(300.00));

        verify(budgetService).getExceededBudgetsByUser(userId);
    }

    @Test
    void getExceededBudgets_NoExceededBudgets_ReturnsEmptyList() throws Exception {
        when(budgetService.getExceededBudgetsByUser(userId)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/users/{userId}/budgets/exceeded", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(budgetService).getExceededBudgetsByUser(userId);
    }

    // UPDATE BUDGET TESTS
    @Test
    void updateBudget_Success_ReturnsUpdatedBudget() throws Exception {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusDays(30);

        String budgetJson = String.format("""
        {
            "name": "Updated Monthly Groceries",
            "amount": 600.00,
            "spent": 400.00,
            "period": "MONTHLY",
            "startDate": "%s",
            "endDate": "%s",
            "category": {"id": %d}
        }
        """, startDate, endDate, categoryId);

        Budget updatedBudget = new Budget();
        updatedBudget.setId(budgetId);
        updatedBudget.setName("Updated Monthly Groceries");
        updatedBudget.setAmount(new BigDecimal("600.00"));
        updatedBudget.setSpent(new BigDecimal("400.00"));
        updatedBudget.setPeriod(BudgetPeriod.MONTHLY);
        updatedBudget.setStartDate(startDate);
        updatedBudget.setEndDate(endDate);
        updatedBudget.setUser(testUser);
        updatedBudget.setCategory(testCategory);

        when(budgetService.getBudgetById(budgetId)).thenReturn(Optional.of(testBudget));
        when(budgetService.updateBudget(eq(budgetId), any(BudgetUpdateDTO.class))).thenReturn(updatedBudget);

        MvcResult result = mockMvc.perform(put("/api/users/{userId}/budgets/{budgetId}", userId, budgetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(budgetJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(budgetId))
                .andExpect(jsonPath("$.name").value("Updated Monthly Groceries"))
                .andExpect(jsonPath("$.amount").value(600.00))
                .andReturn();

        System.out.println("Response body: " + result.getResponse().getContentAsString());

        verify(budgetService).getBudgetById(budgetId);
        verify(budgetService).updateBudget(eq(budgetId), any(BudgetUpdateDTO.class));
    }

    @Test
    void updateBudget_PartialUpdate_Success() throws Exception {
        String budgetJson = """
        {
            "name": "Partially Updated Name"
        }
        """;

        Budget updatedBudget = new Budget();
        updatedBudget.setId(budgetId);
        updatedBudget.setName("Partially Updated Name");
        updatedBudget.setAmount(testBudget.getAmount());
        updatedBudget.setSpent(testBudget.getSpent());
        updatedBudget.setPeriod(testBudget.getPeriod());
        updatedBudget.setStartDate(testBudget.getStartDate());
        updatedBudget.setEndDate(testBudget.getEndDate());
        updatedBudget.setUser(testUser);
        updatedBudget.setCategory(testCategory);

        when(budgetService.getBudgetById(budgetId)).thenReturn(Optional.of(testBudget));
        when(budgetService.updateBudget(eq(budgetId), any(BudgetUpdateDTO.class))).thenReturn(updatedBudget);

        MvcResult result = mockMvc.perform(put("/api/users/{userId}/budgets/{budgetId}", userId, budgetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(budgetJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Partially Updated Name"))
                .andExpect(jsonPath("$.amount").value(500.00))
                .andReturn();

        System.out.println("Response body: " + result.getResponse().getContentAsString());

        verify(budgetService).getBudgetById(budgetId);
        verify(budgetService).updateBudget(eq(budgetId), any(BudgetUpdateDTO.class));
    }

    @Test
    void updateBudget_NotFound_ReturnsNotFound() throws Exception {
        when(budgetService.getBudgetById(budgetId)).thenReturn(Optional.empty());

        String budgetJson = """
        {
            "name": "Updated Budget",
            "amount": 600.00,
            "period": "MONTHLY",
            "startDate": "%s",
            "endDate": "%s",
            "category": {"id": %d}
        }
        """.formatted(LocalDate.now(), LocalDate.now().plusDays(30), categoryId);

        mockMvc.perform(put("/api/users/{userId}/budgets/{budgetId}", userId, budgetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(budgetJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Budget not found with id: " + budgetId));

        verify(budgetService).getBudgetById(budgetId);
        verify(budgetService, never()).updateBudget(anyLong(), any(BudgetUpdateDTO.class));
    }

    @Test
    void updateBudget_WrongUser_ReturnsNotFound() throws Exception {
        testBudget.setUser(differentUser);
        when(budgetService.getBudgetById(budgetId)).thenReturn(Optional.of(testBudget));

        String budgetJson = """
        {
            "name": "Updated Budget",
            "amount": 600.00,
            "period": "MONTHLY",
            "startDate": "%s",
            "endDate": "%s",
            "category": {"id": %d}
        }
        """.formatted(LocalDate.now(), LocalDate.now().plusDays(30), categoryId);

        mockMvc.perform(put("/api/users/{userId}/budgets/{budgetId}", userId, budgetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(budgetJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Budget not found for this user"));

        verify(budgetService).getBudgetById(budgetId);
        verify(budgetService, never()).updateBudget(anyLong(), any(BudgetUpdateDTO.class));
    }

    @Test
    void updateBudget_InvalidBudget_ReturnsBadRequest() throws Exception {
        BudgetUpdateDTO invalidBudget = new BudgetUpdateDTO();
        invalidBudget.setName(""); // Violates @NotBlank
        invalidBudget.setAmount(null); // Violates @NotNull
        invalidBudget.setPeriod(null); // Violates @NotNull
        invalidBudget.setStartDate(null); // Violates @NotNull
        invalidBudget.setEndDate(null); // Violates @NotNull
        invalidBudget.setCategory(null); // Violates @NotNull

        mockMvc.perform(put("/api/users/{userId}/budgets/{budgetId}", userId, budgetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidBudget)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("One or more fields failed validation"));

        verify(budgetService, never()).getBudgetById(anyLong());
        verify(budgetService, never()).updateBudget(anyLong(), any(BudgetUpdateDTO.class));
    }

    // DELETE BUDGET TESTS
    @Test
    void deleteBudget_Success_ReturnsSuccessMessage() throws Exception {
        when(budgetService.getBudgetById(budgetId)).thenReturn(Optional.of(testBudget));
        doNothing().when(budgetService).deleteBudget(budgetId);

        mockMvc.perform(delete("/api/users/{userId}/budgets/{budgetId}", userId, budgetId))
                .andExpect(status().isOk())
                .andExpect(content().string("Budget deleted successfully"));

        verify(budgetService).getBudgetById(budgetId);
        verify(budgetService).deleteBudget(budgetId);
    }

    @Test
    void deleteBudget_NotFound_ReturnsNotFound() throws Exception {
        when(budgetService.getBudgetById(budgetId)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/users/{userId}/budgets/{budgetId}", userId, budgetId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Budget not found with id: " + budgetId));

        verify(budgetService).getBudgetById(budgetId);
        verify(budgetService, never()).deleteBudget(anyLong());
    }

    @Test
    void deleteBudget_WrongUser_ReturnsNotFound() throws Exception {
        testBudget.setUser(differentUser);
        when(budgetService.getBudgetById(budgetId)).thenReturn(Optional.of(testBudget));

        mockMvc.perform(delete("/api/users/{userId}/budgets/{budgetId}", userId, budgetId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Budget not found for this user"));

        verify(budgetService).getBudgetById(budgetId);
        verify(budgetService, never()).deleteBudget(anyLong());
    }

    // RESET BUDGET SPENT TESTS
    @Test
    void resetBudgetSpent_Success_ReturnsSuccessMessage() throws Exception {
        when(budgetService.getBudgetById(budgetId)).thenReturn(Optional.of(testBudget));
        doNothing().when(budgetService).resetBudgetSpentAmount(budgetId);

        mockMvc.perform(post("/api/users/{userId}/budgets/{budgetId}/reset", userId, budgetId))
                .andExpect(status().isOk())
                .andExpect(content().string("Budget spent amount reset successfully"));

        verify(budgetService).getBudgetById(budgetId);
        verify(budgetService).resetBudgetSpentAmount(budgetId);
    }

    @Test
    void resetBudgetSpent_NotFound_ReturnsNotFound() throws Exception {
        when(budgetService.getBudgetById(budgetId)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/users/{userId}/budgets/{budgetId}/reset", userId, budgetId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Budget not found with id: " + budgetId));

        verify(budgetService).getBudgetById(budgetId);
        verify(budgetService, never()).resetBudgetSpentAmount(anyLong());
    }

    // GET BUDGETS ENDING SOON TESTS
    @Test
    void getBudgetsEndingSoon_Success_ReturnsBudgets() throws Exception {
        List<Budget> endingBudgets = Arrays.asList(testBudget);
        when(budgetService.getBudgetsEndingSoon(userId, 7)).thenReturn(endingBudgets);

        mockMvc.perform(get("/api/users/{userId}/budgets/ending-soon", userId)
                        .param("days", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(budgetId))
                .andExpect(jsonPath("$[0].name").value("Monthly Groceries"));

        verify(budgetService).getBudgetsEndingSoon(userId, 7);
    }

    @Test
    void getBudgetsEndingSoon_DefaultDays_Success() throws Exception {
        List<Budget> endingBudgets = Arrays.asList(testBudget);
        when(budgetService.getBudgetsEndingSoon(userId, 7)).thenReturn(endingBudgets);

        mockMvc.perform(get("/api/users/{userId}/budgets/ending-soon", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(budgetService).getBudgetsEndingSoon(userId, 7);
    }

    @Test
    void getBudgetsEndingSoon_InvalidDays_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/users/{userId}/budgets/ending-soon", userId)
                        .param("days", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Days parameter must be between 1 and 365"));

        verify(budgetService, never()).getBudgetsEndingSoon(anyLong(), anyInt());
    }

    @Test
    void getBudgetsEndingSoon_DaysTooLarge_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/users/{userId}/budgets/ending-soon", userId)
                        .param("days", "400"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Days parameter must be between 1 and 365"));

        verify(budgetService, never()).getBudgetsEndingSoon(anyLong(), anyInt());
    }

    @Test
    void getBudgetsEndingSoon_NoBudgets_ReturnsEmptyList() throws Exception {
        when(budgetService.getBudgetsEndingSoon(userId, 7)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/users/{userId}/budgets/ending-soon", userId)
                        .param("days", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(budgetService).getBudgetsEndingSoon(userId, 7);
    }

    // GET BUDGET UTILIZATION TESTS
    @Test
    void getBudgetUtilization_Success_ReturnsUtilization() throws Exception {
        double utilization = 70.5;
        when(budgetService.getBudgetById(budgetId)).thenReturn(Optional.of(testBudget));
        when(budgetService.calculateBudgetUtilization(budgetId)).thenReturn(utilization);

        mockMvc.perform(get("/api/users/{userId}/budgets/{budgetId}/utilization", userId, budgetId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(utilization));

        verify(budgetService).getBudgetById(budgetId);
        verify(budgetService).calculateBudgetUtilization(budgetId);
    }

    @Test
    void getBudgetUtilization_NotFound_ReturnsNotFound() throws Exception {
        when(budgetService.getBudgetById(budgetId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/{userId}/budgets/{budgetId}/utilization", userId, budgetId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Budget not found with id: " + budgetId));

        verify(budgetService).getBudgetById(budgetId);
        verify(budgetService, never()).calculateBudgetUtilization(anyLong());
    }

    @Test
    void getBudgetUtilization_WrongUser_ReturnsNotFound() throws Exception {
        testBudget.setUser(differentUser);
        when(budgetService.getBudgetById(budgetId)).thenReturn(Optional.of(testBudget));

        mockMvc.perform(get("/api/users/{userId}/budgets/{budgetId}/utilization", userId, budgetId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Budget not found for this user"));

        verify(budgetService).getBudgetById(budgetId);
        verify(budgetService, never()).calculateBudgetUtilization(anyLong());
    }

    // Example of one more test for brevity
    @Test
    void getBudgetById_Success_ReturnsBudget() throws Exception {
        when(budgetService.getBudgetById(budgetId)).thenReturn(Optional.of(testBudget));

        mockMvc.perform(get("/api/users/{userId}/budgets/{budgetId}", userId, budgetId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(budgetId))
                .andExpect(jsonPath("$.name").value("Monthly Groceries"))
                .andExpect(jsonPath("$.amount").value(500.00))
                .andExpect(jsonPath("$.spent").value(350.00))
                .andExpect(jsonPath("$.user.id").value(userId));

        verify(budgetService).getBudgetById(budgetId);
    }

    // Utility methods for Mockito matchers
    private static <T> T any(Class<T> clazz) {
        return org.mockito.ArgumentMatchers.any(clazz);
    }

    private static Long anyLong() {
        return org.mockito.ArgumentMatchers.anyLong();
    }

    private static Integer anyInt() {
        return org.mockito.ArgumentMatchers.anyInt();
    }
}
