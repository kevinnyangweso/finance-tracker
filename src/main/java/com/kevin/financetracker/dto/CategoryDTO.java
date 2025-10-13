package com.kevin.financetracker.dto;

import com.kevin.financetracker.model.CategoryType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CategoryDTO {
    private Long id;
    private String name;
    private CategoryType type;
    private String description;
    private Long parentCategoryId;
    private List<CategoryDTO> subCategories;

    public CategoryDTO() {
        this.subCategories = new ArrayList<>();
    }

    public CategoryDTO(Long id, String name, CategoryType type, String description, Long parentCategoryId, List<CategoryDTO> subCategories) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.description = description;
        this.parentCategoryId = parentCategoryId;
        this.subCategories = subCategories != null ? subCategories : new ArrayList<>();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public CategoryType getType() { return type; }
    public void setType(CategoryType type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getParentCategoryId() { return parentCategoryId; }
    public void setParentCategoryId(Long parentCategoryId) { this.parentCategoryId = parentCategoryId; }

    public List<CategoryDTO> getSubCategories() { return subCategories; }
    public void setSubCategories(List<CategoryDTO> subCategories) { this.subCategories = subCategories; }

    // Helper methods
    public void addSubCategory(CategoryDTO subCategory) {
        if (subCategory != null) {
            this.subCategories.add(subCategory);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CategoryDTO that = (CategoryDTO) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                type == that.type &&
                Objects.equals(description, that.description) &&
                Objects.equals(parentCategoryId, that.parentCategoryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, type, description, parentCategoryId);
    }

    @Override
    public String toString() {
        return "CategoryDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", description='" + description + '\'' +
                ", parentCategoryId=" + parentCategoryId +
                ", subCategories=" + subCategories +
                '}';
    }
}