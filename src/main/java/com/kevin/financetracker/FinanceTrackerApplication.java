package com.kevin.financetracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan("com.kevin.financetracker.model")
@EnableJpaRepositories("com.kevin.financetracker.repository")
public class FinanceTrackerApplication {
	public static void main(String[] args) {
		SpringApplication.run(FinanceTrackerApplication.class, args);
	}
}