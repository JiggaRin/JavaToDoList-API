# Java ToDo List API

[![Java](https://img.shields.io/badge/Java-21-blue)](https://jdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.4-brightgreen)](https://spring.io/projects/spring-boot)
[![Docker](https://img.shields.io/badge/Docker-Compose-blue)](https://www.docker.com/)

The **Java ToDo List API** is a robust *backend RESTful API* for managing users, tasks, and task categories in a to-do list application. Built with **Spring Boot**, it features secure **JWT-based authentication**, a **MySQL** database with **Flyway** migrations, and comprehensive unit and integration tests. This project showcases expertise in designing *scalable REST APIs*, implementing advanced security with role-based and permission-based access, managing relational databases, and containerizing applications with **Docker**.

This is a personal project to master **Spring Boot**, *REST API design*, *secure backend development*, and *testing*.

[**GitHub Repository**](https://github.com/JiggaRin/JavaToDoList-API)

---

## Features

- **User Authentication**:
  - Register new users with email, username, and password (`/api/register`).
  - Authenticate users and issue JWT tokens (`/api/login`).
  - Refresh JWT tokens (`/api/refresh`).
  - Log out users (`/api/logout`).
- **Admin User Management**:
  - Create users with specific roles (`USER`, `ADMIN`) (`/api/admin/register`, admin only).
  - Delete users and associated data (`/api/admin/users/{userId}`, admin only).
- **User Profile Management**:
  - View authenticated user’s profile (`/api/users/me`).
  - Update profile details (e.g., name, email) (`/api/users/me`).
  - Securely update user password (`/api/users/me/password`).
- **Task Management**:
  - Create tasks with title, description, status, priority, due date, and optional category (`/api/tasks`).
  - List all tasks for the authenticated user (`/api/tasks`).
  - Retrieve a specific task by ID (`/api/tasks/{taskId}`).
  - Update task details (`/api/tasks/{taskId}`).
  - Delete tasks (`/api/tasks/{taskId}`).
  - Restricted to task owners via permission-based access.
- **Task Category Management**:
  - Create categories to organize tasks (`/api/categories`).
  - List all categories for the authenticated user (`/api/categories`).
  - Retrieve a specific category by ID (`/api/categories/{categoryId}`).
  - Update category details (`/api/categories/{categoryId}`).
  - Delete categories (`/api/categories/{categoryId}`).
  - Restricted to category owners.
- **Security**:
  - JWT-based authentication with `JwtAuthenticationFilter`.
  - Role-based access control (`USER`, `ADMIN`).
  - Permission-based access for tasks and categories (via `PermissionEvaluator`).
- **Database**:
  - MySQL with tables for `user`, `task`, `refresh_token`, and `category`.
  - Cascading deletes (e.g., deleting a user removes tasks, tokens, and categories).
  - Flyway migrations for schema consistency.
- **Testing**:
  - Unit tests for services and repositories.
  - Integration tests for repository interactions with the database.
- **API Documentation** (In Progress):
  - Swagger UI for interactive API documentation (work in progress).

---

## Tech Stack

- **Java 21**: Modern Java features for robust development.
- **Spring Boot 3.3.4**: Framework for RESTful APIs.
- **Spring Security**: JWT authentication, role-based, and permission-based authorization.
- **MySQL**: Relational database for persistent storage.
- **Flyway**: Database migrations for schema management.
- **Springdoc OpenAPI 2.6.0**: API documentation (Swagger UI, in progress).
- **Maven**: Dependency management and build tool.
- **Docker & Docker Compose**: Containerized application and database.
- **JUnit 5**: Unit and integration testing.

---

## Prerequisites

Ensure the following tools are installed:
- [**Docker**](https://www.docker.com/get-started)
- [**Docker Compose**](https://docs.docker.com/compose/install/)
- [**Java Development Kit (JDK) 21**](https://jdk.java.net/)
- [**Maven**](https://maven.apache.org/install.html)

Verify installations:
```bash
docker --version
docker-compose --version
java --version
mvn --version