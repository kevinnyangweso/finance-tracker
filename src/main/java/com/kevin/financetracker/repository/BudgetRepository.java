package com.kevin.financetracker.repository;

import com.kevin.financetracker.model.Budget;
import com.kevin.financetracker.model.BudgetPeriod;
import com.kevin.financetracker.model.User;
import com.kevin.financetracker.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    List<Budget> findByUser(User user);
    List<Budget> findByUserAndPeriod(User user, BudgetPeriod period);
    Optional<Budget> findByUserAndCategory(User user, Category category);

    @Query("SELECT b FROM Budget b WHERE b.user = :user AND :currentDate BETWEEN b.startDate AND b.endDate")
    List<Budget> findActiveBudgetsByUser(@Param("user") User user, @Param("currentDate") LocalDate currentDate);

    @Query("SELECT b FROM Budget b WHERE b.user = :user AND b.spent > b.amount")
    List<Budget> findExceededBudgetsByUser(@Param("user") User user);

    @Query("SELECT b FROM Budget b WHERE b.user = :user AND b.endDate BETWEEN :start AND :end")
    List<Budget> findBudgetsEndingSoon(
            @Param("user") User user,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    @Modifying
    @Query("UPDATE Budget b SET b.spent = b.spent + :amount WHERE b.id = :budgetId")
    void updateSpentAmount(@Param("budgetId") Long budgetId, @Param("amount") BigDecimal amount);

    @Query("SELECT COUNT(b) > 0 FROM Budget b WHERE b.user = :user AND b.category = :category AND " +
            "((b.startDate BETWEEN :startDate AND :endDate) OR (b.endDate BETWEEN :startDate AND :endDate))")
    boolean existsByUserAndCategoryAndDateRange(
            @Param("user") User user,
            @Param("category") Category category,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}