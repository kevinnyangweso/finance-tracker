package com.kevin.financetracker.repository;

import com.kevin.financetracker.model.Account;
import com.kevin.financetracker.model.AccountType;
import com.kevin.financetracker.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    // Find all accounts for a specific user
    List<Account> findByUser(User user);

    // Find all accounts for a user by user ID
    List<Account> findByUserId(Long userId);

    // Find accounts by type for a specific user
    List<Account> findByUserAndType(User user, AccountType type);

    // Find account by name and user (for uniqueness check)
    Optional<Account> findByUserAndName(User user, String name);

    // Find accounts with balance greater than specified amount
    List<Account> findByUserAndBalanceGreaterThan(User user, BigDecimal balance);

    // Find accounts with balance less than specified amount
    List<Account> findByUserAndBalanceLessThan(User user, BigDecimal balance);

    // Custom query to get total balance for a user
    @Query("SELECT SUM(a.balance) FROM Account a WHERE a.user = :user")
    Optional<BigDecimal> getTotalBalanceByUser(@Param("user") User user);

    // Check if account name already exists for a user
    boolean existsByUserAndName(User user, String name);

    // Find accounts by type
    List<Account> findByType(AccountType type);
}
