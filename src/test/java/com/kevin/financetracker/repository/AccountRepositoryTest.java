package com.kevin.financetracker.repository;

import com.kevin.financetracker.model.Account;
import com.kevin.financetracker.model.AccountType;
import com.kevin.financetracker.model.User;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class AccountRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private User testUser2;
    private Account checkingAccount;
    private Account savingsAccount;
    private Account creditCardAccount;
    private Account cashAccount;
    private Account investmentAccount;

    @BeforeEach
    void setUp() {
        // Clear existing data
        accountRepository.deleteAll();
        userRepository.deleteAll();

        // Create test users
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@valid.com");
        testUser.setPassword("password");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser = entityManager.persist(testUser);

        testUser2 = new User();
        testUser2.setUsername("testuser2");
        testUser2.setEmail("test2@valid.com");
        testUser2.setPassword("password");
        testUser2.setFirstName("SecondTest");
        testUser2.setLastName("SecondUser");
        testUser2 = entityManager.persist(testUser2);

        // Create test accounts for testUser
        checkingAccount = new Account();
        checkingAccount.setName("Main Checking");
        checkingAccount.setType(AccountType.CHECKING);
        checkingAccount.setBalance(new BigDecimal("1000.00"));
        checkingAccount.setCurrency("USD");
        checkingAccount.setDescription("Primary checking account");
        checkingAccount.setUser(testUser);
        checkingAccount = entityManager.persist(checkingAccount);

        savingsAccount = new Account();
        savingsAccount.setName("Emergency Savings");
        savingsAccount.setType(AccountType.SAVINGS);
        savingsAccount.setBalance(new BigDecimal("5000.00"));
        savingsAccount.setCurrency("USD");
        savingsAccount.setDescription("Emergency fund");
        savingsAccount.setUser(testUser);
        savingsAccount = entityManager.persist(savingsAccount);

        creditCardAccount = new Account();
        creditCardAccount.setName("Visa Credit Card");
        creditCardAccount.setType(AccountType.CREDIT_CARD);
        creditCardAccount.setBalance(new BigDecimal("-250.00")); // Valid for CREDIT_CARD
        creditCardAccount.setCurrency("USD");
        creditCardAccount.setDescription("Primary credit card");
        creditCardAccount.setUser(testUser);
        creditCardAccount = entityManager.persist(creditCardAccount);

        cashAccount = new Account();
        cashAccount.setName("Cash Wallet");
        cashAccount.setType(AccountType.CASH);
        cashAccount.setBalance(new BigDecimal("150.50"));
        cashAccount.setCurrency("USD");
        cashAccount.setDescription("Daily cash expenses");
        cashAccount.setUser(testUser);
        cashAccount = entityManager.persist(cashAccount);

        investmentAccount = new Account();
        investmentAccount.setName("Stock Portfolio");
        investmentAccount.setType(AccountType.INVESTMENT);
        investmentAccount.setBalance(new BigDecimal("12500.75"));
        investmentAccount.setCurrency("USD");
        investmentAccount.setDescription("Stock investments");
        investmentAccount.setUser(testUser);
        investmentAccount = entityManager.persist(investmentAccount);

        // Create accounts for the second user
        Account user2Checking = new Account();
        user2Checking.setName("User2 Checking");
        user2Checking.setType(AccountType.CHECKING);
        user2Checking.setBalance(new BigDecimal("2000.00"));
        user2Checking.setCurrency("USD");
        user2Checking.setDescription("Secondary checking account");
        user2Checking.setUser(testUser2);
        entityManager.persist(user2Checking);

        Account user2Savings = new Account();
        user2Savings.setName("User2 Savings");
        user2Savings.setType(AccountType.SAVINGS);
        user2Savings.setBalance(new BigDecimal("3500.00"));
        user2Savings.setCurrency("USD");
        user2Savings.setDescription("Secondary savings account");
        user2Savings.setUser(testUser2);
        entityManager.persist(user2Savings);

        // Flush and clear only after all entities are persisted
        entityManager.flush();
        entityManager.clear();
    }

    // BASIC CRUD OPERATIONS TESTS
    @Test
    @DisplayName("Save account - should persist account with all fields")
    void saveAccount_ShouldPersistAccountWithAllFields() {
        Account newAccount = new Account();
        newAccount.setName("New Investment Account");
        newAccount.setType(AccountType.INVESTMENT);
        newAccount.setBalance(new BigDecimal("10000.00"));
        newAccount.setCurrency("EUR");
        newAccount.setDescription("Long-term investments");
        newAccount.setUser(entityManager.merge(testUser)); // Reattach testUser

        Account savedAccount = accountRepository.save(newAccount);

        assertThat(savedAccount.getId()).isNotNull();
        assertThat(savedAccount.getName()).isEqualTo("New Investment Account");
        assertThat(savedAccount.getType()).isEqualTo(AccountType.INVESTMENT);
        assertThat(savedAccount.getBalance()).isEqualByComparingTo("10000.00");
        assertThat(savedAccount.getCurrency()).isEqualTo("EUR");
        assertThat(savedAccount.getDescription()).isEqualTo("Long-term investments");
        assertThat(savedAccount.getUser().getId()).isEqualTo(testUser.getId());
        assertThat(savedAccount.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Save account without required fields - should throw exception")
    void saveAccount_WithoutRequiredFields_ShouldThrowException() {
        Account account = new Account();
        account.setUser(entityManager.merge(testUser)); // Reattach testUser
        assertThatThrownBy(() -> accountRepository.saveAndFlush(account))
                .isInstanceOf(jakarta.validation.ConstraintViolationException.class)
                .hasMessageContaining("Account name is required")
                .hasMessageContaining("Account type is required")
                .hasMessageContaining("Balance is required");
    }

    @Test
    @DisplayName("Save account - with negative balance for CHECKING should throw exception")
    void saveAccount_WithNegativeBalanceForChecking_ShouldThrowException() {
        Account invalidAccount = new Account();
        invalidAccount.setName("Invalid Checking");
        invalidAccount.setType(AccountType.CHECKING);
        invalidAccount.setBalance(new BigDecimal("-100.00"));
        invalidAccount.setCurrency("USD");
        invalidAccount.setUser(entityManager.merge(testUser)); // Reattach testUser

        assertThatThrownBy(() -> accountRepository.saveAndFlush(invalidAccount))
                .isInstanceOf(jakarta.validation.ConstraintViolationException.class)
                .hasMessageContaining("Account balance must be appropriate for the account type");
    }

    @Test
    @DisplayName("Update account - should update account fields")
    void updateAccount_ShouldUpdateAccountFields() {
        Account account = accountRepository.findByUserAndName(testUser, "Main Checking").get();
        account.setName("Updated Checking Account");
        account.setBalance(new BigDecimal("1500.00"));
        account.setDescription("Updated description");

        Account updatedAccount = accountRepository.save(account);

        assertThat(updatedAccount.getName()).isEqualTo("Updated Checking Account");
        assertThat(updatedAccount.getBalance()).isEqualByComparingTo("1500.00");
        assertThat(updatedAccount.getDescription()).isEqualTo("Updated description");
    }

    @Test
    @DisplayName("Delete account - should remove account from database")
    void deleteAccount_ShouldRemoveAccountFromDatabase() {
        Long accountId = checkingAccount.getId();

        accountRepository.delete(checkingAccount);
        entityManager.flush();
        entityManager.clear();

        Optional<Account> deletedAccount = accountRepository.findById(accountId);
        assertThat(deletedAccount).isEmpty();
    }

    @Test
    @DisplayName("Find all - should return all accounts across all users")
    void findAll_ShouldReturnAllAccounts() {
        List<Account> allAccounts = accountRepository.findAll();

        assertThat(allAccounts).hasSize(7);
    }

    @Test
    @DisplayName("Find by ID - should return account when exists")
    void findById_WhenAccountExists_ShouldReturnAccount() {
        Optional<Account> foundAccount = accountRepository.findById(checkingAccount.getId());

        assertThat(foundAccount).isPresent();
        assertThat(foundAccount.get().getName()).isEqualTo("Main Checking");
        assertThat(foundAccount.get().getType()).isEqualTo(AccountType.CHECKING);
        assertThat(foundAccount.get().getBalance()).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("Find by ID - with non-existent ID should return empty")
    void findById_WithNonExistentId_ShouldReturnEmpty() {
        Optional<Account> foundAccount = accountRepository.findById(999L);

        assertThat(foundAccount).isEmpty();
    }

    // CUSTOM QUERY METHOD TESTS
    @Test
    @DisplayName("Find by user - should return all accounts for specific user")
    void findByUser_ShouldReturnAccountsForUser() {
        List<Account> userAccounts = accountRepository.findByUser(testUser);

        assertThat(userAccounts).hasSize(5);
        assertThat(userAccounts)
                .extracting(Account::getName)
                .containsExactlyInAnyOrder(
                        "Main Checking",
                        "Emergency Savings",
                        "Visa Credit Card",
                        "Cash Wallet",
                        "Stock Portfolio"
                );
    }

    @Test
    @DisplayName("Find by user ID - should return all accounts for user ID")
    void findByUserId_ShouldReturnAccountsForUserId() {
        List<Account> userAccounts = accountRepository.findByUserId(testUser.getId());

        assertThat(userAccounts).hasSize(5);
        assertThat(userAccounts)
                .extracting(Account::getUser)
                .allMatch(user -> user.getId().equals(testUser.getId()));
    }

    @Test
    @DisplayName("Find by user ID - with invalid user ID should return empty list")
    void findByUserId_WithInvalidUserId_ShouldReturnEmptyList() {
        List<Account> userAccounts = accountRepository.findByUserId(999L);

        assertThat(userAccounts).isEmpty();
    }

    @Test
    @DisplayName("Find by user and type - should return accounts of specific type for user")
    void findByUserAndType_ShouldReturnAccountsOfTypeForUser() {
        List<Account> checkingAccounts = accountRepository.findByUserAndType(testUser, AccountType.CHECKING);

        assertThat(checkingAccounts).hasSize(1);
        assertThat(checkingAccounts.get(0).getType()).isEqualTo(AccountType.CHECKING);
        assertThat(checkingAccounts.get(0).getName()).isEqualTo("Main Checking");
    }

    @Test
    @DisplayName("Find by user and type - with multiple accounts of same type")
    void findByUserAndType_WithMultipleAccountsOfSameType_ShouldReturnAll() {
        Account anotherSavings = new Account();
        anotherSavings.setName("Vacation Savings");
        anotherSavings.setType(AccountType.SAVINGS);
        anotherSavings.setBalance(new BigDecimal("2000.00"));
        anotherSavings.setCurrency("USD");
        anotherSavings.setUser(entityManager.merge(testUser)); // Reattach testUser
        entityManager.persistAndFlush(anotherSavings);

        List<Account> savingsAccounts = accountRepository.findByUserAndType(testUser, AccountType.SAVINGS);

        assertThat(savingsAccounts).hasSize(2);
        assertThat(savingsAccounts)
                .extracting(Account::getType)
                .containsOnly(AccountType.SAVINGS);
    }

    @Test
    @DisplayName("Find by user and type - with non-existent type should return empty list")
    void findByUserAndType_WithNonExistentType_ShouldReturnEmptyList() {
        List<Account> loanAccounts = accountRepository.findByUserAndType(testUser, AccountType.LOAN);

        assertThat(loanAccounts).isEmpty();
    }

    @Test
    @DisplayName("Find by user and name - should return account with exact name match")
    void findByUserAndName_ShouldReturnAccountWithExactName() {
        Optional<Account> foundAccount = accountRepository.findByUserAndName(testUser, "Main Checking");

        assertThat(foundAccount).isPresent();
        assertThat(foundAccount.get().getName()).isEqualTo("Main Checking");
        assertThat(foundAccount.get().getUser().getId()).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("Find by user and name - with non-existent name should return empty")
    void findByUserAndName_WithNonExistentName_ShouldReturnEmpty() {
        Optional<Account> foundAccount = accountRepository.findByUserAndName(testUser, "Non Existent Account");

        assertThat(foundAccount).isEmpty();
    }

    @Test
    @DisplayName("Find by user and name - with different user should return empty")
    void findByUserAndName_WithDifferentUser_ShouldReturnEmpty() {
        Optional<Account> foundAccount = accountRepository.findByUserAndName(testUser2, "Main Checking");

        assertThat(foundAccount).isEmpty();
    }

    @Test
    @DisplayName("Find by user and balance greater than - should return accounts with balance above threshold")
    void findByUserAndBalanceGreaterThan_ShouldReturnAccountsWithBalanceAboveThreshold() {
        List<Account> accounts = accountRepository.findByUserAndBalanceGreaterThan(testUser, new BigDecimal("1000.00"));

        assertThat(accounts).hasSize(2);
        assertThat(accounts)
                .extracting(Account::getName)
                .containsExactlyInAnyOrder("Emergency Savings", "Stock Portfolio");
    }

    @Test
    @DisplayName("Find by user and balance less than - should return accounts with balance below threshold")
    void findByUserAndBalanceLessThan_ShouldReturnAccountsWithBalanceBelowThreshold() {
        List<Account> accounts = accountRepository.findByUserAndBalanceLessThan(testUser, new BigDecimal("0.00"));

        assertThat(accounts).hasSize(1);
        assertThat(accounts.get(0).getName()).isEqualTo("Visa Credit Card");
        assertThat(accounts.get(0).getBalance()).isEqualTo(new BigDecimal("-250.00"));
    }

    @Test
    @DisplayName("Find by user and balance less than - with positive threshold")
    void findByUserAndBalanceLessThan_WithPositiveThreshold_ShouldReturnAccountsBelowThreshold() {
        List<Account> accounts = accountRepository.findByUserAndBalanceLessThan(testUser, new BigDecimal("1000.00"));

        assertThat(accounts).hasSize(2);
        assertThat(accounts)
                .extracting(Account::getName)
                .containsExactlyInAnyOrder("Visa Credit Card", "Cash Wallet");
    }

    @Test
    @DisplayName("Get total balance by user - should return correct sum of balances")
    void getTotalBalanceByUser_ShouldReturnCorrectSum() {
        Optional<BigDecimal> totalBalance = accountRepository.getTotalBalanceByUser(testUser);

        assertThat(totalBalance).isPresent();
        assertThat(totalBalance.get()).isEqualByComparingTo("18401.25");
    }

    @Test
    @DisplayName("Get total balance by user - with user having no accounts should return empty")
    void getTotalBalanceByUser_WithUserHavingNoAccounts_ShouldReturnEmpty() {
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setEmail("new@valid.com");
        newUser.setPassword("password");
        newUser.setFirstName("New");
        newUser.setLastName("User");
        newUser = entityManager.persistAndFlush(newUser);

        Optional<BigDecimal> totalBalance = accountRepository.getTotalBalanceByUser(newUser);

        assertThat(totalBalance).isEmpty();
    }

    @Test
    @DisplayName("Exists by user and name - should return true when account exists")
    void existsByUserAndName_WhenAccountExists_ShouldReturnTrue() {
        boolean exists = accountRepository.existsByUserAndName(testUser, "Main Checking");

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Exists by user and name - should return false when account does not exist")
    void existsByUserAndName_WhenAccountDoesNotExist_ShouldReturnFalse() {
        boolean exists = accountRepository.existsByUserAndName(testUser, "Non Existent Account");

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Exists by user and name - with different user should return false")
    void existsByUserAndName_WithDifferentUser_ShouldReturnFalse() {
        boolean exists = accountRepository.existsByUserAndName(testUser2, "Main Checking");

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Find by type - should return all accounts of specific type across all users")
    void findByType_ShouldReturnAllAccountsOfType() {
        List<Account> checkingAccounts = accountRepository.findByType(AccountType.CHECKING);

        assertThat(checkingAccounts).hasSize(2);
        assertThat(checkingAccounts)
                .extracting(Account::getType)
                .containsOnly(AccountType.CHECKING);
    }

    // VALIDATION AND CONSTRAINT TESTS
    @Test
    @DisplayName("Account creation - should set created timestamp automatically")
    void accountCreation_ShouldSetCreatedTimestampAutomatically() {
        Account newAccount = new Account();
        newAccount.setName("Timestamp Test Account");
        newAccount.setType(AccountType.CASH);
        newAccount.setBalance(BigDecimal.ZERO);
        newAccount.setCurrency("USD");
        newAccount.setUser(entityManager.merge(testUser)); // Reattach testUser

        Account savedAccount = accountRepository.save(newAccount);

        assertThat(savedAccount.getCreatedAt()).isNotNull();
        assertThat(savedAccount.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("Account uniqueness - should prevent duplicate account names for same user")
    void accountUniqueness_ShouldPreventDuplicateAccountNamesForSameUser() {
        Account duplicateAccount = new Account();
        duplicateAccount.setName("Main Checking");
        duplicateAccount.setType(AccountType.CHECKING);
        duplicateAccount.setBalance(new BigDecimal("500.00"));
        duplicateAccount.setCurrency("USD");
        duplicateAccount.setUser(entityManager.merge(testUser)); // Reattach testUser

        assertThatThrownBy(() -> accountRepository.saveAndFlush(duplicateAccount))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Account uniqueness - should allow same account name for different users")
    void accountUniqueness_ShouldAllowSameAccountNameForDifferentUsers() {
        Account sameNameAccount = new Account();
        sameNameAccount.setName("Main Checking");
        sameNameAccount.setType(AccountType.CHECKING);
        sameNameAccount.setBalance(new BigDecimal("500.00"));
        sameNameAccount.setCurrency("USD");
        sameNameAccount.setUser(entityManager.merge(testUser2)); // Reattach testUser2

        Account savedAccount = accountRepository.save(sameNameAccount);

        assertThat(savedAccount.getId()).isNotNull();
        assertThat(savedAccount.getName()).isEqualTo("Main Checking");
        assertThat(savedAccount.getUser().getId()).isEqualTo(testUser2.getId());
    }

    @Test
    @DisplayName("Currency validation - should store various currency codes")
    void currencyValidation_ShouldStoreVariousCurrencyCodes() {
        Account accountWithEUR = new Account();
        accountWithEUR.setName("EUR Account");
        accountWithEUR.setType(AccountType.CHECKING);
        accountWithEUR.setBalance(BigDecimal.ZERO);
        accountWithEUR.setCurrency("EUR");
        accountWithEUR.setUser(entityManager.merge(testUser)); // Reattach testUser

        Account accountWithGBP = new Account();
        accountWithGBP.setName("GBP Account");
        accountWithGBP.setType(AccountType.SAVINGS);
        accountWithGBP.setBalance(BigDecimal.ZERO);
        accountWithGBP.setCurrency("GBP");
        accountWithGBP.setUser(entityManager.merge(testUser)); // Reattach testUser

        Account savedEURAccount = accountRepository.save(accountWithEUR);
        Account savedGBPAccount = accountRepository.save(accountWithGBP);

        assertThat(savedEURAccount.getCurrency()).isEqualTo("EUR");
        assertThat(savedGBPAccount.getCurrency()).isEqualTo("GBP");
    }

    @Test
    @DisplayName("Balance precision - should handle large balance values correctly")
    void balancePrecision_ShouldHandleLargeBalanceValuesCorrectly() {
        Account largeBalanceAccount = new Account();
        largeBalanceAccount.setName("Large Balance Account");
        largeBalanceAccount.setType(AccountType.INVESTMENT);
        largeBalanceAccount.setBalance(new BigDecimal("1234567890123.45"));
        largeBalanceAccount.setCurrency("USD");
        largeBalanceAccount.setUser(entityManager.merge(testUser)); // Reattach testUser

        Account savedAccount = accountRepository.save(largeBalanceAccount);

        assertThat(savedAccount.getBalance()).isEqualByComparingTo("1234567890123.45");

        Account retrievedAccount = accountRepository.findById(savedAccount.getId()).get();
        assertThat(retrievedAccount.getBalance()).isEqualByComparingTo("1234567890123.45");
    }

    @Test
    @DisplayName("Balance precision - should handle very small balance values correctly")
    void balancePrecision_ShouldHandleVerySmallBalanceValuesCorrectly() {
        Account smallBalanceAccount = new Account();
        smallBalanceAccount.setName("Small Balance Account");
        smallBalanceAccount.setType(AccountType.CASH);
        smallBalanceAccount.setBalance(new BigDecimal("0.01"));
        smallBalanceAccount.setCurrency("USD");
        smallBalanceAccount.setUser(entityManager.merge(testUser)); // Reattach testUser

        Account savedAccount = accountRepository.save(smallBalanceAccount);

        assertThat(savedAccount.getBalance()).isEqualByComparingTo("0.01");
    }

    // EDGE CASE AND PERFORMANCE TESTS
    @Test
    @DisplayName("Find accounts - with empty database should return empty lists")
    void findAccounts_WithEmptyDatabase_ShouldReturnEmptyLists() {
        userRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();

        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setEmail("new@valid.com");
        newUser.setPassword("password");
        newUser.setFirstName("New");
        newUser.setLastName("User");
        newUser = entityManager.persistAndFlush(newUser);

        List<Account> allAccounts = accountRepository.findAll();
        List<Account> userAccounts = accountRepository.findByUser(newUser);
        Optional<BigDecimal> totalBalance = accountRepository.getTotalBalanceByUser(newUser);

        assertThat(allAccounts).isEmpty();
        assertThat(userAccounts).isEmpty();
        assertThat(totalBalance).isEmpty();
    }

    @Test
    @DisplayName("Account relationships - should maintain user relationship correctly")
    void accountRelationships_ShouldMaintainUserRelationshipCorrectly() {
        Account account = accountRepository.findByUserAndName(testUser, "Main Checking").get();
        User user = account.getUser();

        assertThat(user).isNotNull();
        assertThat(user.getId()).isEqualTo(testUser.getId());
        assertThat(user.getUsername()).isEqualTo("testuser");
        assertThat(user.getEmail()).isEqualTo("test@valid.com");
        assertThat(user.getFirstName()).isEqualTo("Test");
        assertThat(user.getLastName()).isEqualTo("User");
    }

    @Test
    @DisplayName("Multiple account types - should handle all account types correctly")
    void multipleAccountTypes_ShouldHandleAllAccountTypesCorrectly() {
        List<Account> checkingAccounts = accountRepository.findByType(AccountType.CHECKING);
        List<Account> savingsAccounts = accountRepository.findByType(AccountType.SAVINGS);
        List<Account> creditCardAccounts = accountRepository.findByType(AccountType.CREDIT_CARD);
        List<Account> cashAccounts = accountRepository.findByType(AccountType.CASH);
        List<Account> investmentAccounts = accountRepository.findByType(AccountType.INVESTMENT);
        List<Account> loanAccounts = accountRepository.findByType(AccountType.LOAN);

        assertThat(checkingAccounts).hasSize(2);
        assertThat(savingsAccounts).hasSize(2);
        assertThat(creditCardAccounts).hasSize(1);
        assertThat(cashAccounts).hasSize(1);
        assertThat(investmentAccounts).hasSize(1);
        assertThat(loanAccounts).isEmpty();
    }

    @Test
    @DisplayName("Account description - should handle long descriptions correctly")
    void accountDescription_ShouldHandleLongDescriptionsCorrectly() {
        String longDescription = "This is a very long description for testing purposes. " +
                "It should be able to handle up to 255 characters without any issues. " +
                "Let's make sure this works properly for account descriptions in our financial tracker application.";

        Account accountWithLongDesc = new Account();
        accountWithLongDesc.setName("Long Desc Account");
        accountWithLongDesc.setType(AccountType.SAVINGS);
        accountWithLongDesc.setBalance(BigDecimal.ZERO);
        accountWithLongDesc.setCurrency("USD");
        accountWithLongDesc.setDescription(longDescription);
        accountWithLongDesc.setUser(entityManager.merge(testUser)); // Reattach testUser

        Account savedAccount = accountRepository.save(accountWithLongDesc);

        assertThat(savedAccount.getDescription()).isEqualTo(longDescription);
        assertThat(savedAccount.getDescription().length()).isLessThanOrEqualTo(255);
    }

    @Test
    @DisplayName("Account balance - should handle zero balance correctly")
    void accountBalance_ShouldHandleZeroBalanceCorrectly() {
        Account zeroBalanceAccount = new Account();
        zeroBalanceAccount.setName("Zero Balance Account");
        zeroBalanceAccount.setType(AccountType.CHECKING);
        zeroBalanceAccount.setBalance(BigDecimal.ZERO);
        zeroBalanceAccount.setCurrency("USD");
        zeroBalanceAccount.setUser(entityManager.merge(testUser)); // Reattach testUser

        Account savedAccount = accountRepository.save(zeroBalanceAccount);

        assertThat(savedAccount.getBalance()).isEqualByComparingTo("0.00");

        List<Account> zeroBalanceAccounts = accountRepository.findByUserAndBalanceLessThan(testUser, new BigDecimal("0.01"));
        assertThat(zeroBalanceAccounts).extracting(Account::getName).contains("Zero Balance Account");
    }

    @Test
    @DisplayName("Account ordering - should return accounts in consistent order")
    void accountOrdering_ShouldReturnAccountsInConsistentOrder() {
        List<Account> userAccounts = accountRepository.findByUser(testUser);
        List<Account> allAccounts = accountRepository.findAll();

        assertThat(userAccounts).hasSize(5);
        assertThat(allAccounts).hasSize(7);

        List<Account> userAccountsSecondCall = accountRepository.findByUser(testUser);
        assertThat(userAccounts).hasSameSizeAs(userAccountsSecondCall);
    }

    @Test
    @DisplayName("Verify accounts table existence")
    void verifyAccountsTableExistence() {
        List<?> tables = entityManager.getEntityManager()
                .createNativeQuery("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'ACCOUNTS'")
                .getResultList();
        assertThat(tables).isNotEmpty();
    }
}