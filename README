# TODO List Application

This README provides instructions on how to set up and start the TODO List application using Docker Compose from scratch.

## Prerequisites

1. Ensure you have the following tools installed on your machine:
   - [Docker](https://www.docker.com/get-started)
   - [Docker Compose](https://docs.docker.com/compose/install/)
   - [Java Development Kit (JDK) 23](https://jdk.java.net/)
   - [Maven](https://maven.apache.org/install.html)

2. Verify the installation by running the following commands:
   ```sh
   docker --version
   docker-compose --version
   java --version
   mvn --version
   ```

## Project Structure

- `src/` - Contains the application source code.
- `Dockerfile` - Docker configuration file for the application.
- `docker-compose.yaml` - Configuration file for Docker Compose.
- `target/` - Directory where the compiled `.jar` file will be generated after the Maven build.

## Setup Instructions

### Step 1: Clone the Repository

Clone this repository to your local machine:
```sh
git clone <repository-url>
cd <repository-folder>
```

### Step 2: Build the Application

Build the project using Maven. This step will compile the code and generate the `.jar` file in the `target/` directory:
```sh
mvn clean install -Pskip-tests
```

### Step 3: Start the Application

Run the following command to start the application and its dependencies (MySQL database) using Docker Compose:
```sh
docker-compose up -d
```
This will:
- Build the Docker image for the application.
- Start the MySQL container.
- Start the application container.

### Step 4: Verify the Setup

1. Check the running containers:
   ```sh
   docker ps
   ```
   Ensure both the `mysql-container` and `todo-app` containers are running.

2. Open a web browser and navigate to:
   ```
   http://localhost:8080
   ```
   You should see the login page for the TODO List application.

## Environment Variables

The `docker-compose.yaml` file includes the following environment variables:

### MySQL Service
- `MYSQL_ROOT_PASSWORD`: Password for the MySQL root user (default: `root`)
- `MYSQL_DATABASE`: Name of the database (default: `todolist`)
- `MYSQL_USER`: Username for the database (default: `todo_user`)
- `MYSQL_PASSWORD`: Password for the database user (default: `password`)

### Application Service
- `SPRING_DATASOURCE_URL`: JDBC URL for the database connection (default: `jdbc:mysql://mysql:3306/todolist`)
- `SPRING_DATASOURCE_USERNAME`: Database username (default: `todo_user`)
- `SPRING_DATASOURCE_PASSWORD`: Database password (default: `password`)

## Stopping the Application

To stop the application and its dependencies, run:
```sh
docker-compose down
```

## Troubleshooting

1. **Database Connection Issues:**
   - Ensure the MySQL container is running: `docker ps`
   - Verify the database credentials in the `docker-compose.yaml` file.

2. **Port Conflicts:**
   - Make sure ports `3306` (MySQL) and `8080` (application) are not in use by other services.

3. **Logs:**
   - Check the logs of the containers for errors:
     ```sh
     docker logs todo-app
     docker logs mysql-container
     ```

## Cleanup

To remove all containers, networks, and volumes associated with the application, run:
```sh
docker-compose down -v
```

---

Now you are ready to start using the TODO List application. Enjoy!

