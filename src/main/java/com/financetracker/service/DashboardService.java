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
import com.financetracker.dto.MonthlyTrendPoint;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Locale;

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

        // === Monthly trend (last 6 months) ===
        List<MonthlyTrendPoint> monthlyTrend = buildMonthlyTrend(userId);       

        return new DashboardResponse(
                totalIncome,
                totalExpenses,
                balance,
                transactionCount,
                currentMonth,
                breakdown,
                monthlyTrend
        );
    }

    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + username));
    }

    private List<MonthlyTrendPoint> buildMonthlyTrend(Long userId) {
    LocalDate today = LocalDate.now();
    LocalDate startDate = today.minusMonths(5).withDayOfMonth(1);

    // Fetch raw rows from the DB
    List<Object[]> rows = transactionRepository.sumByMonth(userId, startDate);

    // Build a map: "YYYY-MM" → {income, expenses}
    java.util.Map<String, BigDecimal> incomeMap = new java.util.HashMap<>();
    java.util.Map<String, BigDecimal> expenseMap = new java.util.HashMap<>();

    for (Object[] row : rows) {
        Integer year = ((Number) row[0]).intValue();
        Integer month = ((Number) row[1]).intValue();
        TransactionType type = (TransactionType) row[2];
        BigDecimal sum = (BigDecimal) row[3];

        String key = year + "-" + String.format("%02d", month);

        if (type == TransactionType.INCOME) {
            incomeMap.put(key, sum);
        } else {
            expenseMap.put(key, sum);
        }
    }

    // Build the final list — one point per month, in chronological order
    List<MonthlyTrendPoint> trend = new ArrayList<>();
    for (int i = 5; i >= 0; i--) {
        LocalDate monthStart = today.minusMonths(i).withDayOfMonth(1);
        String key = monthStart.getYear() + "-" + String.format("%02d", monthStart.getMonthValue());
        String label = monthStart.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                + " " + monthStart.getYear();

        trend.add(new MonthlyTrendPoint(
                label,
                incomeMap.getOrDefault(key, BigDecimal.ZERO),
                expenseMap.getOrDefault(key, BigDecimal.ZERO)
        ));
    }

    return trend;
}
}