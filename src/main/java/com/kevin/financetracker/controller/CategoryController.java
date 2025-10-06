package com.kevin.financetracker.controller;

import com.kevin.financetracker.model.Category;
import com.kevin.financetracker.model.CategoryType;
import com.kevin.financetracker.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

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
    public ResponseEntity<?> createCategory(@PathVariable Long userId, @Valid @RequestBody Category category) {
        try {
            Category createdCategory = categoryService.createCategory(userId, category);
            return new ResponseEntity<>(createdCategory, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // Create a subcategory
    @PostMapping("/{parentCategoryId}/subcategories")
    public ResponseEntity<?> createSubcategory(@PathVariable Long userId, @PathVariable Long parentCategoryId,
                                               @Valid @RequestBody Category subcategory) {
        try {
            Category createdSubcategory = categoryService.createSubcategory(parentCategoryId, subcategory);
            return new ResponseEntity<>(createdSubcategory, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // Get all categories for user
    @GetMapping
    public ResponseEntity<List<Category>> getCategoriesByUser(@PathVariable Long userId) {
        List<Category> categories = categoryService.getCategoriesByUser(userId);
        return new ResponseEntity<>(categories, HttpStatus.OK);
    }

    // Get category by ID
    @GetMapping("/{categoryId}")
    public ResponseEntity<?> getCategoryById(@PathVariable Long userId, @PathVariable Long categoryId) {
        Optional<Category> category = categoryService.getCategoryById(categoryId);
        if (category.isPresent() && category.get().getUser().getId().equals(userId)) {
            return new ResponseEntity<>(category.get(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Category not found", HttpStatus.NOT_FOUND);
        }
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
        List<Category> subcategories = categoryService.getSubcategories(parentCategoryId);
        return new ResponseEntity<>(subcategories, HttpStatus.OK);
    }

    // Update category
    @PutMapping("/{categoryId}")
    public ResponseEntity<?> updateCategory(@PathVariable Long userId, @PathVariable Long categoryId,
                                            @Valid @RequestBody Category categoryDetails) {
        try {
            Category updatedCategory = categoryService.updateCategory(categoryId, categoryDetails);
            return new ResponseEntity<>(updatedCategory, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    // Delete category
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long userId, @PathVariable Long categoryId) {
        try {
            categoryService.deleteCategory(categoryId);
            return new ResponseEntity<>("Category deleted successfully", HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (IllegalStateException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }
}