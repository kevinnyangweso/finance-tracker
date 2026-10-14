package com.financetracker.dto;

import java.math.BigDecimal;

public class MonthlyTrendPoint {

    private String label;    // e.g., "May 2026" or "May"
    private BigDecimal income;
    private BigDecimal expenses;

    public MonthlyTrendPoint() {
    }

    public MonthlyTrendPoint(String label, BigDecimal income, BigDecimal expenses) {
        this.label = label;
        this.income = income;
        this.expenses = expenses;
    }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public BigDecimal getIncome() { return income; }
    public void setIncome(BigDecimal income) { this.income = income; }

    public BigDecimal getExpenses() { return expenses; }
    public void setExpenses(BigDecimal expenses) { this.expenses = expenses; }
}