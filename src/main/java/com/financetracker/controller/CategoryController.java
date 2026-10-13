package com.financetracker.controller;

import com.financetracker.dto.CategoryRequest;
import com.financetracker.dto.CategoryResponse;
import com.financetracker.dto.MessageResponse;
import com.financetracker.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> create(
            @Valid @RequestBody CategoryRequest request,
            Authentication authentication) {
        return new ResponseEntity<>(
                categoryService.create(request, authentication.getName()),
                HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAll(Authentication authentication) {
        return ResponseEntity.ok(categoryService.getAll(authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getById(
            @PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(categoryService.getById(id, authentication.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(categoryService.update(id, request, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> delete(
            @PathVariable Long id, Authentication authentication) {
        categoryService.delete(id, authentication.getName());
        return ResponseEntity.ok(new MessageResponse("Category deleted successfully"));
    }
}