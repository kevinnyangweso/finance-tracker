package com.kevin.financetracker;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.SQLException;

@SpringBootTest
@ActiveProfiles("test")
class FinanceTrackerApplicationTests {
	@Autowired
	private DataSource dataSource;

	@Test
	void printDataSourceProperties() throws SQLException {
		System.out.println("URL: " + dataSource.getConnection().getMetaData().getURL());
		System.out.println("Username: " + dataSource.getConnection().getMetaData().getUserName());
	}

	@Test
	void contextLoads() {
	}
}