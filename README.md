# Finance Tracker

A personal finance management web application that helps individuals track their income and expenses, giving them a clear picture of their financial health.

---

## Table of Contents

- [About the Project](#about-the-project)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
- [Usage](#usage)
- [Project Structure](#project-structure)
- [API Endpoints](#api-endpoints)
- [Roadmap](#roadmap)
- [Contributing](#contributing)
- [License](#license)
- [Contact](#contact)

---

## About the Project

**Finance Tracker** is a web application designed for individuals who want to take control of their personal finances. It provides a simple, intuitive way to record income and expenses, categorize transactions, and visualize spending habits over time.

Whether you're a student managing a tight budget, a professional tracking monthly expenses, or someone saving for a big goal, Finance Tracker helps you stay on top of your money - without the complexity of traditional accounting software.

> **Our Mission:** Make personal finance tracking simple, transparent, and accessible to everyone.

---

## Features

### Core Features (MVP)

- User Authentication - Secure sign-up, login, and logout using Spring Security
- Income Tracking - Record and manage all sources of income
- Expense Tracking - Log daily, weekly, or monthly expenses
- Categories - Organize transactions (e.g., Food, Rent, Salary, Transport)
- Dashboard - See total income, total expenses, and current balance at a glance
- Transaction History - View, edit, and delete past transactions
- Search and Filter - Find transactions by date, category, or amount

### Future Features (Post-MVP)

- Charts and Reports - Visualize spending trends over time
- Budget Goals - Set monthly budgets per category
- Notifications - Alerts for overspending or upcoming bills
- Export Data - Download transactions as CSV or PDF
- Dark Mode - For late-night budgeting sessions
- Mobile-Responsive Design - Full support for phones and tablets
- Bank Integration - Auto-import transactions via APIs (future)

---

## Tech Stack

This project uses a modern, industry-standard stack built around the Java ecosystem.

### Backend

- Java 21 - Programming language (LTS version)
- Spring Boot 4.1.1 - Application framework
- Spring Web (MVC) - Building REST APIs
- Spring Data JPA - Database access layer
- Spring Security - Authentication and authorization
- JWT (JSON Web Tokens) - Stateless authentication
- Hibernate - ORM (Object-Relational Mapping)
- Bean Validation - Input validation with annotations
- Maven - Build and dependency management

### Database

- PostgreSQL - Relational database for production
- H2 Database - In-memory database for testing and development

### Frontend

- Thymeleaf - Server-side rendered templates with Spring Boot
- Tailwind CSS - Utility-first styling

### DevOps and Tools

- Git and GitHub - Version control
- Postman / Insomnia - API testing
- Docker - Containerization
- Render / Railway / AWS - Deployment
- GitHub Actions - CI/CD pipelines

---

## Getting Started

Follow these steps to set up the project locally.

### Prerequisites

Make sure you have the following installed:

- Java Development Kit (JDK) 21+
- Maven (or use the included Maven Wrapper ./mvnw)
- PostgreSQL (or use H2 for local dev)
- Git
- An IDE such as IntelliJ IDEA, VS Code, or Eclipse

### Installation

1. Clone the repository

git clone https://github.com/kevinnyangweso/finance-tracker.git
cd finance-tracker

2. Configure the database

Create a PostgreSQL database:

CREATE DATABASE finance_tracker;

3. Set up environment variables

Create an application-dev.properties file in src/main/resources/:

# Server
server.port=8080

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/finance_tracker
spring.datasource.username=your_db_username
spring.datasource.password=your_db_password
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# JWT
app.jwt.secret=your_super_secret_key
app.jwt.expiration=86400000

Or use H2 for quick local development:

spring.datasource.url=jdbc:h2:mem:financetracker
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.h2.console.enabled=true

4. Build the project

./mvnw clean install

5. Run the application

./mvnw spring-boot:run

6. Verify it is running

To enable a health check endpoint, add Spring Boot Actuator to pom.xml:

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

Then run the app and check:

http://localhost:8080/actuator/health

You should see:

{ "status": "UP" }

---

## Usage

1. Register a new account via POST /api/auth/register.
2. Log in via POST /api/auth/login to receive a JWT token.
3. Include the token in the Authorization header for all protected requests:

Authorization: Bearer <your_jwt_token>

4. Add income via POST /api/transactions/income.
5. Add expenses via POST /api/transactions/expense.
6. View your balance on the dashboard via GET /api/dashboard/summary.
7. Manage transactions - search, filter, edit, or delete entries.

### Example Request - Add an Expense

curl -X POST http://localhost:8080/api/transactions/expense \
  -H "Authorization: Bearer <your_jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 45.00,
    "category": "Food",
    "date": "2026-10-08",
    "note": "Lunch with team"
  }'

### Example Response

{
  "id": 1,
  "type": "EXPENSE",
  "amount": 45.00,
  "category": "Food",
  "date": "2026-10-08",
  "note": "Lunch with team",
  "createdAt": "2026-10-08T12:34:56"
}

### Example Transaction

| Field | Value |
| :--- | :--- |
| Type | Expense |
| Amount | 45.00 |
| Category | Food |
| Date | 2026-10-08 |
| Note | Lunch with team |

---

## Project Structure

finance-tracker/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── financetracker/
│   │   │           ├── FinanceTrackerApplication.java
│   │   │           ├── config/
│   │   │           ├── controller/
│   │   │           ├── service/
│   │   │           ├── repository/
│   │   │           ├── entity/
│   │   │           ├── dto/
│   │   │           ├── exception/
│   │   │           ├── security/
│   │   │           └── util/
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-dev.properties
│   │       ├── application-prod.properties
│   │       └── templates/
│   └── test/
│       └── java/
│           └── com/
│               └── financetracker/
│                   ├── controller/
│                   ├── service/
│                   └── repository/
├── .gitignore
├── pom.xml
├── README.md
└── LICENSE

---

## API Endpoints

### Authentication

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| POST | /api/auth/register | Register a new user |
| POST | /api/auth/login | Log in and receive JWT |
| POST | /api/auth/logout | Invalidate session (optional) |

### Transactions

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| GET | /api/transactions | Get all transactions for user |
| GET | /api/transactions/{id} | Get a single transaction |
| POST | /api/transactions/income | Add income |
| POST | /api/transactions/expense | Add expense |
| PUT | /api/transactions/{id} | Update a transaction |
| DELETE | /api/transactions/{id} | Delete a transaction |

### Dashboard

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| GET | /api/dashboard/summary | Total income, expenses, balance |

### Categories

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| GET | /api/categories | List all categories |
| POST | /api/categories | Create a category |

---

## Roadmap

- [x] Project planning and README
- [ ] Set up Spring Boot project (Maven, dependencies)
- [ ] Configure database (PostgreSQL + Hibernate)
- [ ] Implement User entity and authentication (Spring Security + JWT)
- [ ] Build Category entity and CRUD APIs
- [ ] Build Transaction entity and CRUD APIs
- [ ] Add search and filtering for transactions
- [ ] Build dashboard summary endpoint
- [ ] Write unit and integration tests (JUnit, Mockito)
- [ ] UI/UX design (wireframes and mockups)
- [ ] Frontend: login/signup pages
- [ ] Frontend: dashboard
- [ ] Frontend: transaction management
- [ ] Integration and end-to-end testing
- [ ] Deployment (MVP launch)
- [ ] Post-MVP: charts, budgets, notifications

---

## Contributing

Contributions are welcome! If you'd like to improve Finance Tracker:

1. Fork the repository
2. Create a feature branch

git checkout -b feature/amazing-feature

3. Commit your changes

git commit -m "Add amazing feature"

4. Push to the branch

git push origin feature/amazing-feature

5. Open a Pull Request

Please make sure to update tests as appropriate.

---

## License

This project is licensed under the MIT License - see the LICENSE file for details.

---

## Contact

**Kevin Nyangweso** - nyangwesokevin1@gmail.com

**Project Link:** https://github.com/kevinnyangweso/finance-tracker

**Live Demo:** (coming soon)

---

## Acknowledgments

- Spring Boot Documentation
- Baeldung - Spring tutorials
- Choose an Open Source License
- Shields.io for badges
- Best-README-Template for inspiration

---

If you find this project useful, please consider giving it a star on GitHub!
