# Finance Tracker 💰

A comprehensive personal finance management application built with Spring Boot 3 that helps users track accounts, categories, budgets, and transactions.

## 🚀 Technology Stack

- **Java 21** - Modern Java features
- **Spring Boot 3.5.6** - Enterprise application framework
- **Maven** - Dependency management
- **PostgreSQL** - Relational database
- **Spring Data JPA** - ORM and data access
- **Spring Security** - Security framework
- **Spring Web** - REST API development
- **Lombok** - Code reduction
- **Spring Boot Test** - Testing framework
- **Spring Boot Validation** - Input validation

## 📋 Features

### 🏦 Multi-Account Management
- Support for multiple account types (Checking, Savings, Credit Card, Cash, Investment)
- Real-time balance tracking
- Account transfers and transactions
- Portfolio overview

### 📊 Category System
- Hierarchical categories (Income/Expense)
- Custom subcategories for detailed tracking
- Budget allocation per category
- Spending analysis and reports

### 💡 Budget Planning
- Time-period based budgeting (Monthly, Weekly, Custom)
- Category-wise spending limits
- Budget vs. actual tracking
- Visual progress indicators

### 🔄 Transaction Management
- Income and expense tracking
- Transfer between accounts
- Categorization and tagging
- Date-based filtering and search

### 📈 Financial Analytics
- Multi-account portfolio view
- Spending trends and patterns
- Budget adherence reports
- Net worth calculation
- Category-wise analysis

## 🏗️ System Architecture
Controller Layer (REST API)
↓
Service Layer (Business Logic)
↓
Repository Layer (Data Access)
↓
Database (PostgreSQL)

text

### Core Entities
- **User** - Application users and authentication
- **Account** - Financial accounts with balances
- **Category** - Transaction categorization system
- **BudgetPeriod** - Time-bound budget planning
- **Transaction** - Financial records and history

## 🗂️ Project Structure
src/main/java/com/kevin/financetracker/
├── controller/ # REST API endpoints
├── service/ # Business logic layer
├── repository/ # Data access layer
├── model/ # Entity classes
└── dto/ # Data transfer objects

text

## ⚙️ Prerequisites

- Java 21 or later
- PostgreSQL 12+
- Maven 3.6+
- IDE (IntelliJ IDEA recommended)

# Database Setup
- Configure your database connection in `application.properties`
- Create a `.env` file with your database credentials
- The application now uses custom exceptions and validation

# API Documentation
- UserController endpoints are available at `/api/users`

# Exception Handling
- Custom exceptions for business logic
- Global exception handler for consistent error responses

### Security Implementation
- **Spring Security** with JWT authentication
- **JWT Authentication Filter** for request validation
- **Secure endpoints** with role-based access
- **Password encryption** using BCrypt

### Enhanced Data Transfer
- **Request DTOs** for input validation and type safety
- **Response DTOs** for structured API output
- **Comprehensive validation** with custom validators

### Transaction Management
- **CRUD operations** for transactions
- **Custom transaction type validation**
- **Enhanced service layer** with better error handling
- **RESTful API** endpoints

## 🛠️ Tech Stack

- **Backend**: Spring Boot 3.x
- **Security**: Spring Security + JWT
- **Validation**: Bean Validation + Custom Validators
- **Database**: postgres 
- **Build Tool**: Maven

## 🚀 Quick Start

### 1. Clone and Setup
```bash
git clone https://github.com/kevinnyangweso/finance-tracker.git
cd finance-tracker
