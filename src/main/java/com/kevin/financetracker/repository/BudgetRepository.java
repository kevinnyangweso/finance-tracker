package com.kevin.financetracker.repository;

import com.kevin.financetracker.model.Budget;
import com.kevin.financetracker.model.BudgetPeriod;
import com.kevin.financetracker.model.User;
import com.kevin.financetracker.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    // Find all budgets for a specific user
    List<Budget> findByUser(User user);

    // Find budgets by user and period
    List<Budget> findByUserAndPeriod(User user, BudgetPeriod period);

    // Find budget by category and user
    Optional<Budget> findByUserAndCategory(User user, Category category);

    // Find active budgets (current date within start and end date)
    @Query("SELECT b FROM Budget b WHERE b.user = :user AND :currentDate BETWEEN b.startDate AND b.endDate")
    List<Budget> findActiveBudgetsByUser(@Param("user") User user, @Param("currentDate") LocalDate currentDate);

    // Find budgets that are exceeded (spent > amount)
    @Query("SELECT b FROM Budget b WHERE b.user = :user AND b.spent > b.amount")
    List<Budget> findExceededBudgetsByUser(@Param("user") User user);

    // Find budgets ending soon (within next 7 days)
    @Query("SELECT b FROM Budget b WHERE b.user = :user AND b.endDate BETWEEN :start AND :end")
    List<Budget> findBudgetsEndingSoon(
            @Param("user") User user,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    // Custom query to update spent amount (you'll call this from service)
    @Query("UPDATE Budget b SET b.spent = b.spent + :amount WHERE b.id = :budgetId")
    void updateSpentAmount(@Param("budgetId") Long budgetId, @Param("amount") BigDecimal amount);

    // Check if budget exists for category and date range
    @Query("SELECT COUNT(b) > 0 FROM Budget b WHERE b.user = :user AND b.category = :category AND " +
            "((b.startDate BETWEEN :startDate AND :endDate) OR (b.endDate BETWEEN :startDate AND :endDate))")
    boolean existsByUserAndCategoryAndDateRange(
            @Param("user") User user,
            @Param("category") Category category,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}