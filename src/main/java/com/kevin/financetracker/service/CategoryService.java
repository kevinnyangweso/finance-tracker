package com.kevin.financetracker.service;

import com.kevin.financetracker.model.Category;
import com.kevin.financetracker.model.CategoryType;
import com.kevin.financetracker.model.User;
import com.kevin.financetracker.repository.CategoryRepository;
import com.kevin.financetracker.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Autowired
    public CategoryService(CategoryRepository categoryRepository, UserRepository userRepository) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    // Create a new category
    public Category createCategory(Long userId, Category category) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        // Check if category name already exists for this user
        if (categoryRepository.existsByUserAndName(user, category.getName())) {
            throw new IllegalArgumentException("Category name already exists for this user: " + category.getName());
        }

        category.setUser(user);
        return categoryRepository.save(category);
    }

    // Create a subcategory
    public Category createSubcategory(Long parentCategoryId, Category subcategory) {
        Category parentCategory = categoryRepository.findById(parentCategoryId)
                .orElseThrow(() -> new IllegalArgumentException("Parent category not found with id: " + parentCategoryId));

        // Check if subcategory name already exists for this user
        if (categoryRepository.existsByUserAndName(parentCategory.getUser(), subcategory.getName())) {
            throw new IllegalArgumentException("Category name already exists: " + subcategory.getName());
        }

        subcategory.setUser(parentCategory.getUser());
        subcategory.setParentCategory(parentCategory);

        Category savedSubcategory = categoryRepository.save(subcategory);
        parentCategory.addSubCategory(savedSubcategory);

        return savedSubcategory;
    }

    // Get category by ID
    @Transactional(readOnly = true)
    public Optional<Category> getCategoryById(Long id) {
        return categoryRepository.findById(id);
    }

    // Get all categories for a user
    @Transactional(readOnly = true)
    public List<Category> getCategoriesByUser(Long userId) {
        return categoryRepository.findByUser(userId);
    }

    // Get categories by type for a user
    @Transactional(readOnly = true)
    public List<Category> getCategoriesByUserAndType(Long userId, CategoryType type) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        return categoryRepository.findByUserAndType(user, type);
    }

    // Get top-level categories (no parent) for a user
    @Transactional(readOnly = true)
    public List<Category> getTopLevelCategoriesByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        return categoryRepository.findByUserAndParentCategoryIsNull(user);
    }

    // Get subcategories of a parent category
    @Transactional(readOnly = true)
    public List<Category> getSubcategories(Long parentCategoryId) {
        Category parentCategory = categoryRepository.findById(parentCategoryId)
                .orElseThrow(() -> new IllegalArgumentException("Parent category not found with id: " + parentCategoryId));
        return categoryRepository.findByParentCategory(parentCategory);
    }

    // Update category
    public Category updateCategory(Long categoryId, Category categoryDetails) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with id: " + categoryId));

        // Update fields if provided
        if (categoryDetails.getName() != null) {
            // Check if new name is unique for this user
            if (!category.getName().equals(categoryDetails.getName()) &&
                    categoryRepository.existsByUserAndName(category.getUser(), categoryDetails.getName())) {
                throw new IllegalArgumentException("Category name already exists: " + categoryDetails.getName());
            }
            category.setName(categoryDetails.getName());
        }
        if (categoryDetails.getDescription() != null) {
            category.setDescription(categoryDetails.getDescription());
        }
        if (categoryDetails.getType() != null) {
            category.setType(categoryDetails.getType());
        }

        return categoryRepository.save(category);
    }

    // Delete category
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with id: " + id));

        // Check if category has transactions or budgets
        if (!category.getTransactions().isEmpty()) {
            throw new IllegalStateException("Cannot delete category with existing transactions");
        }
        if (!category.getBudgets().isEmpty()) {
            throw new IllegalStateException("Cannot delete category with existing budgets");
        }
        if (!category.getSubCategories().isEmpty()) {
            throw new IllegalStateException("Cannot delete category with existing subcategories");
        }

        categoryRepository.delete(category);
    }

    // Search categories by name for a user
    @Transactional(readOnly = true)
    public List<Category> searchCategoriesByName(Long userId, String name) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        return categoryRepository.findByUserAndNameContainingIgnoreCase(user, name);
    }
}