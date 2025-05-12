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
import org.junit.jupiter.api.BeforeEach;
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

    private static final String USERNAME_1 = "testuser";
    private static final String USERNAME_2 = "testuser2";
    private static final String USERNAME_3 = "testuser3";
    private static final String TASK_TITLE = "Task Title";
    private static final String TASK_TITLE_ONE = "Task One";
    private static final String TASK_TITLE_TWO = "Task Two";
    private static final String PASSWORD = "Password123!";
    private static final String EMAIL_DOMAIN = "@example.com";
    private static final String CATEGORY_NAME = "Category A";
    private static final int PAGE_SIZE = 10;
    private static final String SEARCH_TERM = "one";
    private static final String NON_MATCHING_TITLE = "Another Task";
    private static final String USER2_TASK_TITLE = "Task One belongs testuser2";
    private static final String CHILD_TASK_TITLE = "Child Task";

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Pageable defaultPageable;

    @BeforeEach
    @SuppressWarnings("unused")
    void setup() {
        defaultPageable = PageRequest.of(0, PAGE_SIZE);
    }

    private User setupUser(String username) {
        User user = new User(username, PASSWORD, null);
        user.setEmail(username + EMAIL_DOMAIN);
        return user;
    }

    private Category setupCategory(String categoryName) {
        Category category = new Category(categoryName);
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());
        return category;
    }

    private Task setupTask(String title, Set<Category> categories, User owner, Status status) {
        Task task = new Task(null, null, owner, title, "Task Description", status, LocalDateTime.now(), LocalDateTime.now(), null);
        task.setCategories(categories);
        return task;
    }

    private void assertTaskTitlesContain(List<Task> tasks, List<String> expectedTitles) {
        List<String> actualTitles = tasks.stream().map(Task::getTitle).collect(Collectors.toList());
        expectedTitles.forEach(title -> assertTrue(actualTitles.contains(title), "Should contain " + title));
    }

    private void assertPageMetadata(Page<?> page, int expectedContentSize, long expectedTotalElements, int expectedTotalPages, int expectedPageNumber) {
        assertEquals(expectedContentSize, page.getContent().size(), "Page content size should match");
        assertEquals(expectedTotalElements, page.getTotalElements(), "Total elements should match");
        assertEquals(expectedTotalPages, page.getTotalPages(), "Total pages should match");
        assertEquals(expectedPageNumber, page.getNumber(), "Page number should match");
        assertEquals(PAGE_SIZE, page.getSize(), "Page size should be " + PAGE_SIZE);
    }

    @Test
    @DisplayName("Save Task with valid data persists and returns Task")
    void saveTask_validData_successfulSave() {
        // Arrange
        User user = setupUser(USERNAME_1);
        User savedUser = userRepository.save(user);
        Category category = setupCategory(CATEGORY_NAME);
        Category savedCategory = categoryRepository.save(category);
        Set<Category> categories = new HashSet<>(Set.of(savedCategory));
        Task task = setupTask(TASK_TITLE, categories, savedUser, Status.TODO);

        // Act
        Task savedTask = taskRepository.save(task);

        // Assert
        assertNotNull(savedTask.getId(), "Task ID should be generated");
        assertEquals(TASK_TITLE, savedTask.getTitle(), "Title should match");
        assertEquals(savedUser.getId(), savedTask.getOwner().getId(), "Owner ID should match");
        assertEquals(Status.TODO, savedTask.getStatus(), "Status should match");
        assertEquals(1, savedTask.getCategories().size(), "Should have one category");
        assertTrue(savedTask.getCategories().contains(savedCategory), "Should contain Category A");
        assertNotNull(savedTask.getCreatedAt(), "Created timestamp should be set");
        assertNotNull(savedTask.getUpdatedAt(), "Updated timestamp should be set");
    }

    

    @Test
    @DisplayName("Save Task with null title throws DataIntegrityViolationException")
    void saveTask_nullTitle_throwsException() {
        // Arrange
        User user = setupUser(USERNAME_1);
        User savedUser = userRepository.save(user);
        Task task = setupTask(null, new HashSet<>(), savedUser, Status.TODO);

        // Act & Assert
        try {
            taskRepository.saveAndFlush(task);
            fail("Expected DataIntegrityViolationException but none was thrown");
        } catch (DataIntegrityViolationException e) {
            assertTrue(true);
        }
    }

    @Test
    @DisplayName("Save Task with null owner throws DataIntegrityViolationException")
    void saveTask_nullOwner_throwsException() {
        // Arrange
        Task task = setupTask(TASK_TITLE, new HashSet<>(), null, Status.TODO);

        // Act & Assert
        try {
            taskRepository.saveAndFlush(task);
            fail("Expected DataIntegrityViolationException but none was thrown");
        } catch (DataIntegrityViolationException e) {
            assertTrue(true);
        }
    }

    @Test
    @DisplayName("Save Task with null status throws DataIntegrityViolationException")
    void saveTask_nullStatus_throwsException() {
        // Arrange
        User user = setupUser(USERNAME_1);
        User savedUser = userRepository.save(user);
        Task task = setupTask(TASK_TITLE, new HashSet<>(), savedUser, null);

        // Act & Assert
        try {
            taskRepository.saveAndFlush(task);
            fail("Expected DataIntegrityViolationException but none was thrown");
        } catch (DataIntegrityViolationException e) {
            assertTrue(true);
        }
    }

    @Test
    @DisplayName("Save Task with parent task persists and returns Task")
    void saveTask_setParentTask_successfulSave() {
        // Arrange
        User user = setupUser(USERNAME_1);
        User savedUser = userRepository.save(user);
        Task parentTask = setupTask("Parent Task Title", new HashSet<>(), savedUser, Status.TODO);
        Task savedParentTask = taskRepository.save(parentTask);
        Task childTask = setupTask(TASK_TITLE, new HashSet<>(), savedUser, Status.TODO);
        childTask.setParentTask(savedParentTask);

        // Act
        Task savedChildTask = taskRepository.save(childTask);

        // Assert
        assertNotNull(savedChildTask.getId(), "Task ID should be generated");
        assertEquals(TASK_TITLE, savedChildTask.getTitle(), "Title should match");
        assertEquals(savedUser.getId(), savedChildTask.getOwner().getId(), "Owner ID should match");
        assertEquals(Status.TODO, savedChildTask.getStatus(), "Status should match");
        assertEquals(savedParentTask.getId(), savedChildTask.getParentTask().getId(), "Parent task ID should match");
        assertNotNull(savedChildTask.getCreatedAt(), "Created timestamp should be set");
        assertNotNull(savedChildTask.getUpdatedAt(), "Updated timestamp should be set");
    }

    @Test
    @DisplayName("Find Parent Tasks by User ID returns Tasks")
    void findParentTasks_byUserId_returnsTasks() {
        // Arrange
        User user1 = setupUser(USERNAME_1);
        User savedUser1 = userRepository.save(user1);
        User user2 = setupUser(USERNAME_2);
        User savedUser2 = userRepository.save(user2);
        Task task1 = setupTask(TASK_TITLE_ONE, new HashSet<>(), savedUser1, Status.TODO);
        Task task2 = setupTask(TASK_TITLE_TWO, new HashSet<>(), savedUser1, Status.TODO);
        Task savedTask1 = taskRepository.save(task1);
        taskRepository.save(task2);
        Task user2Task = setupTask(USER2_TASK_TITLE, new HashSet<>(), savedUser2, Status.TODO);
        taskRepository.save(user2Task);
        Task childTask = setupTask(CHILD_TASK_TITLE, new HashSet<>(), savedUser1, Status.TODO);
        childTask.setParentTask(savedTask1);
        taskRepository.save(childTask);

        // Act
        Page<Task> result = taskRepository.findParentTasks(savedUser1.getId(), null, defaultPageable);

        // Assert
        assertPageMetadata(result, 2, 2, 1, 0);
        assertTaskTitlesContain(result.getContent(), List.of(TASK_TITLE_ONE, TASK_TITLE_TWO));
        result.getContent().forEach(task -> {
            assertEquals(savedUser1.getId(), task.getOwner().getId(), "Task should belong to user1");
            assertNull(task.getParentTask(), "Task should have no parent");
        });
    }

    @Test
    @DisplayName("Find Parent Tasks by User ID with search term returns Tasks")
    void findParentTasks_withSearch_returnsTasks() {
        // Arrange
        User user1 = setupUser(USERNAME_1);
        User savedUser1 = userRepository.save(user1);
        User user2 = setupUser(USERNAME_2);
        User savedUser2 = userRepository.save(user2);
        Task task1 = setupTask(TASK_TITLE_ONE, new HashSet<>(), savedUser1, Status.TODO);
        Task task2 = setupTask(TASK_TITLE_TWO, new HashSet<>(), savedUser1, Status.TODO);
        Task nonMatchingTask = setupTask(NON_MATCHING_TITLE, new HashSet<>(), savedUser1, Status.TODO);
        Task savedTask1 = taskRepository.save(task1);
        taskRepository.save(task2);
        taskRepository.save(nonMatchingTask);
        Task childTask = setupTask(CHILD_TASK_TITLE, new HashSet<>(), savedUser1, Status.TODO);
        childTask.setParentTask(savedTask1);
        taskRepository.save(childTask);
        Task user2Task = setupTask(USER2_TASK_TITLE, new HashSet<>(), savedUser2, Status.TODO);
        taskRepository.save(user2Task);

        // Act
        Page<Task> result = taskRepository.findParentTasks(savedUser1.getId(), SEARCH_TERM, defaultPageable);

        // Assert
        assertPageMetadata(result, 1, 1, 1, 0);
        assertTaskTitlesContain(result.getContent(), List.of(TASK_TITLE_ONE));
        result.getContent().forEach(task -> {
            assertEquals(savedUser1.getId(), task.getOwner().getId(), "Task should belong to user1");
            assertNull(task.getParentTask(), "Task should have no parent");
        });

        // Act (uppercase search)
        result = taskRepository.findParentTasks(savedUser1.getId(), SEARCH_TERM.toUpperCase(), defaultPageable);

        // Assert (uppercase search)
        assertPageMetadata(result, 1, 1, 1, 0);
        assertTaskTitlesContain(result.getContent(), List.of(TASK_TITLE_ONE));
    }

    @Test
    @DisplayName("Find Parent Tasks with null User ID returns all Tasks")
    void findParentTasks_withNullUserId_returnsTasks() {
        // Arrange
        User user1 = setupUser(USERNAME_1);
        User savedUser1 = userRepository.save(user1);
        User user2 = setupUser(USERNAME_2);
        User savedUser2 = userRepository.save(user2);
        Task task1 = setupTask(TASK_TITLE_ONE, new HashSet<>(), savedUser1, Status.TODO);
        Task task2 = setupTask(TASK_TITLE_TWO, new HashSet<>(), savedUser1, Status.TODO);
        Task savedTask1 = taskRepository.save(task1);
        taskRepository.save(task2);
        Task childTask = setupTask(CHILD_TASK_TITLE, new HashSet<>(), savedUser1, Status.TODO);
        childTask.setParentTask(savedTask1);
        taskRepository.save(childTask);
        Task user2Task = setupTask(USER2_TASK_TITLE, new HashSet<>(), savedUser2, Status.TODO);
        taskRepository.save(user2Task);

        // Act
        Page<Task> result = taskRepository.findParentTasks(null, null, defaultPageable);

        // Assert
        assertPageMetadata(result, 3, 3, 1, 0);
        assertTaskTitlesContain(result.getContent(), List.of(TASK_TITLE_ONE, TASK_TITLE_TWO, USER2_TASK_TITLE));
        result.getContent().forEach(task -> assertNull(task.getParentTask(), "Task should have no parent"));
    }

    @Test
    @DisplayName("Find Parent Tasks with null User ID and search term returns Tasks")
    void findParentTasks_withNullUserIdAndSearch_returnsTasks() {
        // Arrange
        User user1 = setupUser(USERNAME_1);
        User savedUser1 = userRepository.save(user1);
        User user2 = setupUser(USERNAME_2);
        User savedUser2 = userRepository.save(user2);
        Task task1 = setupTask(TASK_TITLE_ONE, new HashSet<>(), savedUser1, Status.TODO);
        Task task2 = setupTask(TASK_TITLE_TWO, new HashSet<>(), savedUser1, Status.TODO);
        Task nonMatchingTask = setupTask(NON_MATCHING_TITLE, new HashSet<>(), savedUser1, Status.TODO);
        Task savedTask1 = taskRepository.save(task1);
        taskRepository.save(task2);
        taskRepository.save(nonMatchingTask);
        Task childTask = setupTask(CHILD_TASK_TITLE, new HashSet<>(), savedUser1, Status.TODO);
        childTask.setParentTask(savedTask1);
        taskRepository.save(childTask);
        Task user2Task = setupTask(USER2_TASK_TITLE, new HashSet<>(), savedUser2, Status.TODO);
        taskRepository.save(user2Task);

        // Act
        Page<Task> result = taskRepository.findParentTasks(null, SEARCH_TERM, defaultPageable);

        // Assert
        assertPageMetadata(result, 2, 2, 1, 0);
        assertTaskTitlesContain(result.getContent(), List.of(TASK_TITLE_ONE, USER2_TASK_TITLE));
        result.getContent().forEach(task -> assertNull(task.getParentTask(), "Task should have no parent"));

        // Act (uppercase search)
        result = taskRepository.findParentTasks(null, SEARCH_TERM.toUpperCase(), defaultPageable);

        // Assert (uppercase search)
        assertPageMetadata(result, 2, 2, 1, 0);
        assertTaskTitlesContain(result.getContent(), List.of(TASK_TITLE_ONE, USER2_TASK_TITLE));
    }

    @Test
    @DisplayName("Find Parent Tasks when no parent tasks exist returns empty Page")
    void findParentTasks_noParentTasksExist_returnsEmptyPage() {
        // Arrange
        User user1 = setupUser(USERNAME_1);
        User savedUser1 = userRepository.save(user1);
        Task task1 = setupTask("Task 1", new HashSet<>(), savedUser1, Status.TODO);
        Task task2 = setupTask("Task 2", new HashSet<>(), savedUser1, Status.TODO);
        Task savedTask1 = taskRepository.save(task1);
        Task savedTask2 = taskRepository.save(task2);
        savedTask1.setParentTask(savedTask2);
        savedTask2.setParentTask(savedTask1);
        taskRepository.save(savedTask1);
        taskRepository.save(savedTask2);

        // Act
        Page<Task> result = taskRepository.findParentTasks(null, null, defaultPageable);

        // Assert
        assertPageMetadata(result, 0, 0, 0, 0);
        assertTrue(result.getContent().isEmpty(), "Should return no parent tasks");

        // Act (user-specific)
        result = taskRepository.findParentTasks(savedUser1.getId(), null, defaultPageable);

        // Assert (user-specific)
        assertPageMetadata(result, 0, 0, 0, 0);
        assertTrue(result.getContent().isEmpty(), "Should return no parent tasks for user1");

        // Act (with search)
        result = taskRepository.findParentTasks(null, "task", defaultPageable);

        // Assert (with search)
        assertPageMetadata(result, 0, 0, 0, 0);
        assertTrue(result.getContent().isEmpty(), "Should return no parent tasks with search");
    }

    @Test
    @DisplayName("Find Parent Tasks with pagination returns two pages")
    void findParentTasks_paginationTwoPages() {
        // Arrange
        User user1 = setupUser(USERNAME_1);
        User savedUser1 = userRepository.save(user1);
        User user2 = setupUser(USERNAME_2);
        User savedUser2 = userRepository.save(user2);
        for (int i = 0; i < 15; i++) {
            Task task = setupTask("Task " + i, new HashSet<>(), savedUser1, Status.TODO);
            taskRepository.save(task);
        }
        Task parentTask = setupTask("Parent Task", new HashSet<>(), savedUser1, Status.TODO);
        Task savedParentTask = taskRepository.save(parentTask);
        Task childTask = setupTask(CHILD_TASK_TITLE, new HashSet<>(), savedUser1, Status.TODO);
        childTask.setParentTask(savedParentTask);
        taskRepository.save(childTask);
        Task user2Task = setupTask("User2 Task", new HashSet<>(), savedUser2, Status.TODO);
        taskRepository.save(user2Task);

        // Act (first page)
        Pageable pageable = PageRequest.of(0, PAGE_SIZE, Sort.by("id"));
        Page<Task> result = taskRepository.findParentTasks(savedUser1.getId(), null, pageable);

        // Assert (first page)
        assertPageMetadata(result, 10, 16, 2, 0);
        result.getContent().forEach(task -> {
            assertNull(task.getParentTask(), "Task should have no parent");
            assertEquals(savedUser1.getId(), task.getOwner().getId(), "Task should belong to user1");
        });
        List<String> expectedFirstPageTitles = List.of("Task 0", "Task 1", "Task 2", "Task 3", "Task 4", "Task 5", "Task 6", "Task 7", "Task 8", "Task 9");
        assertTaskTitlesContain(result.getContent(), expectedFirstPageTitles);

        // Act (second page)
        pageable = PageRequest.of(1, PAGE_SIZE, Sort.by("id"));
        result = taskRepository.findParentTasks(savedUser1.getId(), null, pageable);

        // Assert (second page)
        assertPageMetadata(result, 6, 16, 2, 1);
        result.getContent().forEach(task -> {
            assertNull(task.getParentTask(), "Task should have no parent");
            assertEquals(savedUser1.getId(), task.getOwner().getId(), "Task should belong to user1");
        });
        List<String> expectedSecondPageTitles = List.of("Task 10", "Task 11", "Task 12", "Task 13", "Task 14", "Parent Task");
        assertTaskTitlesContain(result.getContent(), expectedSecondPageTitles);
    }

    @Test
    @DisplayName("Find Tasks by Owner returns Tasks")
    void findByOwner_returnsTasks() {
        // Arrange
        User user1 = setupUser(USERNAME_1);
        User savedUser1 = userRepository.save(user1);
        User user2 = setupUser(USERNAME_2);
        User savedUser2 = userRepository.save(user2);
        User user3 = setupUser(USERNAME_3);
        User savedUser3 = userRepository.save(user3);
        Task task1 = setupTask(TASK_TITLE_ONE, new HashSet<>(), savedUser1, Status.TODO);
        Task task2 = setupTask(TASK_TITLE_TWO, new HashSet<>(), savedUser1, Status.IN_PROGRESS);
        Task savedTask1 = taskRepository.save(task1);
        taskRepository.save(task2);
        Task childTask = setupTask(CHILD_TASK_TITLE, new HashSet<>(), savedUser1, Status.DONE);
        childTask.setParentTask(savedTask1);
        taskRepository.save(childTask);
        Task user2Task = setupTask(USER2_TASK_TITLE, new HashSet<>(), savedUser2, Status.TODO);
        taskRepository.save(user2Task);

        // Act
        List<Task> result = taskRepository.findByOwner(savedUser1);

        // Assert
        assertEquals(3, result.size(), "Should return 3 tasks for user1");
        result.forEach(task -> assertEquals(savedUser1.getId(), task.getOwner().getId(), "Task should belong to user1"));
        assertTaskTitlesContain(result, List.of(TASK_TITLE_ONE, TASK_TITLE_TWO, CHILD_TASK_TITLE));

        // Act (empty result)
        result = taskRepository.findByOwner(savedUser3);

        // Assert (empty result)
        assertTrue(result.isEmpty(), "Should return no tasks for user3");
    }

    @Test
    @DisplayName("Find Tasks by Owner with no tasks returns empty List")
    void findByOwner_noTasks_returnsEmptyList() {
        // Arrange
        User user1 = setupUser(USERNAME_1);
        User savedUser1 = userRepository.save(user1);
        User user2 = setupUser(USERNAME_2);
        User savedUser2 = userRepository.save(user2);
        User user3 = setupUser(USERNAME_3);
        User savedUser3 = userRepository.save(user3);
        Task task1 = setupTask(TASK_TITLE_ONE, new HashSet<>(), savedUser1, Status.TODO);
        Task task2 = setupTask(TASK_TITLE_TWO, new HashSet<>(), savedUser1, Status.IN_PROGRESS);
        Task savedTask1 = taskRepository.save(task1);
        taskRepository.save(task2);
        Task childTask = setupTask(CHILD_TASK_TITLE, new HashSet<>(), savedUser1, Status.DONE);
        childTask.setParentTask(savedTask1);
        taskRepository.save(childTask);

        // Act
        List<Task> result = taskRepository.findByOwner(savedUser2);

        // Assert
        assertTrue(result.isEmpty(), "Should return no tasks for user2");

        // Act
        result = taskRepository.findByOwner(savedUser3);

        // Assert
        assertTrue(result.isEmpty(), "Should return no tasks for user3");
    }

    @Test
    @DisplayName("Find Tasks by Owner for multiple owners returns Tasks")
    void findByOwner_multipleOwners_returnsTasks() {
        // Arrange
        User user1 = setupUser(USERNAME_1);
        User savedUser1 = userRepository.save(user1);
        User user2 = setupUser(USERNAME_2);
        User savedUser2 = userRepository.save(user2);
        User user3 = setupUser(USERNAME_3);
        User savedUser3 = userRepository.save(user3);
        Task task1 = setupTask(TASK_TITLE_ONE, new HashSet<>(), savedUser1, Status.TODO);
        Task savedTask1 = taskRepository.save(task1);
        Task childTask1 = setupTask(CHILD_TASK_TITLE, new HashSet<>(), savedUser1, Status.DONE);
        childTask1.setParentTask(savedTask1);
        taskRepository.save(childTask1);
        Task task2 = setupTask(TASK_TITLE_TWO, new HashSet<>(), savedUser2, Status.IN_PROGRESS);
        Task savedTask2 = taskRepository.save(task2);
        Task childTask2 = setupTask("Child Task Two", new HashSet<>(), savedUser2, Status.DONE);
        childTask2.setParentTask(savedTask2);
        taskRepository.save(childTask2);

        // Act
        List<Task> result = taskRepository.findByOwner(savedUser1);

        // Assert
        assertEquals(2, result.size(), "Should return 2 tasks for user1");
        result.forEach(task -> assertEquals(savedUser1.getId(), task.getOwner().getId(), "Task should belong to user1"));
        assertTaskTitlesContain(result, List.of(TASK_TITLE_ONE, CHILD_TASK_TITLE));

        // Act
        result = taskRepository.findByOwner(savedUser2);

        // Assert
        assertEquals(2, result.size(), "Should return 2 tasks for user2");
        result.forEach(task -> assertEquals(savedUser2.getId(), task.getOwner().getId(), "Task should belong to user2"));
        assertTaskTitlesContain(result, List.of(TASK_TITLE_TWO, "Child Task Two"));

        // Act
        result = taskRepository.findByOwner(savedUser3);

        // Assert
        assertTrue(result.isEmpty(), "Should return no tasks for user3");
    }

    @Test
    @DisplayName("Find Subtasks by Parent Task ID returns Tasks")
    void findByParentTaskId_validData_returnsTasks() {
        // Arrange
        User user = setupUser(USERNAME_1);
        User savedUser = userRepository.save(user);
        Task parentTask = setupTask(TASK_TITLE_ONE, new HashSet<>(), savedUser, Status.TODO);
        Task savedParentTask = taskRepository.save(parentTask);
        Task childTask1 = setupTask("Child Task One", new HashSet<>(), savedUser, Status.IN_PROGRESS);
        childTask1.setParentTask(savedParentTask);
        taskRepository.save(childTask1);
        Task childTask2 = setupTask("Child Task Two", new HashSet<>(), savedUser, Status.DONE);
        childTask2.setParentTask(savedParentTask);
        taskRepository.save(childTask2);

        // Act
        List<Task> result = taskRepository.findByParentTaskId(savedParentTask.getId());

        // Assert
        assertEquals(2, result.size(), "Should return 2 child tasks");
        result.forEach(task -> assertEquals(savedParentTask.getId(), task.getParentTask().getId(), "Task should belong to parent"));
        assertTaskTitlesContain(result, List.of("Child Task One", "Child Task Two"));
    }

    @Test
    @DisplayName("Find Subtasks by Parent Task ID with no child tasks returns empty List")
    void findByParentTaskId_noChildTasks_returnsEmptyList() {
        // Arrange
        User user = setupUser(USERNAME_1);
        User savedUser = userRepository.save(user);
        Task parentTask1 = setupTask(TASK_TITLE_ONE, new HashSet<>(), savedUser, Status.TODO);
        Task savedParentTask1 = taskRepository.save(parentTask1);
        Task parentTask2 = setupTask(TASK_TITLE_TWO, new HashSet<>(), savedUser, Status.IN_PROGRESS);
        Task savedParentTask2 = taskRepository.save(parentTask2);
        Task childTask = setupTask(CHILD_TASK_TITLE, new HashSet<>(), savedUser, Status.DONE);
        childTask.setParentTask(savedParentTask2);
        taskRepository.save(childTask);

        // Act
        List<Task> result = taskRepository.findByParentTaskId(savedParentTask1.getId());

        // Assert
        assertTrue(result.isEmpty(), "Should return no child tasks for parent task 'Task One'");
    }

    @Test
    @DisplayName("Find Subtasks by non-existent Parent Task ID returns empty List")
    void findByParentTaskId_nonExistentParentTaskId_returnsEmptyList() {
        // Act & Assert
        List<Task> result = taskRepository.findByParentTaskId(Long.MAX_VALUE);
        assertTrue(result.isEmpty(), "Should return no child tasks for non-existent parent task ID");
    }

    @Test
    @DisplayName("Check if Task Title is unique returns true for unique title")
    void isTitleUnique_uniqueTitle_returnsTrue() {
        // Arrange
        User user = setupUser(USERNAME_1);
        User savedUser = userRepository.save(user);
        Task task = setupTask(TASK_TITLE_ONE, new HashSet<>(), savedUser, Status.TODO);
        taskRepository.save(task);

        // Act
        boolean result = taskRepository.isTitleUnique(TASK_TITLE_TWO, savedUser.getId(), null);

        // Assert
        assertTrue(result, "Should return true for unique title");
    }

    @Test
    @DisplayName("Check if Task Title is unique returns false for non-unique title")
    void isTitleUnique_nonUniqueTitle_returnsFalse() {
        // Arrange
        User user = setupUser(USERNAME_1);
        User savedUser = userRepository.save(user);
        Task task = setupTask(TASK_TITLE_ONE, new HashSet<>(), savedUser, Status.TODO);
        taskRepository.save(task);

        // Act
        boolean result = taskRepository.isTitleUnique(TASK_TITLE_ONE, savedUser.getId(), null);

        // Assert
        assertFalse(result, "Should return false for non-unique title");
    }

    @Test
    @DisplayName("Check if Task Title is unique returns true when excluding self")
    void isTitleUnique_excludingSelf_returnsTrue() {
        // Arrange
        User user = setupUser(USERNAME_1);
        User savedUser = userRepository.save(user);
        Task task = setupTask(TASK_TITLE_ONE, new HashSet<>(), savedUser, Status.IN_PROGRESS);
        Task savedTask = taskRepository.save(task);

        // Act
        boolean result = taskRepository.isTitleUnique(TASK_TITLE_ONE, savedUser.getId(), savedTask.getId());

        // Assert
        assertTrue(result, "Should return true when excluding task's own ID");
    }

    @Test
    @DisplayName("Check if Task Title is unique returns true for same title with different user")
    void isTitleUnique_differentUser_returnsTrue() {
        // Arrange
        User user1 = setupUser(USERNAME_1);
        User savedUser1 = userRepository.save(user1);
        User user2 = setupUser(USERNAME_2);
        User savedUser2 = userRepository.save(user2);
        Task task = setupTask(TASK_TITLE_ONE, new HashSet<>(), savedUser1, Status.TODO);
        taskRepository.save(task);

        // Act
        boolean result = taskRepository.isTitleUnique(TASK_TITLE_ONE, savedUser2.getId(), null);

        // Assert
        assertTrue(result, "Should return true for same title with different user");
    }
}
