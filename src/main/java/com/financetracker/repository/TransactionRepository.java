package com.financetracker.repository;

import com.financetracker.entity.Transaction;
import com.financetracker.entity.TransactionType;
import com.financetracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // Find all transactions for a user
    List<Transaction> findByUserOrderByDateDesc(User user);

    // Find a specific transaction by ID and user (prevents access to others' data)
    Optional<Transaction> findByIdAndUser(Long id, User user);

    // Filter by user and type
    List<Transaction> findByUserAndTypeOrderByDateDesc(User user, TransactionType type);

    // Filter by user and date range
    List<Transaction> findByUserAndDateBetweenOrderByDateDesc(
            User user, LocalDate startDate, LocalDate endDate);

    // Filter by user and category
    List<Transaction> findByUserAndCategoryOrderByDateDesc(User user, String category);

    // ============================================
    // AGGREGATION QUERIES
    // ============================================

    // Sum all amounts for a user and type
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.user.id = :userId AND t.type = :type")
    BigDecimal sumAmountByUserAndType(
            @Param("userId") Long userId,
            @Param("type") TransactionType type);

    // Sum all amounts for a user and type within a date range
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.user.id = :userId AND t.type = :type " +
            "AND t.date BETWEEN :start AND :end")
    BigDecimal sumAmountByUserAndTypeAndDateBetween(
            @Param("userId") Long userId,
            @Param("type") TransactionType type,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    // Count transactions for a user
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.user.id = :userId")
            Long countByUserId(@Param("userId") Long userId);

    // Category breakdown: sum by category, for a user and type
    @Query("SELECT t.category.id, t.category.name, SUM(t.amount), COUNT(t) " +
            "FROM Transaction t " +
            "WHERE t.user.id = :userId AND t.type = :type " +
            "GROUP BY t.category.id, t.category.name " +
            "ORDER BY SUM(t.amount) DESC")
    List<Object[]> sumByCategory(
            @Param("userId") Long userId,
            @Param("type") TransactionType type);
}