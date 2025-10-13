package com.kevin.financetracker.repository;

import com.kevin.financetracker.model.Category;
import com.kevin.financetracker.model.CategoryType;
import com.kevin.financetracker.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CategoryRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CategoryRepository categoryRepository;

    private User user;
    private User anotherUser;
    private Category parentCategory;
    private Category childCategory;
    private Category incomeCategory;
    private Category anotherUserCategory;

    @BeforeEach
    void setUp() {
        // Create and persist test users with all required fields
        user = new User();
        user.setUsername("testuser");
        user.setEmail("testuser@gmail.com");
        user.setPassword("Password123!");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRole(User.Role.USER);
        user.setAccountNonExpired(true);
        user.setAccountNonLocked(true);
        user.setCredentialsNonExpired(true);
        user.setEnabled(true);
        user = entityManager.persistAndFlush(user);

        anotherUser = new User();
        anotherUser.setUsername("anotheruser");
        anotherUser.setEmail("anotheruser@gmail.com");
        anotherUser.setPassword("Password123!");
        anotherUser.setFirstName("Another");
        anotherUser.setLastName("User");
        anotherUser.setRole(User.Role.USER);
        anotherUser.setAccountNonExpired(true);
        anotherUser.setAccountNonLocked(true);
        anotherUser.setCredentialsNonExpired(true);
        anotherUser.setEnabled(true);
        anotherUser = entityManager.persistAndFlush(anotherUser);

        // Create and persist parent category
        parentCategory = new Category();
        parentCategory.setName("Parent Expense");
        parentCategory.setType(CategoryType.EXPENSE);
        parentCategory.setUser(user);
        parentCategory.setDescription("Parent category for expenses");
        parentCategory = entityManager.persistAndFlush(parentCategory);

        // Create and persist child category
        childCategory = new Category();
        childCategory.setName("Child Expense");
        childCategory.setType(CategoryType.EXPENSE);
        childCategory.setUser(user);
        childCategory.setParentCategory(parentCategory);
        childCategory.setDescription("Child category under parent");
        childCategory = entityManager.persistAndFlush(childCategory);

        // Create and persist income category
        incomeCategory = new Category();
        incomeCategory.setName("Salary");
        incomeCategory.setType(CategoryType.INCOME);
        incomeCategory.setUser(user);
        incomeCategory.setDescription("Salary income");
        incomeCategory = entityManager.persistAndFlush(incomeCategory);

        // Create and persist category for another user
        anotherUserCategory = new Category();
        anotherUserCategory.setName("Another User Category");
        anotherUserCategory.setType(CategoryType.EXPENSE);
        anotherUserCategory.setUser(anotherUser);
        anotherUserCategory = entityManager.persistAndFlush(anotherUserCategory);
    }

    @Test
    void findByUser_ShouldReturnCategoriesForSpecificUser() {
        // Act
        List<Category> userCategories = categoryRepository.findByUser(user.getId());

        // Assert
        assertNotNull(userCategories);
        assertEquals(3, userCategories.size());
        assertTrue(userCategories.stream().allMatch(category -> category.getUser().getId().equals(user.getId())));
    }

    @Test
    void findByUser_ShouldReturnEmptyListForUserWithNoCategories() {
        // Arrange
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setEmail("newuser@gmail.com");
        newUser.setPassword("Password123!");
        newUser.setFirstName("New");
        newUser.setLastName("User");
        newUser.setRole(User.Role.USER);
        newUser.setAccountNonExpired(true);
        newUser.setAccountNonLocked(true);
        newUser.setCredentialsNonExpired(true);
        newUser.setEnabled(true);
        newUser = entityManager.persistAndFlush(newUser);

        // Act
        List<Category> categories = categoryRepository.findByUser(newUser.getId());

        // Assert
        assertNotNull(categories);
        assertTrue(categories.isEmpty());
    }

    @Test
    void testUserPersistence() {
        User testUser = new User();
        testUser.setUsername("testuser1");
        testUser.setEmail("testuser1@gmail.com");
        testUser.setPassword("Password123!");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setRole(User.Role.USER);
        testUser.setEnabled(true);
        testUser.setAccountNonExpired(true);
        testUser.setAccountNonLocked(true);
        testUser.setCredentialsNonExpired(true);
        testUser = entityManager.persistAndFlush(testUser);
        assertNotNull(testUser.getId());

        User moderatorUser = new User();
        moderatorUser.setUsername("moderatoruser");
        moderatorUser.setEmail("moderatoruser@gmail.com");
        moderatorUser.setPassword("Password123!");
        moderatorUser.setFirstName("Moderator");
        moderatorUser.setLastName("User");
        moderatorUser.setRole(User.Role.MODERATOR);
        moderatorUser.setEnabled(true);
        moderatorUser.setAccountNonExpired(true);
        moderatorUser.setAccountNonLocked(true);
        moderatorUser.setCredentialsNonExpired(true);
        moderatorUser = entityManager.persistAndFlush(moderatorUser);
        assertNotNull(moderatorUser.getId());
    }

    @Test
    void findByUserAndType_ShouldReturnFilteredCategories() {
        List<Category> expenseCategories = categoryRepository.findByUserAndType(user, CategoryType.EXPENSE);
        List<Category> incomeCategories = categoryRepository.findByUserAndType(user, CategoryType.INCOME);
        assertEquals(2, expenseCategories.size());
        assertTrue(expenseCategories.stream().allMatch(category -> category.getType() == CategoryType.EXPENSE));
        assertEquals(1, incomeCategories.size());
        assertEquals("Salary", incomeCategories.get(0).getName());
    }

    @Test
    void findByUserAndName_ShouldReturnCategoryWhenExists() {
        Optional<Category> foundCategory = categoryRepository.findByUserAndName(user, "Parent Expense");
        assertTrue(foundCategory.isPresent());
        assertEquals("Parent Expense", foundCategory.get().getName());
        assertEquals(user.getId(), foundCategory.get().getUser().getId());
    }

    @Test
    void findByUserAndName_ShouldReturnEmptyWhenNotFound() {
        Optional<Category> foundCategory = categoryRepository.findByUserAndName(user, "Non-existent Category");
        assertFalse(foundCategory.isPresent());
    }

    @Test
    void findByUserAndName_ShouldNotFindOtherUsersCategories() {
        Optional<Category> foundCategory = categoryRepository.findByUserAndName(user, "Another User Category");
        assertFalse(foundCategory.isPresent());
    }

    @Test
    void findByParentCategory_ShouldReturnAllSubcategories() {
        List<Category> subcategories = categoryRepository.findByParentCategory(parentCategory);
        assertEquals(1, subcategories.size());
        assertEquals("Child Expense", subcategories.get(0).getName());
        assertEquals(parentCategory.getId(), subcategories.get(0).getParentCategory().getId());
    }

    @Test
    void findByParentCategory_ShouldReturnEmptyListForCategoryWithNoChildren() {
        List<Category> subcategories = categoryRepository.findByParentCategory(incomeCategory);
        assertNotNull(subcategories);
        assertTrue(subcategories.isEmpty());
    }

    @Test
    void findByUserAndParentCategoryIsNull_ShouldReturnTopLevelCategories() {
        List<Category> topLevelCategories = categoryRepository.findByUserAndParentCategoryIsNull(user);
        assertEquals(2, topLevelCategories.size());
        assertTrue(topLevelCategories.stream().allMatch(category -> category.getParentCategory() == null));
    }

    @Test
    void findByUserAndNameContainingIgnoreCase_ShouldReturnMatchingCategories() {
        List<Category> matchingCategories = categoryRepository.findByUserAndNameContainingIgnoreCase(user.getId(), "expense");
        assertEquals(2, matchingCategories.size());
        assertTrue(matchingCategories.stream()
                .allMatch(category -> category.getName().toLowerCase().contains("expense")));
    }

    @Test
    void findByUserAndNameContainingIgnoreCase_ShouldBeCaseInsensitive() {
        List<Category> upperCaseResults = categoryRepository.findByUserAndNameContainingIgnoreCase(user.getId(), "EXPENSE");
        List<Category> lowerCaseResults = categoryRepository.findByUserAndNameContainingIgnoreCase(user.getId(), "expense");
        assertEquals(2, upperCaseResults.size());
        assertEquals(2, lowerCaseResults.size());
    }

    @Test
    void findByUserAndTypeAndParentCategoryIsNull_ShouldReturnFilteredTopLevelCategories() {
        List<Category> topLevelExpenseCategories = categoryRepository.findByUserAndTypeAndParentCategoryIsNull(user, CategoryType.EXPENSE);
        List<Category> topLevelIncomeCategories = categoryRepository.findByUserAndTypeAndParentCategoryIsNull(user, CategoryType.INCOME);
        assertEquals(1, topLevelExpenseCategories.size());
        assertEquals("Parent Expense", topLevelExpenseCategories.get(0).getName());
        assertEquals(1, topLevelIncomeCategories.size());
        assertEquals("Salary", topLevelIncomeCategories.get(0).getName());
    }

    @Test
    void existsByUserAndName_ShouldReturnTrueWhenCategoryExists() {
        assertTrue(categoryRepository.existsByUserAndName(user, "Parent Expense"));
        assertTrue(categoryRepository.existsByUserAndName(user, "Salary"));
    }

    @Test
    void existsByUserAndName_ShouldReturnFalseWhenCategoryDoesNotExist() {
        assertFalse(categoryRepository.existsByUserAndName(user, "Non-existent Category"));
        assertFalse(categoryRepository.existsByUserAndName(anotherUser, "Parent Expense"));
    }

    @Test
    void findCategoriesWithTransactionCount_ShouldReturnCategoriesWithCounts() {
        List<Object[]> results = categoryRepository.findCategoriesWithTransactionCount(user);
        assertNotNull(results);
        assertEquals(3, results.size());
        results.forEach(result -> {
            Category c = (Category) result[0];
            Long count = (Long) result[1];
            assertEquals(0L, count);
            assertEquals(user.getId(), c.getUser().getId());
        });
    }

    @Test
    void save_ShouldPersistNewCategory() {
        Category newCategory = new Category();
        newCategory.setName("New Category");
        newCategory.setType(CategoryType.EXPENSE);
        newCategory.setUser(user);
        newCategory.setDescription("Test description");
        Category savedCategory = categoryRepository.save(newCategory);
        assertNotNull(savedCategory.getId());
        Category foundCategory = entityManager.find(Category.class, savedCategory.getId());
        assertEquals("New Category", foundCategory.getName());
        assertEquals(user.getId(), foundCategory.getUser().getId());
    }

    @Test
    void save_ShouldUpdateExistingCategory() {
        Category categoryToUpdate = categoryRepository.findByUserAndName(user, "Parent Expense").get();
        categoryToUpdate.setName("Updated Parent Expense");
        Category updatedCategory = categoryRepository.save(categoryToUpdate);
        Category foundCategory = categoryRepository.findById(updatedCategory.getId()).get();
        assertEquals("Updated Parent Expense", foundCategory.getName());
    }

    @Test
    void findById_ShouldReturnCategoryWhenExists() {
        Optional<Category> foundCategory = categoryRepository.findById(parentCategory.getId());
        assertTrue(foundCategory.isPresent());
        assertEquals(parentCategory.getId(), foundCategory.get().getId());
    }

    @Test
    void findById_ShouldReturnEmptyWhenNotExists() {
        Optional<Category> foundCategory = categoryRepository.findById(999L);
        assertFalse(foundCategory.isPresent());
    }

    @Test
    void findAll_ShouldReturnAllCategories() {
        List<Category> allCategories = categoryRepository.findAll();
        assertNotNull(allCategories);
        assertEquals(4, allCategories.size());
    }

    @Test
    void deleteById_ShouldRemoveCategory() {
        categoryRepository.deleteById(childCategory.getId());
        Optional<Category> deletedCategory = categoryRepository.findById(childCategory.getId());
        assertFalse(deletedCategory.isPresent());
    }

    @Test
    void delete_ShouldRemoveCategory() {
        categoryRepository.delete(incomeCategory);
        Optional<Category> deletedCategory = categoryRepository.findById(incomeCategory.getId());
        assertFalse(deletedCategory.isPresent());
    }

    @Test
    void count_ShouldReturnTotalNumberOfCategories() {
        long count = categoryRepository.count();
        assertEquals(4, count);
    }

    @Test
    void shouldAllowSameCategoryNameForDifferentUsers() {
        Category sameNameCategory = new Category();
        sameNameCategory.setName("Parent Expense");
        sameNameCategory.setType(CategoryType.EXPENSE);
        sameNameCategory.setUser(anotherUser);
        Category savedCategory = categoryRepository.save(sameNameCategory);
        assertNotNull(savedCategory.getId());
        assertTrue(categoryRepository.existsByUserAndName(user, "Parent Expense"));
        assertTrue(categoryRepository.existsByUserAndName(anotherUser, "Parent Expense"));
    }

    @Test
    void findByUserAndNameContainingIgnoreCase_ShouldHandleEmptySearchString() {
        List<Category> results = categoryRepository.findByUserAndNameContainingIgnoreCase(user.getId(), "");
        assertEquals(3, results.size());
    }

    @Test
    void findByUserAndNameContainingIgnoreCase_ShouldHandleNullSearchString() {
        List<Category> results = categoryRepository.findByUserAndNameContainingIgnoreCase(user.getId(), null);
        assertEquals(3, results.size());
    }

    @Test
    void shouldFindCategoriesWithPartialNameMatch() {
        Category testCategory = new Category();
        testCategory.setName("Food and Dining");
        testCategory.setType(CategoryType.EXPENSE);
        testCategory.setUser(user);
        categoryRepository.save(testCategory);
        List<Category> foodResults = categoryRepository.findByUserAndNameContainingIgnoreCase(user.getId(), "food");
        List<Category> diningResults = categoryRepository.findByUserAndNameContainingIgnoreCase(user.getId(), "dining");
        assertEquals(1, foodResults.size());
        assertEquals("Food and Dining", foodResults.get(0).getName());
        assertEquals(1, diningResults.size());
        assertEquals("Food and Dining", diningResults.get(0).getName());
    }

    @Test
    void shouldFindCategoriesByTypeForDifferentUsers() {
        List<Category> userExpenseCategories = categoryRepository.findByUserAndType(user, CategoryType.EXPENSE);
        List<Category> anotherUserExpenseCategories = categoryRepository.findByUserAndType(anotherUser, CategoryType.EXPENSE);
        assertEquals(2, userExpenseCategories.size());
        assertEquals(1, anotherUserExpenseCategories.size());
        assertEquals("Another User Category", anotherUserExpenseCategories.get(0).getName());
    }

    @Test
    void shouldNotFindSubcategoriesForTopLevelCategories() {
        List<Category> parentSubcategories = categoryRepository.findByParentCategory(parentCategory);
        List<Category> incomeSubcategories = categoryRepository.findByParentCategory(incomeCategory);
        assertEquals(1, parentSubcategories.size());
        assertTrue(incomeSubcategories.isEmpty());
    }

    @Test
    void shouldVerifyCategoryExistenceCorrectly() {
        assertTrue(categoryRepository.existsByUserAndName(user, "Parent Expense"));
        assertTrue(categoryRepository.existsByUserAndName(user, "Child Expense"));
        assertTrue(categoryRepository.existsByUserAndName(user, "Salary"));
        assertFalse(categoryRepository.existsByUserAndName(user, "Non-existent"));
        assertFalse(categoryRepository.existsByUserAndName(anotherUser, "Salary"));
    }
}