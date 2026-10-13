package com.financetracker.repository;

import com.financetracker.entity.Category;
import com.financetracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByUserOrderByNameAsc(User user);

    Optional<Category> findByIdAndUser(Long id, User user);

    Optional<Category> findByUserAndName(User user, String name);

    boolean existsByUserAndName(User user, String name);
}