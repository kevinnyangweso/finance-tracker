package com.financetracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financetracker.dto.*;
import com.financetracker.entity.TransactionType;
import com.financetracker.repository.CategoryRepository;
import com.financetracker.repository.TransactionRepository;
import com.financetracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransactionControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private TransactionRepository transactionRepository;

    private String jwt;
    private Long categoryId;

    @BeforeEach
    void setUp() throws Exception {
        transactionRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        // Register a user
        RegisterRequest reg = new RegisterRequest();
        reg.setUsername("kevin");
        reg.setEmail("kevin@example.com");
        reg.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg)));

        // Log in to get JWT
        LoginRequest login = new LoginRequest();
        login.setUsername("kevin");
        login.setPassword("password123");

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andReturn().getResponse().getContentAsString();

        jwt = objectMapper.readTree(response).get("token").asText();

        // Create a category
        CategoryRequest catReq = new CategoryRequest("Food");
        String catResponse = mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(catReq)))
                .andReturn().getResponse().getContentAsString();

        categoryId = objectMapper.readTree(catResponse).get("id").asLong();
    }

    // ============================================
    // CREATE — Happy path
    // ============================================
    @Test
    void createTransaction_shouldReturn201() throws Exception {
        TransactionRequest req = new TransactionRequest();
        req.setAmount(new BigDecimal("45.50"));
        req.setType(TransactionType.EXPENSE);
        req.setCategoryId(categoryId);
        req.setDate(LocalDate.now());
        req.setDescription("Lunch");

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(45.50))
                .andExpect(jsonPath("$.type").value("EXPENSE"))
                .andExpect(jsonPath("$.categoryName").value("Food"));
    }

    // ============================================
    // GET — Without auth → 401
    // ============================================
    @Test
    void getTransactions_shouldReturn401_withoutToken() throws Exception {
        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isUnauthorized());
    }

    // ============================================
    // GET — With auth → returns own transactions only
    // ============================================
    @Test
    void getTransactions_shouldReturnOwnTransactionsOnly() throws Exception {
        // Create a transaction
        TransactionRequest req = new TransactionRequest();
        req.setAmount(new BigDecimal("100.00"));
        req.setType(TransactionType.INCOME);
        req.setCategoryId(categoryId);
        req.setDate(LocalDate.now());

        mockMvc.perform(post("/api/transactions")
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)));

        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].amount").value(100.00));
    }

    // ============================================
    // SECURITY — Cannot access another user's transaction
    // ============================================
    @Test
    void getTransaction_shouldReturn404_forOtherUsersTransaction() throws Exception {
        // Kevin creates a transaction
        TransactionRequest req = new TransactionRequest();
        req.setAmount(new BigDecimal("100.00"));
        req.setType(TransactionType.INCOME);
        req.setCategoryId(categoryId);
        req.setDate(LocalDate.now());

        String response = mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andReturn().getResponse().getContentAsString();

        Long kevinTxId = objectMapper.readTree(response).get("id").asLong();

        // Register Sarah
        RegisterRequest sarahReg = new RegisterRequest();
        sarahReg.setUsername("sarah");
        sarahReg.setEmail("sarah@example.com");
        sarahReg.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sarahReg)));

        LoginRequest sarahLogin = new LoginRequest();
        sarahLogin.setUsername("sarah");
        sarahLogin.setPassword("password123");

        String sarahResp = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sarahLogin)))
                .andReturn().getResponse().getContentAsString();

        String sarahJwt = objectMapper.readTree(sarahResp).get("token").asText();

        // Sarah tries to access Kevin's transaction
        mockMvc.perform(get("/api/transactions/" + kevinTxId)
                        .header("Authorization", "Bearer " + sarahJwt))
                .andExpect(status().isNotFound());
    }

    // ============================================
    // DASHBOARD — Returns correct summary
    // ============================================
    @Test
    void dashboard_shouldReturnCorrectSummary() throws Exception {
        TransactionRequest income = new TransactionRequest();
        income.setAmount(new BigDecimal("5000.00"));
        income.setType(TransactionType.INCOME);
        income.setCategoryId(categoryId);
        income.setDate(LocalDate.now());

        TransactionRequest expense = new TransactionRequest();
        expense.setAmount(new BigDecimal("1200.00"));
        expense.setType(TransactionType.EXPENSE);
        expense.setCategoryId(categoryId);
        expense.setDate(LocalDate.now());

        mockMvc.perform(post("/api/transactions")
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(income)));

        mockMvc.perform(post("/api/transactions")
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(expense)));

        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(5000.00))
                .andExpect(jsonPath("$.totalExpenses").value(1200.00))
                .andExpect(jsonPath("$.balance").value(3800.00))
                .andExpect(jsonPath("$.transactionCount").value(2));
    }
}