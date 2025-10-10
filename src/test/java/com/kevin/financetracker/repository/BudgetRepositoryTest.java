package com.kevin.financetracker.repository;

import com.kevin.financetracker.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class BudgetRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void persistBudget_ShouldWork() {
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@valid.com");
        user.setPassword("Password123!");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRole(User.Role.USER);
        user.setEnabled(true);
        user.setAccountNonExpired(true);
        user.setAccountNonLocked(true);
        user.setCredentialsNonExpired(true);
        entityManager.persistAndFlush(user);

        Category category = new Category();
        category.setName("Groceries");
        category.setType(CategoryType.EXPENSE);
        category.setUser(user);
        entityManager.persistAndFlush(category);

        Budget budget = new Budget();
        budget.setName("Monthly Grocery Budget");
        budget.setAmount(new BigDecimal("500.00"));
        budget.setStartDate(LocalDate.now());
        budget.setEndDate(LocalDate.now().plusDays(30));
        budget.setPeriod(BudgetPeriod.MONTHLY);
        budget.setCategory(category);
        budget.setUser(user);
        entityManager.persistAndFlush(budget);

        assertThat(entityManager.find(Budget.class, budget.getId())).isNotNull();
        assertThat(budgetRepository.findByUser(user)).hasSize(1);
    }
}