package todo.list.todo_list.service.impl;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import todo.list.todo_list.dto.Category.CategoryDTO;
import todo.list.todo_list.dto.Category.CategoryRequest;
import todo.list.todo_list.entity.Category;
import todo.list.todo_list.exception.CategoryInUseException;
import todo.list.todo_list.exception.ResourceConflictException;
import todo.list.todo_list.exception.ResourceNotFoundException;
import todo.list.todo_list.mapper.CategoryMapper;
import todo.list.todo_list.repository.CategoryRepository;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    private static final Long CATEGORY_ID = 1L;
    private static final String CATEGORY_NAME = "Category Name";
    private static final String NEW_CATEGORY_NAME = "New Category Name";

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category defaultCategory;

    @BeforeEach
    @SuppressWarnings("unused")
    void setup() {
        defaultCategory = new Category(CATEGORY_NAME);
        defaultCategory.setId(CATEGORY_ID);
    }

    private CategoryRequest setupCategoryRequest(String categoryName) {
        CategoryRequest request = new CategoryRequest();
        request.setName(categoryName);
        return request;
    }

    private CategoryDTO setupCategoryDTO(Long categoryId, String name) {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(categoryId);
        dto.setName(name);
        return dto;
    }

    private Category setupCategory(Long categoryId, String name) {
        Category category = new Category(name);
        category.setId(categoryId);
        return category;
    }

    private void setupSuccessfulCategoryMocks(CategoryRequest request, Category category, CategoryDTO dto, Long categoryId) {
        when(categoryRepository.isCategoryNameUnique(request.getName(), categoryId)).thenReturn(true);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);
        when(categoryMapper.toCategoryDTO(any(Category.class))).thenReturn(dto);
    }

    @Test
    @DisplayName("Create Category with valid data returns CategoryDTO")
    void createCategory_successfulCreation() {
        // Arrange
        CategoryRequest request = setupCategoryRequest(CATEGORY_NAME);
        CategoryDTO dto = setupCategoryDTO(CATEGORY_ID, CATEGORY_NAME);
        this.setupSuccessfulCategoryMocks(request, defaultCategory, dto, null);

        // Act
        CategoryDTO result = categoryService.createCategory(request);

        // Assert
        assertNotNull(result);
        assertEquals(CATEGORY_NAME, result.getName());
        assertEquals(CATEGORY_ID, result.getId());
        verify(categoryRepository).isCategoryNameUnique(CATEGORY_NAME, null);
        verify(categoryRepository).save(any(Category.class));
        verify(categoryMapper).toCategoryDTO(any(Category.class));
    }

    @Test
    @DisplayName("Create Category with duplicate name throws ResourceConflictException")
    void createCategory_duplicateCategoryName_throwsException() {
        // Arrange
        CategoryRequest request = setupCategoryRequest(CATEGORY_NAME);
        when(categoryRepository.isCategoryNameUnique(CATEGORY_NAME, null)).thenReturn(false);

        // Act & Assert
        ResourceConflictException exception = assertThrows(
                ResourceConflictException.class,
                () -> categoryService.createCategory(request)
        );
        assertEquals("Category name must be unique.", exception.getMessage());
        verify(categoryRepository).isCategoryNameUnique(CATEGORY_NAME, null);
        this.verifyNoCategorySave();
    }

    @Test
    @DisplayName("Create Category with null request throws IllegalArgumentException")
    void createCategory_nullRequest_throwsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> categoryService.createCategory(null)
        );
        assertEquals("Category request cannot be null", exception.getMessage());
        verify(categoryRepository, never()).isCategoryNameUnique(anyString(), anyLong());
        this.verifyNoCategorySave();
    }

    @Test
    @DisplayName("Get Category with valid ID returns CategoryDTO")
    void getCategory_successfulRetrieval() {
        // Arrange
        CategoryDTO dto = setupCategoryDTO(CATEGORY_ID, CATEGORY_NAME);
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(defaultCategory));
        when(categoryMapper.toCategoryDTO(defaultCategory)).thenReturn(dto);

        // Act
        CategoryDTO result = categoryService.getCategory(CATEGORY_ID);

        // Assert
        assertNotNull(result);
        assertEquals(CATEGORY_NAME, result.getName());
        assertEquals(CATEGORY_ID, result.getId());
        verify(categoryRepository).findById(CATEGORY_ID);
        verify(categoryMapper).toCategoryDTO(defaultCategory);
    }

    @Test
    @DisplayName("Get Category with null ID throws IllegalArgumentException")
    void getCategory_nullCategoryId_throwsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> categoryService.getCategory(null)
        );
        assertEquals("Category ID cannot be null", exception.getMessage());
        verify(categoryRepository, never()).findById(anyLong());
        verify(categoryMapper, never()).toCategoryDTO(any());
    }

    @Test
    @DisplayName("Update Category with valid data returns CategoryDTO")
    void updateCategory_successfulUpdate() {
        // Arrange
        CategoryRequest request = setupCategoryRequest(NEW_CATEGORY_NAME);
        Category updatedCategory = setupCategory(CATEGORY_ID, NEW_CATEGORY_NAME);
        CategoryDTO dto = setupCategoryDTO(CATEGORY_ID, NEW_CATEGORY_NAME);
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(defaultCategory));
        this.setupSuccessfulCategoryMocks(request, updatedCategory, dto, CATEGORY_ID);

        // Act
        CategoryDTO result = categoryService.updateCategory(CATEGORY_ID, request);

        // Assert
        assertNotNull(result);
        assertEquals(NEW_CATEGORY_NAME, result.getName());
        assertEquals(CATEGORY_ID, result.getId());
        verify(categoryRepository).findById(CATEGORY_ID);
        verify(categoryRepository).isCategoryNameUnique(NEW_CATEGORY_NAME, CATEGORY_ID);
        verify(categoryRepository).save(any(Category.class));
        verify(categoryMapper).toCategoryDTO(any(Category.class));
    }

    @Test
    @DisplayName("Update Category with non-existent ID throws ResourceNotFoundException")
    void updateCategory_categoryNotFound_throwsException() {
        // Arrange
        CategoryRequest request = setupCategoryRequest(NEW_CATEGORY_NAME);
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> categoryService.updateCategory(CATEGORY_ID, request)
        );
        assertEquals("Category not found with ID: " + CATEGORY_ID, exception.getMessage());
        verify(categoryRepository).findById(CATEGORY_ID);
        verify(categoryRepository, never()).isCategoryNameUnique(anyString(), anyLong());
        this.verifyNoCategorySave();
    }

    @Test
    @DisplayName("Update Category with duplicate name throws ResourceConflictException")
    void updateCategory_duplicateCategoryName_throwsException() {
        // Arrange
        CategoryRequest request = setupCategoryRequest(NEW_CATEGORY_NAME);
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(defaultCategory));
        when(categoryRepository.isCategoryNameUnique(NEW_CATEGORY_NAME, CATEGORY_ID)).thenReturn(false);

        // Act & Assert
        ResourceConflictException exception = assertThrows(
                ResourceConflictException.class,
                () -> categoryService.updateCategory(CATEGORY_ID, request)
        );
        assertEquals("Category name must be unique.", exception.getMessage());
        verify(categoryRepository).findById(CATEGORY_ID);
        verify(categoryRepository).isCategoryNameUnique(NEW_CATEGORY_NAME, CATEGORY_ID);
        this.verifyNoCategorySave();
    }

    @Test
    @DisplayName("Update Category with null ID throws IllegalArgumentException")
    void updateCategory_nullCategoryId_throwsException() {
        // Arrange
        CategoryRequest request = setupCategoryRequest(NEW_CATEGORY_NAME);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> categoryService.updateCategory(null, request)
        );
        assertEquals("Category ID cannot be null", exception.getMessage());
        verify(categoryRepository, never()).findById(anyLong());
        verify(categoryRepository, never()).isCategoryNameUnique(anyString(), anyLong());
        this.verifyNoCategorySave();
    }

    @Test
    @DisplayName("Update Category with null request throws IllegalArgumentException")
    void updateCategory_nullRequest_throwsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> categoryService.updateCategory(CATEGORY_ID, null)
        );
        assertEquals("Category request cannot be null", exception.getMessage());
        verify(categoryRepository, never()).findById(anyLong());
        verify(categoryRepository, never()).isCategoryNameUnique(anyString(), anyLong());
        this.verifyNoCategorySave();
    }

    @Test
    @DisplayName("Delete Category with valid ID succeeds")
    void deleteCategory_successfulDelete() {
        // Arrange
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(defaultCategory));
        when(categoryRepository.isCategoryInUse(CATEGORY_ID)).thenReturn(false);
        doNothing().when(categoryRepository).delete(defaultCategory);

        // Act
        categoryService.deleteCategory(CATEGORY_ID);

        // Assert
        InOrder inOrder = inOrder(categoryRepository);
        inOrder.verify(categoryRepository).findById(CATEGORY_ID);
        inOrder.verify(categoryRepository).isCategoryInUse(CATEGORY_ID);
        inOrder.verify(categoryRepository).delete(defaultCategory);
    }

    @Test
    @DisplayName("Delete Category with null ID throws IllegalArgumentException")
    void deleteCategory_nullCategoryId_throwsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> categoryService.deleteCategory(null)
        );
        assertEquals("Category ID cannot be null", exception.getMessage());
        verify(categoryRepository, never()).findById(anyLong());
        verify(categoryRepository, never()).isCategoryInUse(anyLong());
        verify(categoryRepository, never()).delete(any(Category.class));
    }

    @Test
    @DisplayName("Delete Category with non-existent ID throws ResourceNotFoundException")
    void deleteCategory_categoryNotFound_throwsException() {
        // Arrange
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> categoryService.deleteCategory(CATEGORY_ID)
        );
        assertEquals("Category not found with ID: " + CATEGORY_ID, exception.getMessage());
        verify(categoryRepository).findById(CATEGORY_ID);
        verify(categoryRepository, never()).isCategoryInUse(anyLong());
        verify(categoryRepository, never()).delete(any(Category.class));
    }

    @Test
    @DisplayName("Delete Category with in-use category throws CategoryInUseException")
    void deleteCategory_categoryInUse_throwsException() {
        // Arrange
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(defaultCategory));
        when(categoryRepository.isCategoryInUse(CATEGORY_ID)).thenReturn(true);

        // Act & Assert
        CategoryInUseException exception = assertThrows(
                CategoryInUseException.class,
                () -> categoryService.deleteCategory(CATEGORY_ID)
        );
        assertEquals("Category cannot be deleted as it is assigned to one or more tasks.", exception.getMessage());
        verify(categoryRepository).findById(CATEGORY_ID);
        verify(categoryRepository).isCategoryInUse(CATEGORY_ID);
        verify(categoryRepository, never()).delete(any(Category.class));
    }

    private void verifyNoCategorySave() {
        verify(categoryRepository, never()).save(any(Category.class));
        verify(categoryMapper, never()).toCategoryDTO(any(Category.class));
    }
}