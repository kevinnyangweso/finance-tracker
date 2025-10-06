package com.kevin.financetracker.service;

import com.kevin.financetracker.model.Budget;
import com.kevin.financetracker.model.BudgetPeriod;
import com.kevin.financetracker.model.Category;
import com.kevin.financetracker.model.User;
import com.kevin.financetracker.repository.BudgetRepository;
import com.kevin.financetracker.repository.CategoryRepository;
import com.kevin.financetracker.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Autowired
    public BudgetService(BudgetRepository budgetRepository,
                         CategoryRepository categoryRepository,
                         UserRepository userRepository) {
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    // Create a new budget
    public Budget createBudget(Long userId, Budget budget) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        Category category = categoryRepository.findById(budget.getCategory().getId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        // Verify category belongs to user
        if (!category.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Category does not belong to user");
        }

        // Check for overlapping budgets
        if (budgetRepository.existsByUserAndCategoryAndDateRange(user, category,
                budget.getStartDate(), budget.getEndDate())) {
            throw new IllegalArgumentException("Budget already exists for this category and date range");
        }

        budget.setUser(user);
        budget.setCategory(category);
        budget.setSpent(BigDecimal.ZERO); // Initialize spent amount

        return budgetRepository.save(budget);
    }

    // Get budget by ID
    @Transactional(readOnly = true)
    public Optional<Budget> getBudgetById(Long id) {
        return budgetRepository.findById(id);
    }

    // Get all budgets for a user
    @Transactional(readOnly = true)
    public List<Budget> getBudgetsByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        return budgetRepository.findByUser(user);
    }

    // Get active budgets for a user
    @Transactional(readOnly = true)
    public List<Budget> getActiveBudgetsByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        return budgetRepository.findActiveBudgetsByUser(user, LocalDate.now());
    }

    // Get exceeded budgets for a user
    @Transactional(readOnly = true)
    public List<Budget> getExceededBudgetsByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        return budgetRepository.findExceededBudgetsByUser(user);
    }

    // Update budget
    public Budget updateBudget(Long budgetId, Budget budgetDetails) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new IllegalArgumentException("Budget not found with id: " + budgetId));

        // Update fields if provided
        if (budgetDetails.getName() != null) {
            budget.setName(budgetDetails.getName());
        }
        if (budgetDetails.getAmount() != null) {
            budget.setAmount(budgetDetails.getAmount());
        }
        if (budgetDetails.getStartDate() != null) {
            budget.setStartDate(budgetDetails.getStartDate());
        }
        if (budgetDetails.getEndDate() != null) {
            budget.setEndDate(budgetDetails.getEndDate());
        }
        if (budgetDetails.getPeriod() != null) {
            budget.setPeriod(budgetDetails.getPeriod());
        }

        return budgetRepository.save(budget);
    }

    // Delete budget
    public void deleteBudget(Long id) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Budget not found with id: " + id));
        budgetRepository.delete(budget);
    }

    // Update spent amount for a budget (called when transactions are added)
    public void updateBudgetSpentAmount(Long userId, Long categoryId, BigDecimal amount) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        // Find active budget for this category
        List<Budget> activeBudgets = budgetRepository.findActiveBudgetsByUser(user, LocalDate.now());
        Optional<Budget> matchingBudget = activeBudgets.stream()
                .filter(budget -> budget.getCategory().getId().equals(categoryId))
                .findFirst();

        if (matchingBudget.isPresent()) {
            Budget budget = matchingBudget.get();
            budget.addSpentAmount(amount);
            budgetRepository.save(budget);
        }
    }

    // Reset spent amount for a budget (e.g., at the start of a new period)
    public void resetBudgetSpentAmount(Long budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new IllegalArgumentException("Budget not found with id: " + budgetId));
        budget.resetSpent();
        budgetRepository.save(budget);
    }

    // Get budgets ending soon
    @Transactional(readOnly = true)
    public List<Budget> getBudgetsEndingSoon(Long userId, int days) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        LocalDate start = LocalDate.now();
        LocalDate end = start.plusDays(days);

        return budgetRepository.findBudgetsEndingSoon(user, start, end);
    }

    // Calculate budget utilization percentage
    @Transactional(readOnly = true)
    public double calculateBudgetUtilization(Long budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new IllegalArgumentException("Budget not found with id: " + budgetId));
        return budget.getProgressPercentage();
    }
}