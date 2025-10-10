package com.kevin.financetracker.repository;

import com.kevin.financetracker.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "logging.level.org.springframework=DEBUG",
        "logging.level.org.hibernate=DEBUG",
        "logging.level.org.hibernate.SQL=DEBUG",
        "logging.level.org.hibernate.orm.jdbc.bind=TRACE"
})
class TransactionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TransactionRepository transactionRepository;

    private User testUser;
    private Account checkingAccount;
    private Account savingsAccount;
    private Category incomeCategory;
    private Category expenseCategory;
    private Transaction incomeTransaction;
    private Transaction expenseTransaction;
    private Transaction transferTransaction;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        try {
            now = LocalDateTime.now().withNano(0);

            // Create and persist User
            testUser = new User();
            testUser.setUsername("testuser123");
            testUser.setEmail("test@valid.com");
            testUser.setPassword("SecurePass123!");
            testUser.setFirstName("John");
            testUser.setLastName("Doe");
            testUser.setRole(User.Role.USER);
            testUser.setEnabled(true);
            testUser.setAccountNonExpired(true);
            testUser.setAccountNonLocked(true);
            testUser.setCredentialsNonExpired(true);
            entityManager.persist(testUser);

            // Create and persist Accounts
            checkingAccount = new Account();
            checkingAccount.setName("Primary Checking Account");
            checkingAccount.setType(AccountType.CHECKING);
            checkingAccount.setBalance(BigDecimal.valueOf(5000.00));
            checkingAccount.setCurrency("USD");
            checkingAccount.setDescription("Main checking account for daily expenses");
            checkingAccount.setUser(testUser);
            entityManager.persist(checkingAccount);

            savingsAccount = new Account();
            savingsAccount.setName("High-Yield Savings");
            savingsAccount.setType(AccountType.SAVINGS);
            savingsAccount.setBalance(BigDecimal.valueOf(10000.00));
            savingsAccount.setCurrency("USD");
            savingsAccount.setDescription("Long-term savings account");
            savingsAccount.setUser(testUser);
            entityManager.persist(savingsAccount);

            // Create and persist Categories
            incomeCategory = new Category();
            incomeCategory.setName("Salary");
            incomeCategory.setType(CategoryType.INCOME);
            incomeCategory.setUser(testUser);
            entityManager.persist(incomeCategory);

            expenseCategory = new Category();
            expenseCategory.setName("Groceries");
            expenseCategory.setType(CategoryType.EXPENSE);
            expenseCategory.setUser(testUser);
            entityManager.persist(expenseCategory);

            // Create and persist Transactions
            incomeTransaction = new Transaction();
            incomeTransaction.setAmount(BigDecimal.valueOf(2500.00));
            incomeTransaction.setDescription("Monthly Salary Deposit");
            incomeTransaction.setType(TransactionType.INCOME);
            incomeTransaction.setTransactionDate(now.minusDays(5));
            incomeTransaction.setUser(testUser);
            incomeTransaction.setAccount(checkingAccount);
            incomeTransaction.setCategory(incomeCategory);
            incomeTransaction.setNotes("Regular monthly salary from employer");
            incomeTransaction.setLocation("Company Bank");
            entityManager.persist(incomeTransaction);

            expenseTransaction = new Transaction();
            expenseTransaction.setAmount(BigDecimal.valueOf(150.75));
            expenseTransaction.setDescription("Weekly Groceries Purchase");
            expenseTransaction.setType(TransactionType.EXPENSE);
            expenseTransaction.setTransactionDate(now.minusDays(2));
            expenseTransaction.setUser(testUser);
            expenseTransaction.setAccount(checkingAccount);
            expenseTransaction.setCategory(expenseCategory);
            expenseTransaction.setNotes("Grocery shopping at local supermarket");
            expenseTransaction.setLocation("Local Supermarket");
            entityManager.persist(expenseTransaction);

            transferTransaction = new Transaction();
            transferTransaction.setAmount(BigDecimal.valueOf(500.00));
            transferTransaction.setDescription("Monthly Savings Transfer");
            transferTransaction.setType(TransactionType.TRANSFER);
            transferTransaction.setTransactionDate(now.minusDays(1));
            transferTransaction.setUser(testUser);
            transferTransaction.setAccount(checkingAccount);
            transferTransaction.setTransferToAccount(savingsAccount);
            transferTransaction.setNotes("Automatic monthly savings contribution");
            entityManager.persist(transferTransaction);

            entityManager.flush();
        } catch (Exception e) {
            System.err.println("Error in setUp: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    // Keep all test methods unchanged
    @Test
    void contextLoads() {
        assertThat(transactionRepository).isNotNull();
    }

    @Test
    void findByUserAndDescriptionContainingIgnoreCase_ShouldReturnMatchingTransactions() {
        List<Transaction> allTransactions = transactionRepository.findByUser(testUser);
        System.out.println("All transactions: " + allTransactions);
        System.out.println("Searching for description containing: SALARY");

        List<Transaction> salaryTransactions = transactionRepository.findByUserAndDescriptionContainingIgnoreCase(testUser, "SALARY");
        System.out.println("Salary transactions: " + salaryTransactions);

        System.out.println("Searching for description containing: Groceries");
        List<Transaction> groceryTransactions = transactionRepository.findByUserAndDescriptionContainingIgnoreCase(testUser, "Groceries");
        System.out.println("Grocery transactions: " + groceryTransactions);

        assertThat(salaryTransactions).hasSize(1);
        assertThat(salaryTransactions.get(0).getDescription()).isEqualTo("Monthly Salary Deposit");

        assertThat(groceryTransactions).hasSize(1);
        assertThat(groceryTransactions.get(0).getDescription()).isEqualTo("Weekly Groceries Purchase");
    }

    @Test
    void findByUser_ShouldReturnAllTransactionsForUser() {
        List<Transaction> transactions = transactionRepository.findByUser(testUser);
        assertThat(transactions).hasSize(3);
        assertThat(transactions).extracting(Transaction::getUser).containsOnly(testUser);
        assertThat(transactions).extracting(Transaction::getType)
                .containsExactlyInAnyOrder(TransactionType.INCOME, TransactionType.EXPENSE, TransactionType.TRANSFER);
    }

    @Test
    void findByUserAndType_ShouldReturnFilteredTransactions() {
        List<Transaction> incomeTransactions = transactionRepository.findByUserAndType(testUser, TransactionType.INCOME);
        List<Transaction> expenseTransactions = transactionRepository.findByUserAndType(testUser, TransactionType.EXPENSE);
        List<Transaction> transferTransactions = transactionRepository.findByUserAndType(testUser, TransactionType.TRANSFER);

        assertThat(incomeTransactions).hasSize(1);
        assertThat(incomeTransactions.get(0).getType()).isEqualTo(TransactionType.INCOME);
        assertThat(incomeTransactions.get(0).getAccount().getType()).isEqualTo(AccountType.CHECKING);

        assertThat(expenseTransactions).hasSize(1);
        assertThat(expenseTransactions.get(0).getType()).isEqualTo(TransactionType.EXPENSE);

        assertThat(transferTransactions).hasSize(1);
        assertThat(transferTransactions.get(0).getType()).isEqualTo(TransactionType.TRANSFER);
    }

    @Test
    void findByAccount_ShouldReturnTransactionsForAccount() {
        List<Transaction> checkingAccountTransactions = transactionRepository.findByAccount(checkingAccount);
        List<Transaction> savingsAccountTransactions = transactionRepository.findByAccount(savingsAccount);

        assertThat(checkingAccountTransactions).hasSize(3);
        assertThat(checkingAccountTransactions).extracting(Transaction::getAccount).containsOnly(checkingAccount);

        assertThat(savingsAccountTransactions).isEmpty();
    }

    @Test
    void findByCategory_ShouldReturnTransactionsForCategory() {
        List<Transaction> incomeCategoryTransactions = transactionRepository.findByCategory(incomeCategory);
        List<Transaction> expenseCategoryTransactions = transactionRepository.findByCategory(expenseCategory);

        assertThat(incomeCategoryTransactions).hasSize(1);
        assertThat(incomeCategoryTransactions.get(0).getCategory().getName()).isEqualTo("Salary");

        assertThat(expenseCategoryTransactions).hasSize(1);
        assertThat(expenseCategoryTransactions.get(0).getCategory().getName()).isEqualTo("Groceries");
    }

    @Test
    void findByUserAndTransactionDateBetween_ShouldReturnTransactionsInDateRange() {
        LocalDateTime start = now.minusDays(7);
        LocalDateTime end = now;

        List<Transaction> transactions = transactionRepository.findByUserAndTransactionDateBetween(testUser, start, end);

        assertThat(transactions).hasSize(3);
        assertThat(transactions).allMatch(t ->
                !t.getTransactionDate().isBefore(start) && !t.getTransactionDate().isAfter(end));
    }

    @Test
    void findByUserAndAmountGreaterThan_ShouldReturnTransactionsAboveAmount() {
        List<Transaction> transactions = transactionRepository.findByUserAndAmountGreaterThan(testUser, BigDecimal.valueOf(1000.00));

        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getAmount()).isEqualByComparingTo(BigDecimal.valueOf(2500.00));
        assertThat(transactions.get(0).getDescription()).isEqualTo("Monthly Salary Deposit");
    }

    @Test
    void findByUserAndAmountLessThan_ShouldReturnTransactionsBelowAmount() {
        List<Transaction> transactions = transactionRepository.findByUserAndAmountLessThan(testUser, BigDecimal.valueOf(200.00));

        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getAmount()).isEqualByComparingTo(BigDecimal.valueOf(150.75));
        assertThat(transactions.get(0).getDescription()).isEqualTo("Weekly Groceries Purchase");
    }

    @Test
    void verifySetup_ShouldPersistTransactions() {
        List<Transaction> transactions = transactionRepository.findByUser(testUser);
        System.out.println("All transactions in verifySetup: " + transactions);
        assertThat(transactions).hasSize(3);
        assertThat(transactions).extracting(Transaction::getDescription)
                .containsExactlyInAnyOrder(
                        "Monthly Salary Deposit",
                        "Weekly Groceries Purchase",
                        "Monthly Savings Transfer"
                );
    }

    @Test
    void getTotalIncomeByUserAndDateRange_ShouldReturnCorrectSum() {
        LocalDateTime start = now.minusDays(10);
        LocalDateTime end = now.plusDays(1);

        Optional<BigDecimal> totalIncome = transactionRepository.getTotalIncomeByUserAndDateRange(testUser, start, end);

        assertThat(totalIncome).isPresent();
        assertThat(totalIncome.get()).isEqualByComparingTo(BigDecimal.valueOf(2500.00));
    }

    @Test
    void getTotalExpensesByUserAndDateRange_ShouldReturnCorrectSum() {
        LocalDateTime start = now.minusDays(10);
        LocalDateTime end = now.plusDays(1);

        Optional<BigDecimal> totalExpenses = transactionRepository.getTotalExpensesByUserAndDateRange(testUser, start, end);

        assertThat(totalExpenses).isPresent();
        assertThat(totalExpenses.get()).isEqualByComparingTo(BigDecimal.valueOf(150.75));
    }

    @Test
    void getTotalIncomeByUserAndDateRange_ShouldReturnEmptyWhenNoMatches() {
        LocalDateTime start = now.plusDays(10);
        LocalDateTime end = now.plusDays(20);

        Optional<BigDecimal> totalIncome = transactionRepository.getTotalIncomeByUserAndDateRange(testUser, start, end);

        assertThat(totalIncome).isEmpty();
    }

    @Test
    void findRecentTransactionsByUser_ShouldReturnLimitedResultsInDescendingOrder() {
        List<Transaction> recentTransactions = transactionRepository.findRecentTransactionsByUser(testUser, 2);

        assertThat(recentTransactions).hasSize(2);
        assertThat(recentTransactions.get(0).getType()).isEqualTo(TransactionType.TRANSFER);
        assertThat(recentTransactions.get(1).getType()).isEqualTo(TransactionType.EXPENSE);
    }

    @Test
    void getMonthlySummary_ShouldReturnGroupedResults() {
        LocalDateTime start = now.minusDays(30);
        LocalDateTime end = now.plusDays(1);

        List<Object[]> monthlySummary = transactionRepository.getMonthlySummary(testUser, start, end);

        assertThat(monthlySummary).hasSize(2);
        for (Object[] summary : monthlySummary) {
            assertThat(summary).hasSize(4);
            assertThat(summary[0]).isInstanceOf(Integer.class);
            assertThat(summary[1]).isInstanceOf(Integer.class);
            assertThat(summary[2]).isInstanceOf(TransactionType.class);
            assertThat(summary[3]).isInstanceOf(BigDecimal.class);

            TransactionType type = (TransactionType) summary[2];
            BigDecimal sum = (BigDecimal) summary[3];

            if (type == TransactionType.INCOME) {
                assertThat(sum).isEqualByComparingTo(BigDecimal.valueOf(2500.00));
            } else if (type == TransactionType.EXPENSE) {
                assertThat(sum).isEqualByComparingTo(BigDecimal.valueOf(150.75));
            }
        }
    }

    @Test
    void saveTransaction_ShouldPersistTransactionWithAccountRelationship() {
        Transaction newTransaction = new Transaction();
        newTransaction.setAmount(BigDecimal.valueOf(99.99));
        newTransaction.setDescription("Test Expense");
        newTransaction.setType(TransactionType.EXPENSE);
        newTransaction.setTransactionDate(now);
        newTransaction.setUser(testUser);
        newTransaction.setAccount(checkingAccount);
        newTransaction.setCategory(expenseCategory);
        newTransaction.setNotes("Test transaction notes");
        newTransaction.setLocation("Test location");

        Transaction saved = transactionRepository.save(newTransaction);
        entityManager.flush();
        entityManager.clear();

        Transaction found = transactionRepository.findById(saved.getId()).orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getAccount().getId()).isEqualTo(checkingAccount.getId());
        assertThat(found.getAccount().getName()).isEqualTo("Primary Checking Account");
        assertThat(found.getAccount().getType()).isEqualTo(AccountType.CHECKING);
    }

    @Test
    void deleteTransaction_ShouldRemoveTransaction() {
        transactionRepository.delete(incomeTransaction);
        entityManager.flush();

        assertThat(transactionRepository.findById(incomeTransaction.getId())).isEmpty();
        List<Transaction> remainingTransactions = transactionRepository.findByUser(testUser);
        assertThat(remainingTransactions).hasSize(2);
    }

    @Test
    void findByUser_WithNoTransactions_ShouldReturnEmptyList() {
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setEmail("test2@valid.com");
        newUser.setPassword("Password123!");
        newUser.setFirstName("New");
        newUser.setLastName("User");
        newUser.setRole(User.Role.USER);
        newUser.setEnabled(true);
        newUser.setAccountNonExpired(true);
        newUser.setAccountNonLocked(true);
        newUser.setCredentialsNonExpired(true);
        entityManager.persist(newUser);

        Account newAccount = new Account();
        newAccount.setName("New Account");
        newAccount.setType(AccountType.CHECKING);
        newAccount.setBalance(BigDecimal.valueOf(1000.00));
        newAccount.setCurrency("USD");
        newAccount.setUser(newUser);
        entityManager.persist(newAccount);

        entityManager.flush();

        List<Transaction> transactions = transactionRepository.findByUser(newUser);
        assertThat(transactions).isEmpty();
    }

    @Test
    void accountBusinessMethods_ShouldWorkWithTransactions() {
        BigDecimal initialBalance = checkingAccount.getBalance();
        checkingAccount.deposit(BigDecimal.valueOf(100.00));
        assertThat(checkingAccount.getBalance()).isEqualByComparingTo(initialBalance.add(BigDecimal.valueOf(100.00)));

        checkingAccount.withdraw(BigDecimal.valueOf(50.00));
        assertThat(checkingAccount.getBalance()).isEqualByComparingTo(initialBalance.add(BigDecimal.valueOf(50.00)));

        assertThat(checkingAccount.isBalanceValidForAccountType()).isTrue();
    }

    @Test
    void transferTransaction_ShouldHaveTransferToAccount() {
        assertThat(transferTransaction.getTransferToAccount()).isNotNull();
        assertThat(transferTransaction.getTransferToAccount().getId()).isEqualTo(savingsAccount.getId());
        assertThat(transferTransaction.getTransferToAccount().getName()).isEqualTo("High-Yield Savings");
        assertThat(transferTransaction.getTransferToAccount().getType()).isEqualTo(AccountType.SAVINGS);

        assertThat(transferTransaction.isValidTransfer()).isTrue();
        assertThat(transferTransaction.isValidTransferAccounts()).isTrue();
    }

    @Test
    void transactionBusinessMethods_ShouldWorkCorrectly() {
        assertThat(incomeTransaction.isIncome()).isTrue();
        assertThat(incomeTransaction.isExpense()).isFalse();
        assertThat(incomeTransaction.isTransfer()).isFalse();

        assertThat(expenseTransaction.isExpense()).isTrue();
        assertThat(expenseTransaction.isIncome()).isFalse();

        assertThat(transferTransaction.isTransfer()).isTrue();
        assertThat(transferTransaction.isIncome()).isFalse();
        assertThat(transferTransaction.isExpense()).isFalse();

        assertThat(incomeTransaction.isRecent()).isTrue();
        assertThat(incomeTransaction.isLargeTransaction()).isTrue();
        assertThat(expenseTransaction.isLargeTransaction()).isFalse();

        assertThat(incomeTransaction.hasValidCategory()).isTrue();
        assertThat(incomeTransaction.isCategoryTypeValid()).isTrue();
    }

    @Test
    void accountRelationships_ShouldBeProperlyEstablished() {
        assertThat(checkingAccount.getUser().getId()).isEqualTo(testUser.getId());
        assertThat(savingsAccount.getUser().getId()).isEqualTo(testUser.getId());

        assertThat(checkingAccount.getCurrency()).isEqualTo("USD");
        assertThat(checkingAccount.getDescription()).isEqualTo("Main checking account for daily expenses");
        assertThat(checkingAccount.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(5000.00));

        assertThat(savingsAccount.getCurrency()).isEqualTo("USD");
        assertThat(savingsAccount.getDescription()).isEqualTo("Long-term savings account");
        assertThat(savingsAccount.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(10000.00));
    }

    @Test
    void findByAccount_WithMultipleAccountTypes_ShouldWorkCorrectly() {
        Account cashAccount = new Account();
        cashAccount.setName("Cash Wallet");
        cashAccount.setType(AccountType.CASH);
        cashAccount.setBalance(BigDecimal.valueOf(200.00));
        cashAccount.setCurrency("USD");
        cashAccount.setUser(testUser);
        entityManager.persist(cashAccount);

        Transaction cashTransaction = new Transaction();
        cashTransaction.setAmount(BigDecimal.valueOf(50.00));
        cashTransaction.setDescription("Cash Expense");
        cashTransaction.setType(TransactionType.EXPENSE);
        cashTransaction.setTransactionDate(now);
        cashTransaction.setUser(testUser);
        cashTransaction.setAccount(cashAccount);
        cashTransaction.setCategory(expenseCategory);
        entityManager.persist(cashTransaction);

        entityManager.flush();

        List<Transaction> cashAccountTransactions = transactionRepository.findByAccount(cashAccount);
        assertThat(cashAccountTransactions).hasSize(1);
        assertThat(cashAccountTransactions.get(0).getAccount().getType()).isEqualTo(AccountType.CASH);
        assertThat(cashAccountTransactions.get(0).getDescription()).isEqualTo("Cash Expense");
    }
}