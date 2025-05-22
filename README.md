# Java ToDo List API

[![Java](https://img.shields.io/badge/Java-17-blue)](https://jdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4.5-brightgreen)](https://spring.io/projects/spring-boot)
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
  - Create users with specific roles (`USER`, `ADMIN`, `MODERATOR`) (`/api/admin/register`, admin only).
  - Delete users and associated data (`/api/admin/users/{userId}`, admin only).
- **User Profile Management**:
  - Update profile details (e.g., name, email) (`/api/users`).
  - Securely update user password (`/api/users/change-password`).
- **Task Management**:
  - Create tasks with title, description, status, and category (`/api/tasks`).
  - List all tasks for (`/api/tasks`, admin/moderator only).
  - Retrieve a specific task by ID (`/api/tasks/{taskId}`, admin/moderator only).
  - Retrieve all user's task by userID (`/api/tasks/user/{userId}`, admin/moderator only).
  - Retrieve all user's task (`/api/tasks/my-tasks`, task's owner only).
  - Update task details (`/api/tasks/{taskId}`).
  - Update task status (`/api/tasks/update-status/{taskId}`)
  - Delete tasks (`/api/tasks/{taskId}`).
  - Restricted to task owners via permission-based access.
- **Task Category Management**:
  - Create categories to organize tasks (`/api/categories`, admin/moderator only).
  - List all categories (`/api/categories`, admin/moderator only).
  - Retrieve a specific category by ID (`/api/categories/{categoryId}`, admin/moderator only).
  - Update category details (`/api/categories/{categoryId}`, admin/moderator only).
  - Delete categories (`/api/categories/{categoryId}`, admin/moderator only).
- **Security**:
  - JWT-based authentication with `JwtAuthenticationFilter`.
  - Role-based access control (`USER`, `ADMIN`, `MODERATOR`).
  - Permission-based access for tasks.
- **Database**:
  - MySQL with tables for `users`, `tasks`, `refresh_token`, `categories`, `task_categories` and `flyway_schema_history`.
  - Cascading deletes (e.g., deleting a user removes tasks, tokens, and categories).
  - Flyway migrations for schema consistency.
- **Testing**:
  - Unit tests for services.
  - Integration tests for repository interactions with the database.
- **API Documentation** (In Progress):
  - Swagger UI for interactive API documentation (work in progress).

---

## Tech Stack

- **Java 17**: Modern Java features for robust development.
- **Spring Boot 3.4.5**: Framework for RESTful APIs.
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
- [**Java Development Kit (JDK) 17**](https://jdk.java.net/)
- [**Maven**](https://maven.apache.org/install.html)

Verify installations:
```bash
docker --version
docker-compose --version
java --version
mvn --version
```

## Security
- **JWT-Based Authentication**: Uses `JwtAuthenticationFilter` to validate tokens for protected endpoints. Public endpoints (`/api/login`, `/api/refresh`, `/api/register`, `/api/logout`) bypass authentication.
- **Role-Based Access**: Supports `USER`, `ADMIN`, `MODERATOR` roles.
- **Permission-Based Access**: Uses `PermissionEvaluator` for task ownership.
---

## Project Structure

- `src/` - Source code (controllers, services, repositories).
- `Dockerfile` - Docker configuration for the application.
- `docker-compose.yaml` - Docker Compose setup for app and MySQL.
- `target/` - Compiled `.jar` file generated by Maven.
- `src/test/` - Unit and integration tests.

---


#### Section 4: Configuration
## Configuration

The application uses **`Spring Boot profiles`** to manage configurations for different environments:
- **`local`**: For local development, where the Spring Boot app runs manually (e.g., via `mvn spring-boot:run`) and connects to a Dockerized MySQL database on `localhost:3306`.
- **`docker`**: For full Dockerized deployment, where both the app and MySQL run in Docker containers, connecting to MySQL on `mysql:3306` within the Docker network.

This setup prevents issues like the **`Communications link failure`** error, which occurs when using a local database URL (`localhost:3306`) in a Dockerized environment. Profiles ensure the correct database URL is used for each setup.

## Setup Instructions

### 1. Clone the Repository
```bash
git clone https://github.com/JiggaRin/JavaToDoList-API.git
cd JavaToDoList-API

```

### 2. Build the Application
Compile the project and generate the `.jar` file:
```bash
mvn clean package -DskipTests
```
> [!IMPORTANT]
> This flag skips unit and integration tests during the build process. It is recommended only for the initial setup to quickly verify that the project compiles and dependencies are correctly resolved.
> Running tests requires a properly configured database and environment, which may not yet be available during the first install. Skipping them avoids test failures related to missing or uninitialized infrastructure.
> After the initial setup and once the database is running (e.g., via Docker), you should remove the `-DskipTests` flag to ensure that all tests pass and the system behaves as expected.

### 3. Start the Application
## Option 1: Local Development (Recommended for Development)

1. Run a Dockerized MySQL container and the app manually:
```bash
docker-compose up -d mysql
```

2. Run the Spring Boot app with the `local` profile:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

3. Verify the MySQL container is running:
```bash
docker ps
```

## Option 2: Dockerized Deployment

Run both the app and MySQL in Docker containers:
1. Ensure the docker profile is set in docker-compose.yaml:
```bash
environment:
  - SPRING_PROFILES_ACTIVE=docker
```
2. Start both services:
```bash
docker-compose up --build -d
```

This will:
- Build the Docker image for the API.
- Start the MySQL container.
- Run the application on port `8080`.

### 4. Verify the Setup
- Check running containers:
  ```bash
  docker ps
  ```
  Confirm `mysql-container` (and `todo-app` for Dockerized setup) are running.
- Check logs for errors:
  ```bash
  docker logs mysql-container
  docker logs todo-app  # If using Dockerized setup
```

## API Endpoints

<!-- Authentication -->

| Method | Endpoint               | Description              | Access  |
|--------|------------------------|--------------------------|---------|
| POST   | `/api/register`        | Register user            | Public  |
| POST   | `/api/login`           | Authenticate user        | Public  |
| POST   | `/api/refresh`         | Refresh JWT token        | Public  |
| POST   | `/api/logout`          | Log out user             | Public  |

<!-- Admin -->

| Method | Endpoint                        | Description              | Access      |
|--------|---------------------------------|--------------------------|-------------|
| POST   | `/api/admin/register`           | Register user with role  | Admin only  |
| DELETE | `/api/admin/users/{userId}`     | Delete user by ID        | Admin only  |

<!-- User -->

| Method | Endpoint                        | Description              | Access        |
|--------|---------------------------------|--------------------------|---------------|
| PUT    | `/api/users/update`             | Update user profile      | Authenticated |
| PUT    | `/api/users/change-password`    | Update user password     | Authenticated |

<!-- Task -->

| Method | Endpoint                              | Description                  | Access                        |
|--------|---------------------------------------|------------------------------|-------------------------------|
| POST   | `/api/tasks`                          | Create task                  | Authenticated                 |
| GET    | `/api/tasks`                          | List all tasks               | Admin/Moderator               |
| GET    | `/api/tasks/{taskId}`                 | Get task by ID               | Admin/Moderator               |
| PUT    | `/api/tasks/{taskId}`                 | Update task                  | Task Owner/Admin/Moderator    |
| DELETE | `/api/tasks/{taskId}`                 | Delete task                  | Task Owner/Admin/Moderator    |
| GET    | `/api/tasks/user/{userId}`            | Get tasks by user ID         | Admin/Moderator               |
| GET    | `/api/tasks/my-tasks`                 | Get user’s tasks             | Task Owner                    |
| PUT    | `/api/tasks/update-status/{taskId}`   | Update task status           | Task Owner/Admin/Moderator    |

<!-- Task Category -->

| Method | Endpoint                        | Description                  | Access            |
|--------|---------------------------------|------------------------------|-------------------|
| POST   | `/api/categories`               | Create category              | Admin/Moderator   |
| GET    | `/api/categories`               | List all categories          | Admin/Moderator   |
| GET    | `/api/categories/{categoryId}`  | Get category by ID           | Admin/Moderator   |
| PUT    | `/api/categories/{categoryId}`  | Update category              | Admin/Moderator   |
| DELETE | `/api/categories/{categoryId}`  | Delete category              | Admin/Moderator   |

```
## Postman Collection

A Postman collection is available to test the API endpoints. It includes requests for all Authentication, Admin, User, Task, and Task Category endpoints, organized into folders for easy navigation.

### Usage
1. Install [Postman](https://www.postman.com/downloads/).
2. Import the collection (`docs/JavaToDoList-API.postman_collection.json`).
3. Set the `baseUrl` variable to `http://localhost:8080` (or your deployed URL).
4. Run the `POST /api/login` request to obtain a JWT token.
5. Test other endpoints using the stored token.

The collection includes example requests.

## Environment Variables

Defined in `docker-compose.yaml`:

### MySQL Service
- `MYSQL_ROOT_PASSWORD`: `root`
- `MYSQL_DATABASE`: `todolist`
- `MYSQL_USER`: `todo_user`
- `MYSQL_PASSWORD`: `password`

### Application Service
- `SPRING_DATASOURCE_URL`: `jdbc:mysql://mysql:3306/todolist`
- `SPRING_DATASOURCE_USERNAME`: `todo_user`
- `SPRING_DATASOURCE_PASSWORD`: `password`

---

## Stopping the Application
```bash
docker-compose down
```

---

## Troubleshooting

- **Database Issues**:
  - Verify MySQL container: `docker ps`.
  - Check credentials in `docker-compose.yaml`.
- **Port Conflicts**:
  - Ensure ports `3306` (MySQL) and `8080` (app) are free.
- **Logs**:
  - Inspect container logs:
    ```bash
    docker logs todo-app
    docker logs mysql-container
    ```

---

## Cleanup
Remove containers, networks, and volumes:
```bash
docker-compose down -v
```

---

## Future Improvements
- Complete **Swagger UI** integration for interactive API documentation.
- Expand integration tests for controllers and services.
- Implement documentation for Postman collection.
---

## License
This project is licensed under the [**MIT License**](./LICENSE).

---

Thank you for exploring the **Java ToDo List API**! This project highlights my skills in building *secure*, *scalable* REST APIs with **Spring Boot**, **Docker**, and *testing*. For questions or feedback, feel free to open an issue on [**GitHub**](https://github.com/JiggaRin/JavaToDoList-API).