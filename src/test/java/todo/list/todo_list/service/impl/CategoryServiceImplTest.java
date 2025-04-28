package todo.list.todo_list.service.impl;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

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

    @Test
    @DisplayName("Create Category with valid data returns CategoryDTO")
    void createCategory_successfulCreation() {
        CategoryRequest request = this.setupCategoryRequest("Category Name");

        Category savedCategory = this.setupCategory(1L, "Category Name");

        CategoryDTO dto = this.setupCategoryDTO(1L, "Category Name");

        when(categoryRepository.isCategoryNameUnique(request.getName(), null)).thenReturn(true);
        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);
        when(categoryMapper.toCategoryDTO(savedCategory)).thenReturn(dto);

        CategoryDTO result = categoryService.createCategory(request);
        assertNotNull(result);
        assertEquals(request.getName(), result.getName());
        assertEquals(1L, result.getId());

        verify(categoryRepository).isCategoryNameUnique(request.getName(), null);
        verify(categoryRepository).save(any(Category.class));
        verify(categoryMapper).toCategoryDTO(savedCategory);
    }

    @Test
    @DisplayName("Create Category with name which is already exists throws ResourceConflictException")
    void createCategory_duplicateCategoryName_throwsException() {
        CategoryRequest request = this.setupCategoryRequest("Category Name");

        when(categoryRepository.isCategoryNameUnique(request.getName(), null)).thenReturn(false);

        ResourceConflictException exception = assertThrows(
                ResourceConflictException.class,
                () -> categoryService.createCategory(request)
        );

        assertEquals("Category name must be unique.", exception.getMessage());

        verify(categoryRepository).isCategoryNameUnique(request.getName(), null);
        verify(categoryRepository, never()).save(any(Category.class));
        verify(categoryMapper, never()).toCategoryDTO(any(Category.class));
    }

    @Test
    @DisplayName("Create Category but Category request in NULL throws IllegalArgumentException")
    void createCategory_nullRequest_throwsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            categoryService.createCategory(null);
        });
        assertEquals("Category request cannot be null", exception.getMessage());

        verify(categoryRepository, never()).isCategoryNameUnique(anyString(), anyLong());
        verify(categoryRepository, never()).save(any());
        verify(categoryMapper, never()).toCategoryDTO(any());
    }

    @Test
    @DisplayName("Get Category by ID but categoryId is NULL throws IllegalArgumentException")
    void getCategory_nullCategoryId_throwsEsception() {
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
        Long categoryId = 1L;
        CategoryRequest request = this.setupCategoryRequest("New Category Name");

        Category savedCategory = this.setupCategory(categoryId, "New Category Name");

        CategoryDTO dto = this.setupCategoryDTO(categoryId, "New Category Name");

        Category category = this.setupCategory(categoryId, "New Category Name");

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryRepository.isCategoryNameUnique(request.getName(), category.getId())).thenReturn(true);
        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);
        when(categoryMapper.toCategoryDTO(savedCategory)).thenReturn(dto);

        CategoryDTO result = categoryService.updateCategory(categoryId, request);
        assertNotNull(result);
        assertEquals(request.getName(), result.getName());
        assertEquals(1L, result.getId());

        verify(categoryRepository).findById(categoryId);
        verify(categoryRepository).isCategoryNameUnique(request.getName(), category.getId());
        verify(categoryRepository).save(any(Category.class));
        verify(categoryMapper).toCategoryDTO(savedCategory);
    }

    @Test
    @DisplayName("Update Category with Category ID which is not found throws ResourceNotFoundException")
    void updateCategory_categoryNotFound_throwsException() {
        Long categoryId = 1L;
        CategoryRequest request = this.setupCategoryRequest("New Category Name");

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> categoryService.updateCategory(categoryId, request)
        );
        assertEquals("Category not found with ID: " + categoryId, exception.getMessage());

        verify(categoryRepository).findById(categoryId);
        verify(categoryRepository, never()).isCategoryNameUnique(anyString(), anyLong());
        verify(categoryRepository, never()).save(any(Category.class));
        verify(categoryMapper, never()).toCategoryDTO(any(Category.class));
    }

    @Test
    @DisplayName("Update Category with name which is already existed throws ResourceConflictException")
    void updateCategory_duplicateCategoryName_throwsException() {
        Long categoryId = 1L;
        CategoryRequest request = this.setupCategoryRequest("New Category Name");

        Category category = this.setupCategory(categoryId, "Old Category Name");

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        when(categoryRepository.isCategoryNameUnique(request.getName(), category.getId())).thenReturn(false);

        ResourceConflictException exception = assertThrows(
                ResourceConflictException.class,
                () -> categoryService.updateCategory(categoryId, request)
        );
        assertEquals("Category name must be unique.", exception.getMessage());

        verify(categoryRepository).findById(categoryId);
        verify(categoryRepository).isCategoryNameUnique(request.getName(), categoryId);
        verify(categoryRepository, never()).save(any(Category.class));
        verify(categoryMapper, never()).toCategoryDTO(any(Category.class));
    }

    @Test
    @DisplayName("Update Category but categoryId is NULL throws IllegalArgumentException")
    void updateCategory_nullCategoryId_throwsException() {
        CategoryRequest request = new CategoryRequest();
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> categoryService.updateCategory(null, request)
        );
        assertEquals("Category ID cannot be null", exception.getMessage());
        verify(categoryRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Update Category but Category request in NULL throws IllegalArgumentException")
    void updateCategory_nullRequest_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> categoryService.updateCategory(1L, null)
        );
        assertEquals("Category request cannot be null", exception.getMessage());

        verify(categoryRepository, never()).findById(anyLong());
        verify(categoryRepository, never()).isCategoryNameUnique(anyString(), anyLong());
        verify(categoryRepository, never()).save(any());
        verify(categoryMapper, never()).toCategoryDTO(any());
    }

    @Test
    @DisplayName("Delete Category with valid Category ID returns void")
    void deleteCategory_successfulDelete() {
        Long categoryId = 1L;
        Category category = this.setupCategory(categoryId, "Old Category Name");

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryRepository.isCategoryInUse(category.getId())).thenReturn(false);
        doNothing().when(categoryRepository).delete(category);

        categoryService.deleteCategory(categoryId);

        InOrder inOrder = inOrder(categoryRepository);
        inOrder.verify(categoryRepository).findById(categoryId);
        inOrder.verify(categoryRepository).isCategoryInUse(category.getId());
        inOrder.verify(categoryRepository).delete(category);
    }

    @Test
    @DisplayName("Delete Category but categoryId is NULL throws IllegalArgumentException")
    void deleteCategory_nullCategoryId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> categoryService.deleteCategory(null)
        );
        assertEquals("Category ID cannot be null", exception.getMessage());
        verify(categoryRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Delete Category with Category ID which is not found throws ResourceNotFoundException")
    void deleteCategory_categoryNotFound_throwsException() {
        Long categoryId = 1L;

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> categoryService.deleteCategory(categoryId)
        );
        assertEquals("Category not found with ID: " + categoryId, exception.getMessage());

        verify(categoryRepository).findById(categoryId);
        verify(categoryRepository, never()).isCategoryInUse(anyLong());
        verify(categoryRepository, never()).delete(any(Category.class));
    }

    @Test
    @DisplayName("Delete Category with correct Category ID but category is in use throws CategoryInUseException")
    void deleteCategory_categoryInUse_throwsException() {
        Long categoryId = 1L;
        Category category = this.setupCategory(categoryId, "Old Category Name");

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryRepository.isCategoryInUse(category.getId())).thenReturn(true);

        CategoryInUseException exception = assertThrows(
                CategoryInUseException.class,
                () -> categoryService.deleteCategory(category.getId())
        );
        assertEquals("Category cannot be deleted as it is assigned to one or more tasks.", exception.getMessage());

        verify(categoryRepository).findById(category.getId());
        verify(categoryRepository).isCategoryInUse(category.getId());
        verify(categoryRepository, never()).delete(any(Category.class));
    }
}
