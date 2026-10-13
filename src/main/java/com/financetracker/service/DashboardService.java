package com.financetracker.service;

import com.financetracker.dto.CategoryBreakdown;
import com.financetracker.dto.DashboardResponse;
import com.financetracker.dto.MonthlySummary;
import com.financetracker.entity.TransactionType;
import com.financetracker.entity.User;
import com.financetracker.exception.ResourceNotFoundException;
import com.financetracker.repository.TransactionRepository;
import com.financetracker.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public DashboardService(TransactionRepository transactionRepository,
                            UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse getSummary(String username) {
        User user = getUserByUsername(username);
        Long userId = user.getId();

        // === Overall totals ===
        BigDecimal totalIncome = transactionRepository
                .sumAmountByUserAndType(userId, TransactionType.INCOME);
        BigDecimal totalExpenses = transactionRepository
                .sumAmountByUserAndType(userId, TransactionType.EXPENSE);
        BigDecimal balance = totalIncome.subtract(totalExpenses);

        Long transactionCount = transactionRepository.countByUserId(userId);

        // === Current month summary ===
        LocalDate today = LocalDate.now();
        LocalDate firstOfMonth = today.withDayOfMonth(1);
        LocalDate lastOfMonth = today.withDayOfMonth(today.lengthOfMonth());

        BigDecimal monthIncome = transactionRepository
                .sumAmountByUserAndTypeAndDateBetween(
                        userId, TransactionType.INCOME, firstOfMonth, lastOfMonth);
        BigDecimal monthExpenses = transactionRepository
                .sumAmountByUserAndTypeAndDateBetween(
                        userId, TransactionType.EXPENSE, firstOfMonth, lastOfMonth);

        MonthlySummary currentMonth = new MonthlySummary(
                today.getMonth().name(),
                today.getYear(),
                monthIncome,
                monthExpenses,
                monthIncome.subtract(monthExpenses)
        );

        // === Category breakdown (expenses only) ===
        List<Object[]> rows = transactionRepository
                .sumByCategory(userId, TransactionType.EXPENSE);

        List<CategoryBreakdown> breakdown = rows.stream()
                .map(row -> new CategoryBreakdown(
                        (Long) row[0],
                        (String) row[1],
                        (BigDecimal) row[2],
                        (Long) row[3]
                ))
                .collect(Collectors.toList());

        return new DashboardResponse(
                totalIncome,
                totalExpenses,
                balance,
                transactionCount,
                currentMonth,
                breakdown
        );
    }

    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + username));
    }
}