package com.kevin.financetracker.dto;

import com.kevin.financetracker.model.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CategoryRequestDTO {

    @NotBlank(message = "Category name is required")
    @Size(min = 1, max = 100, message = "Category name must be between 1 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9\\s\\-'.,&]+$", message = "Category name can only contain letters, numbers, spaces, and basic punctuation")
    private String name;

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;

    @NotNull(message = "Category type is required")
    private CategoryType type;

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public CategoryType getType() { return type; }
    public void setType(CategoryType type) { this.type = type; }
}