package todo.list.todo_list.integration.repository;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import todo.list.todo_list.entity.Category;
import todo.list.todo_list.entity.Task;
import todo.list.todo_list.entity.User;
import todo.list.todo_list.model.Status;
import todo.list.todo_list.repository.CategoryRepository;
import todo.list.todo_list.repository.TaskRepository;
import todo.list.todo_list.repository.UserRepository;

@DataJpaTest
@EntityScan("todo.list.todo_list.entity")
@ActiveProfiles("test")
class TaskRepositoryIntegrationTest {

    @Autowired
    TaskRepository taskRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    CategoryRepository categoryRepository;

    private Task setupTask(String title, Set<Category> categories, User owner, Status status) {
        Task task = new Task();
        task.setTitle(title);
        task.setStatus(status);
        task.setDescription("Task Description");
        task.setOwner(owner);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        task.setCategories(categories);
        return task;
    }

    private User setupUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPassword("password123");
        return user;
    }

    private Category setupCategory(String categoryName) {
        Category category = new Category();
        category.setName(categoryName);
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());
        return category;
    }

    @Test
    @DisplayName("Save Task with valid data persists and returns Task")
    void saveTask_validData_successfulSave() {
        User user = setupUser("testuser");
        User savedUser = userRepository.save(user);

        Category category = setupCategory("Category A");
        Category savedCategory = categoryRepository.save(category);

        Set<Category> categories = new HashSet<>();
        categories.add(savedCategory);

        Task task = setupTask("Task Title", categories, savedUser, Status.TODO);

        Task savedTask = taskRepository.save(task);

        assertNotNull(savedTask.getId());
        assertEquals("Task Title", savedTask.getTitle());
        assertEquals(savedUser.getId(), savedTask.getOwner().getId());
        assertEquals(Status.TODO, savedTask.getStatus());
        assertEquals(1, savedTask.getCategories().size());
        assertTrue(savedTask.getCategories().contains(savedCategory));
        assertNotNull(savedTask.getCreatedAt());
        assertNotNull(savedTask.getUpdatedAt());
    }

    @Test
    @DisplayName("Save Task with NULL Title throws DataIntegrityViolationException")
    void saveTask_nullTitle_ShouldThrowException() {
        User user = setupUser("testuser");
        User savedUser = userRepository.save(user);

        Task task = setupTask(null, new HashSet<>(), savedUser, Status.TODO);

        try {
            taskRepository.saveAndFlush(task);
            fail("Expected DataIntegrityViolationException but none was thrown");
        } catch (DataIntegrityViolationException e) {
            assertTrue(true);
        }
    }

    @Test
    @DisplayName("Save Task with NULL Owner throws DataIntegrityViolationException")
    void saveTask_nullOwner_ShouldThrowException() {
        Task task = setupTask("Task Title", new HashSet<>(), null, Status.TODO);

        try {
            taskRepository.saveAndFlush(task);
            fail("Expected DataIntegrityViolationException but none was thrown");
        } catch (DataIntegrityViolationException e) {
            assertTrue(true);
        }
    }

    @Test
    @DisplayName("Save Task with NULL Status throws DataIntegrityViolationException")
    void saveTask_nullStatus_ShouldThrowException() {
        User user = setupUser("testuser");
        User savedUser = userRepository.save(user);

        Task task = setupTask("Task Title", new HashSet<>(), savedUser, null);

        try {
            taskRepository.saveAndFlush(task);
            fail("Expected DataIntegrityViolationException but none was thrown");
        } catch (DataIntegrityViolationException e) {
            assertTrue(true);
        }
    }

    @Test
    @DisplayName("Save Task with valid data and set Parent Task returns Task")
    void saveTask_setParentTask_successfulSave() {
        User user = setupUser("testuser");
        User savedUser = userRepository.save(user);

        Task parentTask = setupTask("Parent Task Title", new HashSet<>(), savedUser, Status.TODO);
        Task savedParentTask = taskRepository.save(parentTask);

        Task childTask = setupTask("Task Title", new HashSet<>(), savedUser, Status.TODO);
        childTask.setParentTask(savedParentTask);

        taskRepository.save(childTask);

        assertNotNull(childTask.getId());
        assertEquals("Task Title", childTask.getTitle());
        assertEquals(savedUser.getId(), childTask.getOwner().getId());
        assertEquals(Status.TODO, childTask.getStatus());
        assertEquals(savedParentTask.getId(), childTask.getParentTask().getId());
        assertNotNull(childTask.getCreatedAt());
        assertNotNull(childTask.getUpdatedAt());
    }

    @Test
    @DisplayName("Find Parent Tasks by User ID returns Matching Tasks")
    void findParentTasks_byUserId_successfulFind() {
        User user1 = setupUser("user1");
        User savedUser1 = userRepository.save(user1);

        Task user1Task1 = setupTask("User1 Task 1", new HashSet<>(), savedUser1, Status.TODO);
        Task user1Task2 = setupTask("User1 Task 2", new HashSet<>(), savedUser1, Status.TODO);
        Task savedUser1Task1 = taskRepository.save(user1Task1);
        taskRepository.save(user1Task2);

        User user2 = setupUser("user2");
        User savedUser2 = userRepository.save(user2);
        Task user2Task = setupTask("User2 Task", new HashSet<>(), savedUser2, Status.TODO);
        taskRepository.save(user2Task);

        Task user1ChildTask = setupTask("User1 Child Task", new HashSet<>(), savedUser1, Status.TODO);
        user1ChildTask.setParentTask(savedUser1Task1);
        taskRepository.save(user1ChildTask);

        Pageable pageable = PageRequest.of(0, 10);
        Page<Task> result = taskRepository.findParentTasks(savedUser1.getId(), null, pageable);

        assertEquals(2, result.getContent().size(), "Should return 2 parent tasks for user1");
        List<String> taskTitles = result.getContent().stream()
                .map(Task::getTitle)
                .collect(Collectors.toList());
        assertTrue(taskTitles.contains("User1 Task 1"), "Should contain User1 Task 1");
        assertTrue(taskTitles.contains("User1 Task 2"), "Should contain User1 Task 2");
        result.getContent().forEach(task -> {
            assertEquals(savedUser1.getId(), task.getOwner().getId(), "Task should belong to user1");
            assertNull(task.getParentTask(), "Task should have no parent");
        });
        assertEquals(2, result.getTotalElements(), "Total elements should be 2");
        assertEquals(1, result.getTotalPages(), "Total pages should be 1");
        assertEquals(0, result.getNumber(), "Page number should be 0");
        assertEquals(10, result.getSize(), "Page size should be 10");
    }

    @Test
    @DisplayName("Find Parent Tasks by User ID and Search Term returns Matching Tasks")
    void findParentTasks_withSearch_successfulFind() {
        User user1 = setupUser("testuser");
        User savedUser1 = userRepository.save(user1);

        Task parentTask1 = setupTask("Task One", new HashSet<>(), savedUser1, Status.TODO);
        Task parentTask2 = setupTask("Task Two", new HashSet<>(), savedUser1, Status.TODO);
        Task nonMatchingTask = setupTask("Another Task", new HashSet<>(), savedUser1, Status.TODO);

        taskRepository.save(parentTask1);
        taskRepository.save(parentTask2);
        taskRepository.save(nonMatchingTask);

        Task childTask = setupTask("Child Task One", new HashSet<>(), savedUser1, Status.TODO);
        childTask.setParentTask(parentTask1);
        taskRepository.save(childTask);

        User user2 = setupUser("testuser2");
        User savedUser2 = userRepository.save(user2);

        Task user2Task = setupTask("Task One belongs testuser2", new HashSet<>(), savedUser2, Status.TODO);
        taskRepository.save(user2Task);

        Pageable pageable = PageRequest.of(0, 10);
        Page<Task> result = taskRepository.findParentTasks(savedUser1.getId(), "one", pageable);

        assertEquals(1, result.getContent().size(), "Should return 1 parent task for user1");
        assertEquals("Task One", result.getContent().get(0).getTitle(), "Should return Task One");
        assertEquals(savedUser1.getId(), result.getContent().get(0).getOwner().getId(), "Task should belong to user1");
        assertNull(result.getContent().get(0).getParentTask(), "Task should have no parent");
        assertEquals(1, result.getTotalElements(), "Total elements should be 1");
        assertEquals(1, result.getTotalPages(), "Total pages should be 1");
        assertEquals(0, result.getNumber(), "Page number should be 0");
        assertEquals(10, result.getSize(), "Page size should be 10");

        result = taskRepository.findParentTasks(savedUser1.getId(), "ONE", pageable);
        assertEquals(1, result.getContent().size(), "Should return 1 parent task for user1 with uppercase search");
        assertEquals("Task One", result.getContent().get(0).getTitle(), "Should return Task One with uppercase search");
    }

    @Test
    @DisplayName("Find Parent Tasks with User ID is NULL returns all Tasks")
    void findParentTasks_withNullUserId_successfulFind() {
        User user1 = setupUser("testuser");
        User savedUser1 = userRepository.save(user1);

        Task parentTask1 = setupTask("Task One", new HashSet<>(), savedUser1, Status.TODO);
        Task parentTask2 = setupTask("Task Two", new HashSet<>(), savedUser1, Status.TODO);

        taskRepository.save(parentTask1);
        taskRepository.save(parentTask2);

        Task childTask = setupTask("Child Task One", new HashSet<>(), savedUser1, Status.TODO);
        childTask.setParentTask(parentTask1);
        taskRepository.save(childTask);

        User user2 = setupUser("testuser2");
        User savedUser2 = userRepository.save(user2);

        Task user2Task = setupTask("Task One belongs testuser2", new HashSet<>(), savedUser2, Status.TODO);
        taskRepository.save(user2Task);

        Pageable pageable = PageRequest.of(0, 10);
        Page<Task> result = taskRepository.findParentTasks(null, null, pageable);

        assertEquals(3, result.getContent().size(), "Should return 3 parent tasks");
        List<String> taskTitles = result.getContent().stream()
                .map(Task::getTitle)
                .collect(Collectors.toList());
        assertTrue(taskTitles.contains("Task One"), "Should contain Task One");
        assertTrue(taskTitles.contains("Task Two"), "Should contain Task Two");
        assertTrue(taskTitles.contains("Task One belongs testuser2"), "Should contain Task One belongs testuser2");
        result.getContent().forEach(task
                -> assertNull(task.getParentTask(), "Task should have no parent"));
        assertEquals(3, result.getTotalElements(), "Total elements should be 3");
        assertEquals(1, result.getTotalPages(), "Total pages should be 1");
        assertEquals(0, result.getNumber(), "Page number should be 0");
        assertEquals(10, result.getSize(), "Page size should be 10");
    }

    @Test
    @DisplayName("Find Parent Tasks with User ID is NULL and Search Term returns Matching Tasks")
    void findParentTasks_withNullUserIdAndSearch_successfulFind() {
        User user1 = setupUser("testuser");
        User savedUser1 = userRepository.save(user1);

        Task parentTask1 = setupTask("Task One", new HashSet<>(), savedUser1, Status.TODO);
        Task parentTask2 = setupTask("Task Two", new HashSet<>(), savedUser1, Status.TODO);
        Task nonMatchingTask = setupTask("Another Task", new HashSet<>(), savedUser1, Status.TODO);
        Task savedParentTask1 = taskRepository.save(parentTask1);
        taskRepository.save(parentTask2);
        taskRepository.save(nonMatchingTask);

        Task childTask = setupTask("Child Task", new HashSet<>(), savedUser1, Status.TODO);
        childTask.setParentTask(savedParentTask1);
        taskRepository.save(childTask);

        User user2 = setupUser("testuser2");
        User savedUser2 = userRepository.save(user2);
        Task user2Task = setupTask("Task One belongs testuser2", new HashSet<>(), savedUser2, Status.TODO);
        taskRepository.save(user2Task);

        Pageable pageable = PageRequest.of(0, 10);
        Page<Task> result = taskRepository.findParentTasks(null, "one", pageable);

        assertEquals(2, result.getContent().size(), "Should return 2 parent tasks");
        List<String> taskTitles = result.getContent().stream()
                .map(Task::getTitle)
                .collect(Collectors.toList());
        assertTrue(taskTitles.contains("Task One"), "Should contain Task One");
        assertTrue(taskTitles.contains("Task One belongs testuser2"), "Should contain Task One belongs testuser2");
        result.getContent().forEach(task
                -> assertNull(task.getParentTask(), "Task should have no parent"));
        assertEquals(2, result.getTotalElements(), "Total elements should be 2");
        assertEquals(1, result.getTotalPages(), "Total pages should be 1");
        assertEquals(0, result.getNumber(), "Page number should be 0");
        assertEquals(10, result.getSize(), "Page size should be 10");

        result = taskRepository.findParentTasks(null, "ONE", pageable);
        assertEquals(2, result.getContent().size(), "Should return 2 parent tasks with uppercase search");
        assertTrue(result.getContent().stream()
                .map(Task::getTitle)
                .collect(Collectors.toList())
                .contains("Task One"), "Should contain Task One with uppercase search");
        assertTrue(result.getContent().stream()
                .map(Task::getTitle)
                .collect(Collectors.toList())
                .contains("Task One belongs testuser2"), "Should contain Task One belongs testuser2 with uppercase search");
    }

    @Test
    @DisplayName("Find Parent Tasks when No Parent Tasks Exist returns Empty Page")
    void findParentTasks_noParentTasksExist_returnsEmptyPage() {
        User user1 = setupUser("testuser");
        User savedUser1 = userRepository.save(user1);

        Task task1 = setupTask("Task 1", new HashSet<>(), savedUser1, Status.TODO);
        Task task2 = setupTask("Task 2", new HashSet<>(), savedUser1, Status.TODO);
        Task savedTask1 = taskRepository.save(task1);
        Task savedTask2 = taskRepository.save(task2);

        savedTask1.setParentTask(savedTask2);
        savedTask2.setParentTask(savedTask1);
        taskRepository.save(savedTask1);
        taskRepository.save(savedTask2);

        Pageable pageable = PageRequest.of(0, 10);

        Page<Task> result = taskRepository.findParentTasks(null, null, pageable);
        assertTrue(result.getContent().isEmpty(), "Should return no parent tasks");
        assertEquals(0, result.getTotalElements(), "Total elements should be 0");
        assertEquals(0, result.getTotalPages(), "Total pages should be 0");
        assertEquals(0, result.getNumber(), "Page number should be 0");
        assertEquals(10, result.getSize(), "Page size should be 10");

        result = taskRepository.findParentTasks(savedUser1.getId(), null, pageable);
        assertTrue(result.getContent().isEmpty(), "Should return no parent tasks for user1");
        assertEquals(0, result.getTotalElements(), "Total elements should be 0");
        assertEquals(0, result.getTotalPages(), "Total pages should be 0");

        result = taskRepository.findParentTasks(null, "task", pageable);
        assertTrue(result.getContent().isEmpty(), "Should return no parent tasks with search");
        assertEquals(0, result.getTotalElements(), "Total elements should be 0");
        assertEquals(0, result.getTotalPages(), "Total pages should be 0");
    }

    @Test
    @DisplayName("Find Parent Tasks with More Than 10 Tasks Returns Two Pages")
    void findParentTasks_paginationTwoPages() {
        User user1 = setupUser("testuser");
        User savedUser1 = userRepository.save(user1);
        User user2 = setupUser("testuser2");
        User savedUser2 = userRepository.save(user2);

        for (int i = 0; i < 15; i++) {
            Task task = setupTask("Task " + i, new HashSet<>(), savedUser1, Status.TODO);
            taskRepository.save(task);
        }

        Task parentTask = setupTask("Parent Task", new HashSet<>(), savedUser1, Status.TODO);
        Task savedParentTask = taskRepository.save(parentTask);
        Task childTask = setupTask("Child Task", new HashSet<>(), savedUser1, Status.TODO);
        childTask.setParentTask(savedParentTask);
        taskRepository.save(childTask);

        Task user2Task = setupTask("User2 Task", new HashSet<>(), savedUser2, Status.TODO);
        taskRepository.save(user2Task);

        Pageable pageable = PageRequest.of(0, 10, Sort.by("id"));
        Page<Task> result = taskRepository.findParentTasks(savedUser1.getId(), null, pageable);

        assertEquals(10, result.getContent().size(), "First page should contain 10 tasks");
        result.getContent().forEach(task
                -> assertNull(task.getParentTask(), "Task should have no parent"));
        result.getContent().forEach(task
                -> assertEquals(savedUser1.getId(), task.getOwner().getId(), "Task should belong to saved User"));
        List<String> taskTitles = result.getContent().stream()
                .map(Task::getTitle)
                .collect(Collectors.toList());

        // Expected titles: Task 0, Task 1, Task 2, Task 3, Task 4, Task 5, Task 6, Task 7, Task 8, Task 9
        for (int i = 0; i < 10; i++) {
            assertTrue(taskTitles.contains("Task " + i), "First page should contain Task " + i);
        }

        assertEquals(16, result.getTotalElements(), "Total elements should be 16");
        assertEquals(2, result.getTotalPages(), "Total pages should be 2");
        assertEquals(0, result.getNumber(), "Page number should be 0");
        assertEquals(10, result.getSize(), "Page size should be 10");

        pageable = PageRequest.of(1, 10, Sort.by("id"));
        result = taskRepository.findParentTasks(savedUser1.getId(), null, pageable);

        assertEquals(6, result.getContent().size(), "Second page should contain 6 tasks");
        result.getContent().forEach(task
                -> assertNull(task.getParentTask(), "Task should have no parent"));
        result.getContent().forEach(task
                -> assertEquals(savedUser1.getId(), task.getOwner().getId(), "Task should belong to saved User"));
        taskTitles = result.getContent().stream()
                .map(Task::getTitle)
                .collect(Collectors.toList());

        // Expected titles: Task 10, Task 11, Task 12, Task 13, Task 14, Parent Task
        List<String> expectedSecondPageTitles = List.of(
                "Task 10", "Task 11", "Task 12", "Task 13", "Task 14", "Parent Task"
        );
        for (String title : expectedSecondPageTitles) {
            assertTrue(taskTitles.contains(title), "Second page should contain " + title);
        }

        assertEquals(16, result.getTotalElements(), "Total elements should be 16");
        assertEquals(2, result.getTotalPages(), "Total pages should be 2");
        assertEquals(1, result.getNumber(), "Page number should be 1");
        assertEquals(10, result.getSize(), "Page size should be 10");
    }

    @Test
    @DisplayName("Find Task by Owner returns Matching Tasks")
    void findByOwner_successfulFind() {
        User user1 = setupUser("testuser");
        User savedUser1 = userRepository.save(user1);
        User user2 = setupUser("testuser2");
        User savedUser2 = userRepository.save(user2);
        User user3 = setupUser("testuser3");
        User savedUser3 = userRepository.save(user3);

        Task parentTask1 = setupTask("Task One", new HashSet<>(), savedUser1, Status.TODO);
        Task parentTask2 = setupTask("Task Two", new HashSet<>(), savedUser1, Status.IN_PROGRESS);
        Task savedParentTask1 = taskRepository.save(parentTask1);
        taskRepository.save(parentTask2);

        Task childTask = setupTask("Child Task", new HashSet<>(), savedUser1, Status.DONE);
        childTask.setParentTask(savedParentTask1);
        taskRepository.save(childTask);

        Task user2Task = setupTask("Task One belongs testuser2", new HashSet<>(), savedUser2, Status.TODO);
        taskRepository.save(user2Task);

        List<Task> result = taskRepository.findByOwner(savedUser1);
        assertEquals(3, result.size(), "Should return 3 tasks");
        result.forEach(task
                -> assertEquals(savedUser1.getId(), task.getOwner().getId(),
                        "Task '" + task.getTitle() + "' should belong to user1"));
        List<String> taskTitles = result.stream()
                .map(Task::getTitle)
                .collect(Collectors.toList());
        assertTrue(taskTitles.contains("Task One"), "Should contain Task One");
        assertTrue(taskTitles.contains("Task Two"), "Should contain Task Two");
        assertTrue(taskTitles.contains("Child Task"), "Should contain Child Task");

        result = taskRepository.findByOwner(savedUser3);
        assertTrue(result.isEmpty(), "Should return no tasks for user3");
    }

    @Test
    @DisplayName("Find Tasks by Owner Returns Empty List When No Tasks Exist")
    void findByOwner_noTasksReturnsEmptyList() {
        User user1 = setupUser("testuser");
        User savedUser1 = userRepository.save(user1);
        User user2 = setupUser("testuser2");
        User savedUser2 = userRepository.save(user2);
        User user3 = setupUser("testuser3");
        User savedUser3 = userRepository.save(user3);

        Task parentTask1 = setupTask("Task One", new HashSet<>(), savedUser1, Status.TODO);
        Task parentTask2 = setupTask("Task Two", new HashSet<>(), savedUser1, Status.IN_PROGRESS);
        Task savedParentTask1 = taskRepository.save(parentTask1);
        taskRepository.save(parentTask2);

        Task childTask = setupTask("Child Task", new HashSet<>(), savedUser1, Status.DONE);
        childTask.setParentTask(savedParentTask1);
        taskRepository.save(childTask);

        List<Task> result = taskRepository.findByOwner(savedUser2);
        assertTrue(result.isEmpty(), "Should return no tasks for user2");

        result = taskRepository.findByOwner(savedUser3);
        assertTrue(result.isEmpty(), "Should return no tasks for user3");
    }

    @Test
    @DisplayName("Find Tasks by Owner Returns Matching Tasks for Multiple Owners")
    void findByOwner_multipleOwners_returnsMatchingTasks() {
        User user1 = setupUser("testuser");
        User savedUser1 = userRepository.save(user1);
        User user2 = setupUser("testuser2");
        User savedUser2 = userRepository.save(user2);
        User user3 = setupUser("testuser3");
        User savedUser3 = userRepository.save(user3);

        Task parentTask1 = setupTask("Task One", new HashSet<>(), savedUser1, Status.TODO);
        Task savedParentTask1 = taskRepository.save(parentTask1);

        Task childTask = setupTask("Child Task", new HashSet<>(), savedUser1, Status.DONE);
        childTask.setParentTask(savedParentTask1);
        taskRepository.save(childTask);

        Task parentTask2 = setupTask("Task Two", new HashSet<>(), savedUser2, Status.IN_PROGRESS);
        Task savedParentTask2 = taskRepository.save(parentTask2);

        Task childTask2 = setupTask("Child Task Two", new HashSet<>(), savedUser2, Status.DONE);
        childTask2.setParentTask(savedParentTask2);
        taskRepository.save(childTask2);

        List<Task> result = taskRepository.findByOwner(savedUser1);
        assertEquals(2, result.size(), "Should return 2 tasks for user1");
        result.forEach(task
                -> assertEquals(savedUser1.getId(), task.getOwner().getId(),
                        "Task '" + task.getTitle() + "' should belong to user1"));
        List<String> taskTitles = result.stream()
                .map(Task::getTitle)
                .collect(Collectors.toList());
        assertTrue(taskTitles.contains("Task One"), "Should contain Task One");
        assertTrue(taskTitles.contains("Child Task"), "Should contain Child Task");

        result = taskRepository.findByOwner(savedUser2);
        assertEquals(2, result.size(), "Should return 2 tasks");
        result.forEach(task
                -> assertEquals(savedUser2.getId(), task.getOwner().getId(),
                        "Task '" + task.getTitle() + "' should belong to user2"));
        taskTitles = result.stream()
                .map(Task::getTitle)
                .collect(Collectors.toList());
        assertTrue(taskTitles.contains("Task Two"), "Should contain Task Two");
        assertTrue(taskTitles.contains("Child Task Two"), "Should contain Child Task Two");

        result = taskRepository.findByOwner(savedUser3);
        assertTrue(result.isEmpty(), "Should return no tasks for user3");
    }

    @Test
    @DisplayName("Find By Parent Task ID with valid data returns Matching Parent Task(s)")
    void findByParentTaskId_validData_returnsMatchingTask() {
        User user = setupUser("testuser");
        User savedUser = userRepository.save(user);

        Task parentTask = setupTask("Task One", new HashSet<>(), savedUser, Status.TODO);
        Task savedParentTask = taskRepository.save(parentTask);

        Task childTask1 = setupTask("Child Task One", new HashSet<>(), savedUser, Status.IN_PROGRESS);
        childTask1.setParentTask(savedParentTask);
        taskRepository.save(childTask1);

        Task childTask2 = setupTask("Child Task Two", new HashSet<>(), savedUser, Status.DONE);
        childTask2.setParentTask(savedParentTask);
        taskRepository.save(childTask2);

        List<Task> result = taskRepository.findByParentTaskId(savedParentTask.getId());
        assertEquals(2, result.size(), "Should return 2 child tasks for user");

        result.forEach(childTask
                -> assertEquals(savedParentTask.getId(), childTask.getParentTask().getId(),
                        "Task '" + childTask.getTitle() + "' should belong to Parent Task Task One"));
        List<String> taskTitles = result.stream()
                .map(Task::getTitle)
                .collect(Collectors.toList());
        assertTrue(taskTitles.contains("Child Task One"), "Should contain Child Task One");
        assertTrue(taskTitles.contains("Child Task Two"), "Should contain Child Task Two");
    }

    @Test
    @DisplayName("Find by Parent Task ID but there are no Child Task(s) associated with the provided Parent Task ID returns empty List")
    void findByParentTaskId_noChildTasks_returnsEmptyList() {
        User user = setupUser("testuser");
        User savedUser = userRepository.save(user);

        Task parentTask1 = setupTask("Task One", new HashSet<>(), savedUser, Status.TODO);
        Task savedParentTask1 = taskRepository.save(parentTask1);

        Task parentTask2 = setupTask("Task Two", new HashSet<>(), savedUser, Status.IN_PROGRESS);
        Task savedParentTask2 = taskRepository.save(parentTask2);
        Task childTask = setupTask("Child Task", new HashSet<>(), savedUser, Status.DONE);
        childTask.setParentTask(savedParentTask2);
        taskRepository.save(childTask);

        List<Task> result = taskRepository.findByParentTaskId(savedParentTask1.getId());
        assertTrue(result.isEmpty(), "Should return no child tasks for parent task 'Task One'");
    }

    @Test
    @DisplayName("Find Subtasks by Non-Existent Parent Task ID Returns Empty List")
    void findByParentTaskId_nonExistentParentTaskId_returnsEmptyList() {
        List<Task> result = taskRepository.findByParentTaskId(Long.MAX_VALUE);
        assertTrue(result.isEmpty(), "Should return no child tasks for non-existent parent task ID");
    }

    @Test
    @DisplayName("Check if Task Title is Unique returns TRUE for Unique Title")
    void isTitleUnique_uniqueTitle_returnsTrue() {
        User user = setupUser("testuser");
        User savedUser = userRepository.save(user);

        Task task = setupTask("Task One", new HashSet<>(), savedUser, Status.TODO);
        taskRepository.save(task);

        boolean result = taskRepository.isTitleUnique("Task Two", savedUser.getId(), null);
        assertTrue(result, "Should return true for unique title");
    }

    @Test
    @DisplayName("Check if Task Title is Unique returns FALSE for NOT Unique Title")
    void isTitleUnique_notUniqueTitle_returnsFalse() {
        User user = setupUser("testuser");
        User savedUser = userRepository.save(user);

        Task task = setupTask("Task One", new HashSet<>(), savedUser, Status.TODO);
        taskRepository.save(task);

        boolean result = taskRepository.isTitleUnique("Task One", savedUser.getId(), null);
        assertFalse(result, "Should return false for non-unique title");
    }

    @Test
    @DisplayName("Check if Task Title is Unique returns TRUE excluding Self")
    void isTitleUnique_excludingSelf_returnsTrue() {
        User user = setupUser("testuser");
        User savedUser = userRepository.save(user);

        Task task = setupTask("Task One", new HashSet<>(), savedUser, Status.IN_PROGRESS);
        Task savedTask = taskRepository.save(task);

        boolean result = taskRepository.isTitleUnique("Task One", savedUser.getId(), savedTask.getId());
        assertTrue(result, "Should return true when excluding task's own ID");
    }

    @Test
    @DisplayName("Check if Task Title for Same Title with Different User returns TRUE")
    void isTitleUnique_differentTitle_returnsTrue() {
        User user1 = setupUser("testuser1");
        User savedUser1 = userRepository.save(user1);
        User user2 = setupUser("testuser2");
        User savedUser2 = userRepository.save(user2);

        Task task = setupTask("Task One", new HashSet<>(), savedUser1, Status.TODO);
        taskRepository.save(task);

        boolean result = taskRepository.isTitleUnique("Task One", savedUser2.getId(), null);
        assertTrue(result, "Should return true for same title with different user");
    }
}
