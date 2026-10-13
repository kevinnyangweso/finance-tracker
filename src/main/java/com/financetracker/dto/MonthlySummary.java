package com.financetracker.dto;

import java.math.BigDecimal;

public class MonthlySummary {

    private String month;
    private int year;
    private BigDecimal income;
    private BigDecimal expenses;
    private BigDecimal balance;

    public MonthlySummary() {
    }

    public MonthlySummary(String month, int year, BigDecimal income,
                          BigDecimal expenses, BigDecimal balance) {
        this.month = month;
        this.year = year;
        this.income = income;
        this.expenses = expenses;
        this.balance = balance;
    }

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public BigDecimal getIncome() { return income; }
    public void setIncome(BigDecimal income) { this.income = income; }

    public BigDecimal getExpenses() { return expenses; }
    public void setExpenses(BigDecimal expenses) { this.expenses = expenses; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
}