package com.financetracker.service;

import com.financetracker.dto.CategoryRequest;
import com.financetracker.dto.CategoryResponse;
import com.financetracker.entity.Category;
import com.financetracker.entity.User;
import com.financetracker.exception.EmailAlreadyExistsException;
import com.financetracker.exception.ResourceNotFoundException;
import com.financetracker.repository.CategoryRepository;
import com.financetracker.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.financetracker.repository.TransactionRepository;


import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    public CategoryService(CategoryRepository categoryRepository,
                           UserRepository userRepository,
                           TransactionRepository transactionRepository) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    // CREATE
    @Transactional
    public CategoryResponse create(CategoryRequest request, String username) {
        User user = getUserByUsername(username);

        if (categoryRepository.existsByUserAndName(user, request.getName())) {
            throw new RuntimeException("Category already exists: " + request.getName());
        }

        Category category = new Category(request.getName(), user);
        Category saved = categoryRepository.save(category);
        return toResponse(saved);
    }

    // READ all
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAll(String username) {
        User user = getUserByUsername(username);
        return categoryRepository.findByUserOrderByNameAsc(user)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // READ one
    @Transactional(readOnly = true)
    public CategoryResponse getById(Long id, String username) {
        User user = getUserByUsername(username);
        Category category = categoryRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found with id: " + id));
        return toResponse(category);
    }

    // UPDATE
    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request, String username) {
        User user = getUserByUsername(username);
        Category category = categoryRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found with id: " + id));

        // If name is changing, ensure no duplicate
        if (!category.getName().equals(request.getName())
                && categoryRepository.existsByUserAndName(user, request.getName())) {
            throw new RuntimeException("Category already exists: " + request.getName());
        }

        category.setName(request.getName());
        Category updated = categoryRepository.save(category);
        return toResponse(updated);
    }

    // DELETE
    @Transactional
    public void delete(Long id, String username) {
        User user = getUserByUsername(username);
        Category category = categoryRepository.findByIdAndUser(id, user)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Category not found with id: " + id));

        // Check if any transactions use this category
        // We'll add this check via a new repository method
        if (transactionRepository.existsByCategory(category)) {
            throw new RuntimeException(
                "Cannot delete category: it is used by existing transactions");
        }

        categoryRepository.delete(category);
    }

    // HELPERS
    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private CategoryResponse toResponse(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getCreatedAt());
    }
}