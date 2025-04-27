package todo.list.todo_list.integration.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

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

    private final String username = "testuser";

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    private Category setupCategory(String categoryName) {
        Category category = new Category();
        category.setName(categoryName);
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());

        return category;
    }

    private Task setupTask(String title, Set<Category> categories, User owner) {
        Task task = new Task();
        task.setTitle(title);
        task.setStatus(Status.TODO);
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

    @Test
    @DisplayName("Save Category with valid data persists and returns Category")
    void saveCategory_validData_successfulSave() {
        Category category = this.setupCategory("Category Name");

        Category savedCategory = this.categoryRepository.save(category);

        assertNotNull(savedCategory.getId());
        assertEquals("Category Name", savedCategory.getName());
        assertNotNull(savedCategory.getCreatedAt());
        assertNotNull(savedCategory.getUpdatedAt());
    }

    @Test
    @DisplayName("Find Category by Name when Category exists returns Category")
    void findByName_ShouldReturnCategory_whenCategoryExists() {
        Category category = this.setupCategory("Category Name");
        Category savedCategory = this.categoryRepository.save(category);

        Optional<Category> result = this.categoryRepository.findByName("Category Name");

        assertTrue(result.isPresent());
        assertEquals(savedCategory.getId(), result.get().getId());
        assertEquals("Category Name", result.get().getName());
        assertNotNull(result.get().getCreatedAt());
        assertNotNull(result.get().getUpdatedAt());
    }

    @Test
    @DisplayName("Check if Category name is Unique when name is unique and categoryId is null returns true")
    void isCategoryNameUnique_uniqueNameNullCategoryId_returnsTrue() {
        boolean result = this.categoryRepository.isCategoryNameUnique("Unique Category Name", null);

        assertTrue(result);
    }

    @Test
    @DisplayName("Check if Category name is Unique when name is unique and categoryId is null returns false")
    void isCategoryNameUnique_nameExistsNullCategoryId_returnsFalse() {
        Category category = this.setupCategory("Existing Category Name");
        this.categoryRepository.save(category);

        boolean result = this.categoryRepository.isCategoryNameUnique("Existing Category Name", null);

        assertFalse(result);
    }

    @Test
    @DisplayName("Check if Category name is Unique when name exists but categoryId excludes self returns true")
    void isCategoryNameUnique_nameExistsExcludingSelf_returnsTrue() {
        Category category = this.setupCategory("Existing Category Name");
        Category savedCategory = this.categoryRepository.save(category);

        boolean result = this.categoryRepository.isCategoryNameUnique("Existing Category Name", savedCategory.getId());

        assertTrue(result);
    }

    @Test
    @DisplayName("Check if Category name is Unique when Category name is different and categoryId is null returns true")
    void isCategoryNameUnique_differentNameNullCategoryId_returnsTrue() {
        Category category = this.setupCategory("Existing Category Name");
        this.categoryRepository.save(category);

        boolean result = this.categoryRepository.isCategoryNameUnique("Different Category Name", null);

        assertTrue(result);
    }

    @Test
    @DisplayName("Check if Category in USE when Category is linked to one task returns true")
    void isCategoryInUse_linkedToOneTask_returnsTrue() {
        User user = this.setupUser(this.username);
        User savedUser = this.userRepository.save(user);

        Category category = this.setupCategory("Category A");
        Category savedCategory = this.categoryRepository.save(category);

        Set<Category> categories = new HashSet<>();
        categories.add(savedCategory);
        Task task = this.setupTask("Task 1", categories, savedUser);
        this.taskRepository.save(task);

        boolean result = this.categoryRepository.isCategoryInUse(savedCategory.getId());

        assertTrue(result);
    }

    @Test
    @DisplayName("Check if Category in USE when Category has no linked tasks returns FALSE ")
    void isCategoryInUse_noLinkedTasks_returnsFalse() {
        Category category = this.setupCategory("Category A");
        Category savedCategory = this.categoryRepository.save(category);

        boolean result = this.categoryRepository.isCategoryInUse(savedCategory.getId());

        assertFalse(result);
    }

    @Test
    @DisplayName("Check if Category in USE for non-existent Category returns FALSE")
    void isCategoryInUse_nonExistentCategory_returnsFalse() {
        boolean result = this.categoryRepository.isCategoryInUse(999L);

        assertFalse(result);
    }

    @Test
    @DisplayName("Check if Category in USE when category is linked to multiple tasks returns TRUE")
    void isCategoryInUse_linkedToMultipleTasks_returnsTrue() {
        User user = this.setupUser(this.username);
        User savedUser = this.userRepository.save(user);

        Category category = this.setupCategory("Category A");
        Category savedCategory = this.categoryRepository.save(category);

        Set<Category> categories = new HashSet<>();
        categories.add(savedCategory);
        Task task1 = this.setupTask("Task 1", categories, savedUser);
        Task task2 = this.setupTask("Task 2", categories, savedUser);
        this.taskRepository.save(task1);
        this.taskRepository.save(task2);

        boolean result = this.categoryRepository.isCategoryInUse(savedCategory.getId());

        assertTrue(result);
    }

    @Test
    @DisplayName("Check if Category when other categories are linked but not this one in USE returns FALSE")
    void isCategoryInUse_otherCategoriesLinked_returnsFalse() {
        User user = this.setupUser(this.username);
        User savedUser = this.userRepository.save(user);

        Category categoryA = this.setupCategory("Category A");
        Category categoryB = this.setupCategory("Category B");
        Category savedCategoryA = this.categoryRepository.save(categoryA);
        Category savedCategoryB = this.categoryRepository.save(categoryB);

        Set<Category> categories = new HashSet<>();
        categories.add(savedCategoryA);
        Task task = this.setupTask("Task 1", categories, savedUser);
        this.taskRepository.save(task);

        boolean result = this.categoryRepository.isCategoryInUse(savedCategoryB.getId());

        assertFalse(result);
    }
}
