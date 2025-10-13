package com.kevin.financetracker.service;

import com.kevin.financetracker.dto.CategoryDTO;
import com.kevin.financetracker.exception.DuplicateResourceException;
import com.kevin.financetracker.exception.ResourceNotFoundException;
import com.kevin.financetracker.exception.ValidationException;
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
import java.util.stream.Collectors;

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

    // Create a new category - returns DTO
    public CategoryDTO createCategory(Long userId, Category category) {
        // category should already have name, description, type set from the request DTO
        // service will set the user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Check if category name already exists for this user
        if (categoryRepository.existsByUserAndName(user, category.getName())) {
            throw new DuplicateResourceException("Category name already exists for this user: " + category.getName());
        }

        category.setUser(user); // Set the user from the path variable
        Category savedCategory = categoryRepository.save(category);
        return convertToDTO(savedCategory);
    }

    // Create a subcategory - returns DTO
    public CategoryDTO createSubcategory(Long parentCategoryId, Category subcategory) {
        Category parentCategory = categoryRepository.findById(parentCategoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with id: " + parentCategoryId));

        // Check if subcategory name already exists for this user
        if (categoryRepository.existsByUserAndName(parentCategory.getUser(), subcategory.getName())) {
            throw new DuplicateResourceException("Category name already exists: " + subcategory.getName());
        }

        // Validate that subcategory type matches parent category type
        if (subcategory.getType() != null && !subcategory.getType().equals(parentCategory.getType())) {
            throw new ValidationException("Subcategory type must match parent category type");
        }

        subcategory.setUser(parentCategory.getUser());
        subcategory.setParentCategory(parentCategory);
        subcategory.setType(parentCategory.getType()); // Ensure type matches parent

        Category savedSubcategory = categoryRepository.save(subcategory);
        parentCategory.addSubCategory(savedSubcategory);

        return convertToDTO(savedSubcategory);
    }

    // Get category by ID
    @Transactional(readOnly = true)
    public Optional<Category> getCategoryById(Long id) {
        return categoryRepository.findById(id);
    }

    // Get all categories for a user - returns DTOs
    @Transactional(readOnly = true)
    public List<CategoryDTO> getCategoriesByUser(Long userId) {
        List<Category> categories = categoryRepository.findByUser(userId);
        return categories.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Get categories by type for a user - returns DTOs
    @Transactional(readOnly = true)
    public List<CategoryDTO> getCategoriesByUserAndType(Long userId, CategoryType type) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        List<Category> categories = categoryRepository.findByUserAndType(user, type);
        return categories.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Get top-level categories (no parent) for a user - returns DTOs
    @Transactional(readOnly = true)
    public List<CategoryDTO> getTopLevelCategoriesByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        List<Category> categories = categoryRepository.findByUserAndParentCategoryIsNull(user);
        return categories.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Get subcategories of a parent category - returns DTOs
    @Transactional(readOnly = true)
    public List<CategoryDTO> getSubcategories(Long parentCategoryId) {
        Category parentCategory = categoryRepository.findById(parentCategoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with id: " + parentCategoryId));
        List<Category> subcategories = categoryRepository.findByParentCategory(parentCategory);
        return subcategories.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Update category - returns DTO
    public CategoryDTO updateCategory(Long categoryId, Category categoryDetails) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));

        // Update fields if provided
        if (categoryDetails.getName() != null && !categoryDetails.getName().trim().isEmpty()) {
            // Check if new name is unique for this user
            if (!category.getName().equals(categoryDetails.getName()) &&
                    categoryRepository.existsByUserAndName(category.getUser(), categoryDetails.getName())) {
                throw new DuplicateResourceException("Category name already exists: " + categoryDetails.getName());
            }
            category.setName(categoryDetails.getName());
        }

        if (categoryDetails.getDescription() != null) {
            category.setDescription(categoryDetails.getDescription());
        }

        if (categoryDetails.getType() != null) {
            // If it's a parent category, validate that all subcategories have the same type
            if (!category.getSubCategories().isEmpty()) {
                boolean subcategoriesHaveSameType = category.getSubCategories().stream()
                        .allMatch(sub -> sub.getType().equals(categoryDetails.getType()));
                if (!subcategoriesHaveSameType) {
                    throw new ValidationException("Cannot change category type when subcategories have different types");
                }
            }
            category.setType(categoryDetails.getType());
        }

        Category updatedCategory = categoryRepository.save(category);
        return convertToDTO(updatedCategory);
    }

    // Delete category
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        // Check if category has transactions or budgets
        if (!category.getTransactions().isEmpty()) {
            throw new ValidationException("Cannot delete category with existing transactions");
        }
        if (!category.getBudgets().isEmpty()) {
            throw new ValidationException("Cannot delete category with existing budgets");
        }

        // For categories with subcategories, consider alternative deletion strategies:
        // Option 1: Prevent deletion (current implementation)
        // Option 2: Cascade delete (risky)
        // Option 3: Move subcategories to parent's parent or make them top-level
        if (!category.getSubCategories().isEmpty()) {
            throw new ValidationException("Cannot delete category with existing subcategories. Please delete or reassign subcategories first.");
        }

        categoryRepository.delete(category);
    }

    // Search categories by name for a user - returns DTOs
    @Transactional(readOnly = true)
    public List<CategoryDTO> searchCategoriesByName(Long userId, String name) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        List<Category> categories = categoryRepository.findByUserAndNameContainingIgnoreCase(user.getId(), name);
        return categories.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Convert Category entity to CategoryDTO
    public CategoryDTO convertToDTO(Category category) {
        if (category == null) {
            return null;
        }

        Long parentCategoryId = category.getParentCategory() != null ?
                category.getParentCategory().getId() : null;

        // Convert subcategories to DTOs recursively
        List<CategoryDTO> subCategoryDTOs = category.getSubCategories().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return new CategoryDTO(
                category.getId(),
                category.getName(),
                category.getType(),
                category.getDescription(),
                parentCategoryId,
                subCategoryDTOs
        );
    }

    // Additional helper methods

    // Check if user owns the category
    @Transactional(readOnly = true)
    public boolean isCategoryOwnedByUser(Long categoryId, Long userId) {
        return categoryRepository.existsByIdAndUserId(categoryId, userId);
    }

    // Get category with user validation
    @Transactional(readOnly = true)
    public Category getCategoryByIdAndUserId(Long categoryId, Long userId) {
        return categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found with id: " + categoryId + " for user: " + userId));
    }

    // Get category DTO with user validation
    @Transactional(readOnly = true)
    public CategoryDTO getCategoryDTOByIdAndUserId(Long categoryId, Long userId) {
        Category category = getCategoryByIdAndUserId(categoryId, userId);
        return convertToDTO(category);
    }

    // Check if category can be deleted (no transactions, budgets, or subcategories)
    @Transactional(readOnly = true)
    public boolean isCategoryDeletable(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));

        return category.getTransactions().isEmpty() &&
                category.getBudgets().isEmpty() &&
                category.getSubCategories().isEmpty();
    }

    // Get categories with transaction count
    @Transactional(readOnly = true)
    public List<CategoryDTO> getCategoriesWithTransactionCount(Long userId) {
        List<Category> categories = categoryRepository.findByUser(userId);
        return categories.stream()
                .map(category -> {
                    CategoryDTO dto = convertToDTO(category);
                    // You can add transaction count here if needed
                    return dto;
                })
                .collect(Collectors.toList());
    }
}