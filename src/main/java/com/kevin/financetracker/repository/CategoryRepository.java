package com.kevin.financetracker.repository;

import com.kevin.financetracker.model.Category;
import com.kevin.financetracker.model.CategoryType;
import com.kevin.financetracker.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Find all categories for a specific user
    //List<Category> findByUser(User user);
    @Query("SELECT c FROM Category c WHERE c.user.id = :userId")
    List<Category> findByUser(@Param("userId") Long userId);

    // Find categories by type for a specific user
    List<Category> findByUserAndType(User user, CategoryType type);

    // Find category by name and user
    Optional<Category> findByUserAndName(User user, String name);

    // Find subcategories of a parent category
    List<Category> findByParentCategory(Category parentCategory);

    // Find top-level categories (no parent)
    List<Category> findByUserAndParentCategoryIsNull(User user);

    // Add this method for search functionality
    @Query("SELECT c FROM Category c WHERE c.user.id = :userId AND (:name IS NULL OR UPPER(c.name) LIKE UPPER(CONCAT('%', :name, '%')))")
    List<Category> findByUserAndNameContainingIgnoreCase(@Param("userId") Long userId, @Param("name") String name);

    // Find categories by type and user
    List<Category> findByUserAndTypeAndParentCategoryIsNull(User user, CategoryType type);

    // Check if category name exists for a user
    boolean existsByUserAndName(User user, String name);

    // Custom query to find categories with transaction count
    @Query("SELECT c, COUNT(t) FROM Category c LEFT JOIN c.transactions t WHERE c.user = :user GROUP BY c")
    List<Object[]> findCategoriesWithTransactionCount(@Param("user") User user);

    @Query("SELECT c FROM Category c WHERE c.id = :categoryId AND c.user.id = :userId")
    Optional<Category> findByIdAndUserId(@Param("categoryId") Long categoryId, @Param("userId") Long userId);

    @Query("SELECT COUNT(c) > 0 FROM Category c WHERE c.id = :categoryId AND c.user.id = :userId")
    boolean existsByIdAndUserId(@Param("categoryId") Long categoryId, @Param("userId") Long userId);
}