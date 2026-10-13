package com.financetracker.repository;

import com.financetracker.entity.Transaction;
import com.financetracker.entity.TransactionType;
import com.financetracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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

    // Sum amounts by user and type (useful for dashboard)
    // We'll add this in a later phase
}