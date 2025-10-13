package com.kevin.financetracker.controller;

import com.kevin.financetracker.dto.CategoryRequestDTO;
import com.kevin.financetracker.exception.ResourceNotFoundException;
import com.kevin.financetracker.model.Category;
import com.kevin.financetracker.dto.CategoryDTO;
import com.kevin.financetracker.model.CategoryType;
import com.kevin.financetracker.service.CategoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/categories")
public class CategoryController {

    private static final Logger logger = LoggerFactory.getLogger(CategoryController.class);

    @Autowired
    private CategoryService categoryService;

    @PostMapping
    public ResponseEntity<CategoryDTO> createCategory(@PathVariable Long userId,
                                                      @RequestBody Category categoryRequest) {
        logger.info("Creating category for user {}: {}", userId, categoryRequest.getName());

        // Convert CategoryRequestDTO to Category entity
        Category category = new Category();
        category.setName(categoryRequest.getName());
        category.setDescription(categoryRequest.getDescription());
        category.setType(categoryRequest.getType());
        // User will be set by the service

        CategoryDTO createdCategory = categoryService.createCategory(userId, category);
        return new ResponseEntity<>(createdCategory, HttpStatus.CREATED);
    }

    @PostMapping("/{parentCategoryId}/subcategories")
    public ResponseEntity<CategoryDTO> createSubcategory(@PathVariable Long userId,
                                                         @PathVariable Long parentCategoryId,
                                                         @RequestBody Category subcategoryRequest) {
        logger.info("Creating subcategory for parent category {} and user {}", parentCategoryId, userId);

        Category parentCategory = categoryService.getCategoryById(parentCategoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with id: " + parentCategoryId));

        if (!parentCategory.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Category not found for this user");
        }

        // Convert DTO to Entity
        Category subcategory = new Category();
        subcategory.setName(subcategoryRequest.getName());
        subcategory.setDescription(subcategoryRequest.getDescription());
        subcategory.setType(subcategoryRequest.getType());

        CategoryDTO createdSubcategory = categoryService.createSubcategory(parentCategoryId, subcategory);
        return new ResponseEntity<>(createdSubcategory, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<CategoryDTO>> getCategoriesByUser(@PathVariable Long userId) {
        logger.info("Getting all categories for user {}", userId);
        List<CategoryDTO> categories = categoryService.getCategoriesByUser(userId);
        return new ResponseEntity<>(categories, HttpStatus.OK);
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<CategoryDTO> getCategoryById(@PathVariable Long userId,
                                                       @PathVariable Long categoryId) {
        logger.info("Getting category {} for user {}", categoryId, userId);

        Category category = categoryService.getCategoryById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));

        if (!category.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Category not found for this user");
        }

        CategoryDTO categoryDTO = categoryService.convertToDTO(category);
        return new ResponseEntity<>(categoryDTO, HttpStatus.OK);
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<CategoryDTO>> getCategoriesByType(@PathVariable Long userId,
                                                                 @PathVariable CategoryType type) {
        logger.info("Getting categories of type {} for user {}", type, userId);
        List<CategoryDTO> categories = categoryService.getCategoriesByUserAndType(userId, type);
        return new ResponseEntity<>(categories, HttpStatus.OK);
    }

    @GetMapping("/top-level")
    public ResponseEntity<List<CategoryDTO>> getTopLevelCategories(@PathVariable Long userId) {
        logger.info("Getting top-level categories for user {}", userId);
        List<CategoryDTO> categories = categoryService.getTopLevelCategoriesByUser(userId);
        return new ResponseEntity<>(categories, HttpStatus.OK);
    }

    @GetMapping("/{parentCategoryId}/subcategories")
    public ResponseEntity<List<CategoryDTO>> getSubcategories(@PathVariable Long userId,
                                                              @PathVariable Long parentCategoryId) {
        logger.info("Getting subcategories for parent category {} and user {}", parentCategoryId, userId);

        Category parentCategory = categoryService.getCategoryById(parentCategoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with id: " + parentCategoryId));

        if (!parentCategory.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Category not found for this user");
        }

        List<CategoryDTO> subcategories = categoryService.getSubcategories(parentCategoryId);
        logger.debug("Subcategories response: {}", subcategories);
        return new ResponseEntity<>(subcategories, HttpStatus.OK);
    }

    @PutMapping("/{categoryId}")
    public ResponseEntity<CategoryDTO> updateCategory(@PathVariable Long userId,
                                                      @PathVariable Long categoryId,
                                                      @Valid @RequestBody CategoryRequestDTO categoryDetails) {
        logger.info("Updating category {} for user {}", categoryId, userId);

        Category category = categoryService.getCategoryById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));

        if (!category.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Category not found for this user");
        }

        // Convert DTO to Entity for update
        Category updateEntity = new Category();
        updateEntity.setName(categoryDetails.getName());
        updateEntity.setDescription(categoryDetails.getDescription());
        updateEntity.setType(categoryDetails.getType());

        CategoryDTO updatedCategory = categoryService.updateCategory(categoryId, updateEntity);
        return new ResponseEntity<>(updatedCategory, HttpStatus.OK);
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<String> deleteCategory(@PathVariable Long userId,
                                                 @PathVariable Long categoryId) {
        logger.info("Deleting category {} for user {}", categoryId, userId);

        Category category = categoryService.getCategoryById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));

        if (!category.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Category not found for this user");
        }

        categoryService.deleteCategory(categoryId);
        return new ResponseEntity<>("Category deleted successfully", HttpStatus.OK);
    }

    @GetMapping("/search")
    public ResponseEntity<List<CategoryDTO>> searchCategoriesByName(@PathVariable Long userId,
                                                                    @RequestParam String name) {
        logger.info("Searching categories for user {} with name: {}", userId, name);

        if (name.length() < 2) {
            throw new IllegalArgumentException("Search term must be at least 2 characters long");
        }

        List<CategoryDTO> categories = categoryService.searchCategoriesByName(userId, name);
        return new ResponseEntity<>(categories, HttpStatus.OK);
    }
}