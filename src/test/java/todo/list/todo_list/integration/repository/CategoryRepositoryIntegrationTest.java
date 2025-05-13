package todo.list.todo_list.integration.repository;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
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
class CategoryRepositoryIntegrationTest {

    private static final String USERNAME = "testuser";
    private static final String CATEGORY_NAME = "Category A";
    private static final String TASK_TITLE = "Test Task";
    private static final Long NON_EXISTENT_ID = 999L;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    private User defaultUser;

    @BeforeEach
    @SuppressWarnings("unused")
    void setup() {
        defaultUser = setupUser(USERNAME);
        defaultUser = userRepository.save(defaultUser);
    }

    private Category setupCategory(String categoryName) {
        Category category = new Category();
        category.setName(categoryName);
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());
        return category;
    }

    private Task setupTask(String title, Set<Category> categories, User owner, Task parentTask) {
        Task task = new Task(null, owner, Status.TODO);
        task.setTitle(title);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        task.setCategories(categories != null ? categories : new HashSet<>());
        task.setParentTask(parentTask);
        return task;
    }

    private User setupUser(String username) {
        User user = new User(username, "Password123!", null);
        user.setEmail(username + "@example.com");
        return user;
    }

    @Test
    @DisplayName("Save Category with valid data persists and returns Category")
    void saveCategory_validData_successfulSave() {
        // Arrange
        Category category = setupCategory(CATEGORY_NAME);

        // Act
        Category savedCategory = categoryRepository.save(category);

        // Assert
        assertNotNull(savedCategory.getId(), "Category ID should be generated");
        assertEquals(CATEGORY_NAME, savedCategory.getName(), "Category name should match");
        assertNotNull(savedCategory.getCreatedAt(), "Created timestamp should be set");
        assertNotNull(savedCategory.getUpdatedAt(), "Updated timestamp should be set");
    }

    @Test
    @DisplayName("Find Category by Name when Category exists returns Category")
    void findByName_categoryExists_returnsCategory() {
        // Arrange
        Category category = setupCategory(CATEGORY_NAME);
        Category savedCategory = categoryRepository.save(category);

        // Act
        Optional<Category> result = categoryRepository.findByName(CATEGORY_NAME);

        // Assert
        assertTrue(result.isPresent(), "Category should be found");
        assertEquals(savedCategory.getId(), result.get().getId(), "Category ID should match");
        assertEquals(CATEGORY_NAME, result.get().getName(), "Category name should match");
        assertNotNull(result.get().getCreatedAt(), "Created timestamp should be set");
        assertNotNull(result.get().getUpdatedAt(), "Updated timestamp should be set");
    }

    @Test
    @DisplayName("Check if Category name is unique when name is unique and categoryId is null returns true")
    void isCategoryNameUnique_uniqueNameNullCategoryId_returnsTrue() {
        // Arrange
        String uniqueName = "Unique Category Name";

        // Act
        boolean result = categoryRepository.isCategoryNameUnique(uniqueName, null);

        // Assert
        assertTrue(result, "Unique name with null categoryId should return true");
    }

    @Test
    @DisplayName("Check if Category name is unique when name exists and categoryId is null returns false")
    void isCategoryNameUnique_nameExistsNullCategoryId_returnsFalse() {
        // Arrange
        Category category = setupCategory(CATEGORY_NAME);
        categoryRepository.save(category);

        // Act
        boolean result = categoryRepository.isCategoryNameUnique(CATEGORY_NAME, null);

        // Assert
        assertFalse(result, "Existing name with null categoryId should return false");
    }

    @Test
    @DisplayName("Check if Category name is unique when name exists but categoryId excludes self returns true")
    void isCategoryNameUnique_nameExistsExcludingSelf_returnsTrue() {
        // Arrange
        Category category = setupCategory(CATEGORY_NAME);
        Category savedCategory = categoryRepository.save(category);

        // Act
        boolean result = categoryRepository.isCategoryNameUnique(CATEGORY_NAME, savedCategory.getId());

        // Assert
        assertTrue(result, "Existing name excluding self should return true");
    }

    @Test
    @DisplayName("Check if Category name is unique when name is different and categoryId is null returns true")
    void isCategoryNameUnique_differentNameNullCategoryId_returnsTrue() {
        // Arrange
        Category category = setupCategory(CATEGORY_NAME);
        categoryRepository.save(category);
        String differentName = "Different Category Name";

        // Act
        boolean result = categoryRepository.isCategoryNameUnique(differentName, null);

        // Assert
        assertTrue(result, "Different name with null categoryId should return true");
    }

    @Test
    @DisplayName("Check if Category is in use when linked to one task returns true")
    void isCategoryInUse_linkedToOneTask_returnsTrue() {
        // Arrange
        Category category = setupCategory(CATEGORY_NAME);
        Category savedCategory = categoryRepository.save(category);
        Set<Category> categories = new HashSet<>();
        categories.add(savedCategory);
        Task task = setupTask(TASK_TITLE, categories, defaultUser, null);
        taskRepository.save(task);

        // Act
        boolean result = categoryRepository.isCategoryInUse(savedCategory.getId());

        // Assert
        assertTrue(result, "Category linked to one task should be in use");
    }

    @Test
    @DisplayName("Check if Category is in use when no linked tasks returns false")
    void isCategoryInUse_noLinkedTasks_returnsFalse() {
        // Arrange
        Category category = setupCategory(CATEGORY_NAME);
        Category savedCategory = categoryRepository.save(category);

        // Act
        boolean result = categoryRepository.isCategoryInUse(savedCategory.getId());

        // Assert
        assertFalse(result, "Category with no linked tasks should not be in use");
    }

    @Test
    @DisplayName("Check if Category is in use for non-existent Category returns false")
    void isCategoryInUse_nonExistentCategory_returnsFalse() {
        // Act & Assert
        boolean result = categoryRepository.isCategoryInUse(NON_EXISTENT_ID);
        assertFalse(result, "Non-existent category should not be in use");
    }

    @Test
    @DisplayName("Check if Category is in use when linked to multiple tasks returns true")
    void isCategoryInUse_linkedToMultipleTasks_returnsTrue() {
        // Arrange
        Category category = setupCategory(CATEGORY_NAME);
        Category savedCategory = categoryRepository.save(category);
        Set<Category> categories = new HashSet<>();
        categories.add(savedCategory);
        Task task1 = setupTask(TASK_TITLE + " 1", categories, defaultUser, null);
        Task task2 = setupTask(TASK_TITLE + " 2", categories, defaultUser, null);

        // Act
        taskRepository.save(task1);
        taskRepository.save(task2);
        boolean result = categoryRepository.isCategoryInUse(savedCategory.getId());

        // Assert
        assertTrue(result, "Category linked to multiple tasks should be in use");
    }
}
