package com.financetracker.dto;

import java.math.BigDecimal;
import java.util.List;

public class DashboardResponse {

    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private BigDecimal balance;
    private Long transactionCount;
    private MonthlySummary currentMonth;
    private List<CategoryBreakdown> categoryBreakdown;

    public DashboardResponse() {
    }

    public DashboardResponse(BigDecimal totalIncome, BigDecimal totalExpenses,
                             BigDecimal balance, Long transactionCount,
                             MonthlySummary currentMonth,
                             List<CategoryBreakdown> categoryBreakdown) {
        this.totalIncome = totalIncome;
        this.totalExpenses = totalExpenses;
        this.balance = balance;
        this.transactionCount = transactionCount;
        this.currentMonth = currentMonth;
        this.categoryBreakdown = categoryBreakdown;
    }

    public BigDecimal getTotalIncome() { return totalIncome; }
    public void setTotalIncome(BigDecimal totalIncome) { this.totalIncome = totalIncome; }

    public BigDecimal getTotalExpenses() { return totalExpenses; }
    public void setTotalExpenses(BigDecimal totalExpenses) { this.totalExpenses = totalExpenses; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public Long getTransactionCount() { return transactionCount; }
    public void setTransactionCount(Long transactionCount) { this.transactionCount = transactionCount; }

    public MonthlySummary getCurrentMonth() { return currentMonth; }
    public void setCurrentMonth(MonthlySummary currentMonth) { this.currentMonth = currentMonth; }

    public List<CategoryBreakdown> getCategoryBreakdown() { return categoryBreakdown; }
    public void setCategoryBreakdown(List<CategoryBreakdown> categoryBreakdown) {
        this.categoryBreakdown = categoryBreakdown;
    }
}