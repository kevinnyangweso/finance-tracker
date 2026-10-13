package com.financetracker.dto;

import java.math.BigDecimal;

public class CategoryBreakdown {

    private Long categoryId;
    private String categoryName;
    private BigDecimal total;
    private Long transactionCount;

    public CategoryBreakdown() {
    }

    public CategoryBreakdown(Long categoryId, String categoryName,
                             BigDecimal total, Long transactionCount) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.total = total;
        this.transactionCount = transactionCount;
    }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public Long getTransactionCount() { return transactionCount; }
    public void setTransactionCount(Long transactionCount) { this.transactionCount = transactionCount; }
}