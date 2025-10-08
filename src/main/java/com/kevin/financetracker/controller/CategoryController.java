package com.kevin.financetracker.controller;

import com.kevin.financetracker.exception.ResourceNotFoundException;
import com.kevin.financetracker.model.Category;
import com.kevin.financetracker.model.CategoryType;
import com.kevin.financetracker.service.CategoryService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/categories")
public class CategoryController {

    private final CategoryService categoryService;

    @Autowired
    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    // Create a new category
    @PostMapping
    public ResponseEntity<Category> createCategory(@PathVariable Long userId, @Valid @RequestBody Category category) {
        Category createdCategory = categoryService.createCategory(userId, category);
        return new ResponseEntity<>(createdCategory, HttpStatus.CREATED);
    }

    // Create a subcategory
    @PostMapping("/{parentCategoryId}/subcategories")
    public ResponseEntity<Category> createSubcategory(@PathVariable Long userId, @PathVariable Long parentCategoryId,
                                                      @Valid @RequestBody Category subcategory) {
        // Verify parent category belongs to user
        Category parentCategory = categoryService.getCategoryById(parentCategoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with id: " + parentCategoryId));
        verifyCategoryOwnership(userId, parentCategory);

        Category createdSubcategory = categoryService.createSubcategory(parentCategoryId, subcategory);
        return new ResponseEntity<>(createdSubcategory, HttpStatus.CREATED);
    }

    // Get all categories for user
    @GetMapping
    public ResponseEntity<List<Category>> getCategoriesByUser(@PathVariable Long userId) {
        List<Category> categories = categoryService.getCategoriesByUser(userId);
        return new ResponseEntity<>(categories, HttpStatus.OK);
    }

    // Get category by ID
    @GetMapping("/{categoryId}")
    public ResponseEntity<Category> getCategoryById(@PathVariable Long userId, @PathVariable Long categoryId) {
        Category category = categoryService.getCategoryById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));

        verifyCategoryOwnership(userId, category);
        return new ResponseEntity<>(category, HttpStatus.OK);
    }

    // Get categories by type for user
    @GetMapping("/type/{type}")
    public ResponseEntity<List<Category>> getCategoriesByType(@PathVariable Long userId, @PathVariable CategoryType type) {
        List<Category> categories = categoryService.getCategoriesByUserAndType(userId, type);
        return new ResponseEntity<>(categories, HttpStatus.OK);
    }

    // Get top-level categories for user
    @GetMapping("/top-level")
    public ResponseEntity<List<Category>> getTopLevelCategories(@PathVariable Long userId) {
        List<Category> categories = categoryService.getTopLevelCategoriesByUser(userId);
        return new ResponseEntity<>(categories, HttpStatus.OK);
    }

    // Get subcategories of a parent category
    @GetMapping("/{parentCategoryId}/subcategories")
    public ResponseEntity<List<Category>> getSubcategories(@PathVariable Long userId, @PathVariable Long parentCategoryId) {
        // Verify parent category belongs to user
        Category parentCategory = categoryService.getCategoryById(parentCategoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with id: " + parentCategoryId));
        verifyCategoryOwnership(userId, parentCategory);

        List<Category> subcategories = categoryService.getSubcategories(parentCategoryId);
        return new ResponseEntity<>(subcategories, HttpStatus.OK);
    }

    // Update category
    @PutMapping("/{categoryId}")
    public ResponseEntity<Category> updateCategory(@PathVariable Long userId, @PathVariable Long categoryId,
                                                   @Valid @RequestBody Category categoryDetails) {
        // Verify category belongs to user
        Category existingCategory = categoryService.getCategoryById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));
        verifyCategoryOwnership(userId, existingCategory);

        Category updatedCategory = categoryService.updateCategory(categoryId, categoryDetails);
        return new ResponseEntity<>(updatedCategory, HttpStatus.OK);
    }

    // Delete category
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<String> deleteCategory(@PathVariable Long userId, @PathVariable Long categoryId) {
        // Verify category belongs to user
        Category category = categoryService.getCategoryById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));
        verifyCategoryOwnership(userId, category);

        categoryService.deleteCategory(categoryId);
        return new ResponseEntity<>("Category deleted successfully", HttpStatus.OK);
    }

    // Search categories by name for user
    @GetMapping("/search")
    public ResponseEntity<List<Category>> searchCategoriesByName(@PathVariable Long userId,
                                                                 @RequestParam @NotBlank String name) {
        if (name.trim().length() < 2) {
            throw new IllegalArgumentException("Search term must be at least 2 characters long");
        }

        List<Category> categories = categoryService.searchCategoriesByName(userId, name);
        return new ResponseEntity<>(categories, HttpStatus.OK);
    }

    // Security helper method
    private void verifyCategoryOwnership(Long userId, Category category) {
        if (!category.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Category not found for this user");
        }
    }
}