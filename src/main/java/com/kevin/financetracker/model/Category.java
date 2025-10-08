package com.kevin.financetracker.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Category name is required")
    @Size(min = 1, max = 100, message = "Category name must be between 1 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9\\s\\-'.,&]+$", message = "Category name can only contain letters, numbers, spaces, and basic punctuation")
    @Column(nullable = false, length = 100)
    private String name;

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    @Column(length = 255)
    private String description;

    @NotNull(message = "Category type is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CategoryType type;

    // Relationships
    @NotNull(message = "User is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_category_id")
    private Category parentCategory;

    @OneToMany(mappedBy = "parentCategory", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Category> subCategories = new ArrayList<>();

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Transaction> transactions = new ArrayList<>();

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Budget> budgets = new ArrayList<>();

    // Constructors
    public Category() {}

    public Category(String name, CategoryType type, User user) {
        this.name = name;
        this.type = type;
        this.user = user;
    }

    public Category(String name, CategoryType type, String description, User user) {
        this(name, type, user);
        this.description = description;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public CategoryType getType() { return type; }
    public void setType(CategoryType type) { this.type = type; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Category getParentCategory() { return parentCategory; }
    public void setParentCategory(Category parentCategory) { this.parentCategory = parentCategory; }

    public List<Category> getSubCategories() { return subCategories; }
    public void setSubCategories(List<Category> subCategories) { this.subCategories = subCategories; }

    public List<Transaction> getTransactions() { return transactions; }
    public void setTransactions(List<Transaction> transactions) { this.transactions = transactions; }

    public List<Budget> getBudgets() { return budgets; }
    public void setBudgets(List<Budget> budgets) { this.budgets = budgets; }

    // Helper methods
    public void addSubCategory(Category subCategory) {
        if (subCategory != null) {
            subCategories.add(subCategory);
            subCategory.setParentCategory(this);
        }
    }

    public void addTransaction(Transaction transaction) {
        if (transaction != null) {
            transactions.add(transaction);
            transaction.setCategory(this);
        }
    }

    public void addBudget(Budget budget) {
        if (budget != null) {
            budgets.add(budget);
            budget.setCategory(this);
        }
    }

    // Custom validation methods
    @AssertTrue(message = "Subcategories must have the same type as parent category")
    public boolean isSubCategoryTypeConsistent() {
        if (parentCategory == null) {
            return true; // This is a top-level category
        }

        // Subcategory must have same type as parent
        return this.type.equals(parentCategory.getType());
    }

    @AssertTrue(message = "Category cannot be its own parent")
    public boolean isNotSelfReferencing() {
        if (parentCategory == null) {
            return true;
        }
        return !this.id.equals(parentCategory.getId());
    }

    @AssertTrue(message = "Parent category must be a top-level category (cannot have its own parent)")
    public boolean isValidParentCategory() {
        if (parentCategory == null) {
            return true; // This is a top-level category
        }

        // Parent category should not have its own parent (max 2 levels deep)
        return parentCategory.getParentCategory() == null;
    }

    // Business logic methods
    @Transient
    public boolean isTopLevel() {
        return parentCategory == null;
    }

    @Transient
    public boolean hasSubCategories() {
        return subCategories != null && !subCategories.isEmpty();
    }

    @Transient
    public boolean hasTransactions() {
        return transactions != null && !transactions.isEmpty();
    }

    @Transient
    public boolean hasBudgets() {
        return budgets != null && !budgets.isEmpty();
    }

    @Override
    public String toString() {
        return "Category{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", type=" + type +
                '}';
    }
}