package com.kevin.financetracker.repository;

import com.kevin.financetracker.model.Transaction;
import com.kevin.financetracker.model.TransactionType;
import com.kevin.financetracker.model.User;
import com.kevin.financetracker.model.Account;
import com.kevin.financetracker.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // Find all transactions for a specific user
    List<Transaction> findByUser(User user);

    // Find transactions by user and type
    List<Transaction> findByUserAndType(User user, TransactionType type);

    // Find transactions for a specific account
    List<Transaction> findByAccount(Account account);

    // Find transactions for a specific category
    List<Transaction> findByCategory(Category category);

    // Find transactions within a date range for a user
    List<Transaction> findByUserAndTransactionDateBetween(User user, LocalDateTime start, LocalDateTime end);

    // Find transactions with amount greater than specified amount
    List<Transaction> findByUserAndAmountGreaterThan(User user, BigDecimal amount);

    // Find transactions with amount less than specified amount
    List<Transaction> findByUserAndAmountLessThan(User user, BigDecimal amount);

    // Find transactions by description (search)
    @Query("SELECT t FROM Transaction t WHERE t.user = :user AND LOWER(t.description) LIKE LOWER(CONCAT('%', :description, '%'))")
    List<Transaction> findByUserAndDescriptionContainingIgnoreCase(
            @Param("user") User user,
            @Param("description")
            String description);

    // Custom query to get total income for a user in date range
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user = :user AND t.type = 'INCOME' AND t.transactionDate BETWEEN :start AND :end")
    Optional<BigDecimal> getTotalIncomeByUserAndDateRange(
            @Param("user") User user,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    // Custom query to get total expenses for a user in date range
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user = :user AND t.type = 'EXPENSE' AND t.transactionDate BETWEEN :start AND :end")
    Optional<BigDecimal> getTotalExpensesByUserAndDateRange(
            @Param("user") User user,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    // Find recent transactions for a user (limit results)
    @Query("SELECT t FROM Transaction t WHERE t.user = :user ORDER BY t.transactionDate DESC LIMIT :limit")
    List<Transaction> findRecentTransactionsByUser(@Param("user") User user, @Param("limit") int limit);

    // Custom query to get monthly summary
    @Query("SELECT YEAR(t.transactionDate), MONTH(t.transactionDate), t.type, SUM(t.amount) " +
            "FROM Transaction t WHERE t.user = :user AND t.transactionDate BETWEEN :start AND :end " +
            "AND t.type IN (com.kevin.financetracker.model.TransactionType.INCOME, com.kevin.financetracker.model.TransactionType.EXPENSE) " +
            "GROUP BY YEAR(t.transactionDate), MONTH(t.transactionDate), t.type " +
            "ORDER BY YEAR(t.transactionDate) DESC, MONTH(t.transactionDate) DESC")
    List<Object[]> getMonthlySummary(
            @Param("user") User user,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}