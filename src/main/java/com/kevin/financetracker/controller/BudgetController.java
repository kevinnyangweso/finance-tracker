package com.kevin.financetracker.controller;

import com.kevin.financetracker.exception.ResourceNotFoundException;
import com.kevin.financetracker.model.Budget;
import com.kevin.financetracker.service.BudgetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/budgets")
public class BudgetController {

    private final BudgetService budgetService;

    @Autowired
    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    // Create a new budget
    @PostMapping
    public ResponseEntity<Budget> createBudget(@PathVariable Long userId, @Valid @RequestBody Budget budget) {
        Budget createdBudget = budgetService.createBudget(userId, budget);
        return new ResponseEntity<>(createdBudget, HttpStatus.CREATED);
    }

    // Get all budgets for user
    @GetMapping
    public ResponseEntity<List<Budget>> getBudgetsByUser(@PathVariable Long userId) {
        List<Budget> budgets = budgetService.getBudgetsByUser(userId);
        return new ResponseEntity<>(budgets, HttpStatus.OK);
    }

    // Get budget by ID
    @GetMapping("/{budgetId}")
    public ResponseEntity<Budget> getBudgetById(@PathVariable Long userId, @PathVariable Long budgetId) {
        Budget budget = budgetService.getBudgetById(budgetId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with id: " + budgetId));

        verifyBudgetOwnership(userId, budget);
        return new ResponseEntity<>(budget, HttpStatus.OK);
    }

    // Get active budgets for user
    @GetMapping("/active")
    public ResponseEntity<List<Budget>> getActiveBudgets(@PathVariable Long userId) {
        List<Budget> budgets = budgetService.getActiveBudgetsByUser(userId);
        return new ResponseEntity<>(budgets, HttpStatus.OK);
    }

    // Get exceeded budgets for user
    @GetMapping("/exceeded")
    public ResponseEntity<List<Budget>> getExceededBudgets(@PathVariable Long userId) {
        List<Budget> budgets = budgetService.getExceededBudgetsByUser(userId);
        return new ResponseEntity<>(budgets, HttpStatus.OK);
    }

    // Update budget
    @PutMapping("/{budgetId}")
    public ResponseEntity<Budget> updateBudget(@PathVariable Long userId, @PathVariable Long budgetId,
                                               @Valid @RequestBody Budget budgetDetails) {
        Budget existingBudget = budgetService.getBudgetById(budgetId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with id: " + budgetId));

        verifyBudgetOwnership(userId, existingBudget);

        Budget updatedBudget = budgetService.updateBudget(budgetId, budgetDetails);
        return new ResponseEntity<>(updatedBudget, HttpStatus.OK);
    }

    // Delete budget
    @DeleteMapping("/{budgetId}")
    public ResponseEntity<String> deleteBudget(@PathVariable Long userId, @PathVariable Long budgetId) {
        Budget budget = budgetService.getBudgetById(budgetId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with id: " + budgetId));

        verifyBudgetOwnership(userId, budget);

        budgetService.deleteBudget(budgetId);
        return new ResponseEntity<>("Budget deleted successfully", HttpStatus.OK);
    }

    // Reset budget spent amount
    @PostMapping("/{budgetId}/reset")
    public ResponseEntity<String> resetBudgetSpent(@PathVariable Long userId, @PathVariable Long budgetId) {
        Budget budget = budgetService.getBudgetById(budgetId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with id: " + budgetId));

        verifyBudgetOwnership(userId, budget);

        budgetService.resetBudgetSpentAmount(budgetId);
        return new ResponseEntity<>("Budget spent amount reset successfully", HttpStatus.OK);
    }

    // Get budgets ending soon
    @GetMapping("/ending-soon")
    public ResponseEntity<List<Budget>> getBudgetsEndingSoon(@PathVariable Long userId,
                                                             @RequestParam(defaultValue = "7") int days) {
        // Validate days parameter
        if (days <= 0 || days > 365) {
            throw new IllegalArgumentException("Days parameter must be between 1 and 365");
        }

        List<Budget> budgets = budgetService.getBudgetsEndingSoon(userId, days);
        return new ResponseEntity<>(budgets, HttpStatus.OK);
    }

    // Get budget utilization percentage
    @GetMapping("/{budgetId}/utilization")
    public ResponseEntity<Double> getBudgetUtilization(@PathVariable Long userId, @PathVariable Long budgetId) {
        Budget budget = budgetService.getBudgetById(budgetId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with id: " + budgetId));

        verifyBudgetOwnership(userId, budget);

        double utilization = budgetService.calculateBudgetUtilization(budgetId);
        return new ResponseEntity<>(utilization, HttpStatus.OK);
    }

    // Security helper method
    private void verifyBudgetOwnership(Long userId, Budget budget) {
        if (!budget.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Budget not found for this user");
        }
    }
}