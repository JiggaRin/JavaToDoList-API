package todo.list.todo_list.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
    private CategoryServiceImpl categoyService;

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

        when(this.categoryRepository.isCategoryNameUnique(request.getName(), null)).thenReturn(true);
        when(this.categoryRepository.save(any(Category.class))).thenReturn(savedCategory);
        when(this.categoryMapper.toCategoryDTO(savedCategory)).thenReturn(dto);

        CategoryDTO result = this.categoyService.createCategory(request);
        assertNotNull(result);
        assertEquals(request.getName(), result.getName());
        assertEquals(1L, result.getId());

        verify(this.categoryRepository).isCategoryNameUnique(request.getName(), null);
        verify(this.categoryRepository).save(any(Category.class));
        verify(this.categoryMapper).toCategoryDTO(savedCategory);
    }

    @Test
    @DisplayName("Create Category with name which is already exists throws ResourceConflictException")
    void createCategory_duplicateCategoryName_throwsException() {
        CategoryRequest request = this.setupCategoryRequest("Category Name");

        when(this.categoryRepository.isCategoryNameUnique(request.getName(), null)).thenReturn(false);

        ResourceConflictException exception = assertThrows(
                ResourceConflictException.class,
                () -> this.categoyService.createCategory(request)
        );

        assertEquals("Category name must be unique.", exception.getMessage());

        verify(this.categoryRepository).isCategoryNameUnique(request.getName(), null);
        verify(this.categoryRepository, never()).save(any(Category.class));
        verify(this.categoryMapper, never()).toCategoryDTO(any(Category.class));
    }

    @Test
    @DisplayName("Create Category but Category request in NULL throws IllegalArgumentException")
    void createCategory_nullRequest_throwsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            this.categoyService.createCategory(null);
        });
        assertEquals("Category request cannot be null", exception.getMessage());

        verify(this.categoryRepository, never()).isCategoryNameUnique(anyString(), anyLong());
        verify(this.categoryRepository, never()).save(any());
        verify(this.categoryMapper, never()).toCategoryDTO(any());
    }

    @Test
    @DisplayName("Get Category by ID but categoryId is NULL throws IllegalArgumentException")
    void getCategory_nullCategoryId_throwsEsception() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> this.categoyService.getCategory(null)
        );

        assertEquals("Category ID cannot be null", exception.getMessage());

        verify(this.categoryRepository, never()).findById(anyLong());
        verify(this.categoryMapper, never()).toCategoryDTO(any());
    }

    @Test
    @DisplayName("Update Category with valid data returns CategoryDTO")
    void updateCategory_successfulUpdate() {
        Long categoryId = 1L;
        CategoryRequest request = this.setupCategoryRequest("New Category Name");

        Category savedCategory = this.setupCategory(categoryId, "New Category Name");

        CategoryDTO dto = this.setupCategoryDTO(categoryId, "New Category Name");

        Category category = this.setupCategory(categoryId, "New Category Name");

        when(this.categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(this.categoryRepository.isCategoryNameUnique(request.getName(), category.getId())).thenReturn(true);
        when(this.categoryRepository.save(any(Category.class))).thenReturn(savedCategory);
        when(this.categoryMapper.toCategoryDTO(savedCategory)).thenReturn(dto);

        CategoryDTO result = this.categoyService.updateCategory(categoryId, request);
        assertNotNull(result);
        assertEquals(request.getName(), result.getName());
        assertEquals(1L, result.getId());

        verify(this.categoryRepository).findById(categoryId);
        verify(this.categoryRepository).isCategoryNameUnique(request.getName(), category.getId());
        verify(this.categoryRepository).save(any(Category.class));
        verify(this.categoryMapper).toCategoryDTO(savedCategory);
    }

    @Test
    @DisplayName("Update Category with Category ID which is not found throws ResourceNotFoundException")
    void updateCategory_categoryNotFound_throwsException() {
        Long categoryId = 1L;
        CategoryRequest request = this.setupCategoryRequest("New Category Name");

        when(this.categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> this.categoyService.updateCategory(categoryId, request)
        );
        assertEquals("Category not found with ID: " + categoryId, exception.getMessage());

        verify(this.categoryRepository).findById(categoryId);
        verify(this.categoryRepository, never()).isCategoryNameUnique(anyString(), anyLong());
        verify(this.categoryRepository, never()).save(any(Category.class));
        verify(this.categoryMapper, never()).toCategoryDTO(any(Category.class));
    }

    @Test
    @DisplayName("Update Category with name which is already existed throws ResourceConflictException")
    void updateCategory_duplicateCategoryName_throwsException() {
        Long categoryId = 1L;
        CategoryRequest request = this.setupCategoryRequest("New Category Name");

        Category category = this.setupCategory(categoryId, "Old Category Name");

        when(this.categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        when(this.categoryRepository.isCategoryNameUnique(request.getName(), category.getId())).thenReturn(false);

        ResourceConflictException exception = assertThrows(
                ResourceConflictException.class,
                () -> this.categoyService.updateCategory(categoryId, request)
        );
        assertEquals("Category name must be unique.", exception.getMessage());

        verify(this.categoryRepository).findById(categoryId);
        verify(this.categoryRepository).isCategoryNameUnique(request.getName(), categoryId);
        verify(this.categoryRepository, never()).save(any(Category.class));
        verify(this.categoryMapper, never()).toCategoryDTO(any(Category.class));
    }

    @Test
    @DisplayName("Update Category but categoryId is NULL throws IllegalArgumentException")
    void updateCategory_nullCategoryId_throwsException() {
        CategoryRequest request = new CategoryRequest();
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> this.categoyService.updateCategory(null, request)
        );
        assertEquals("Category ID cannot be null", exception.getMessage());
        verify(this.categoryRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Update Category but Category request in NULL throws IllegalArgumentException")
    void updateCategory_nullRequest_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> this.categoyService.updateCategory(1L, null)
        );
        assertEquals("Category request cannot be null", exception.getMessage());

        verify(this.categoryRepository, never()).findById(anyLong());
        verify(this.categoryRepository, never()).isCategoryNameUnique(anyString(), anyLong());
        verify(this.categoryRepository, never()).save(any());
        verify(this.categoryMapper, never()).toCategoryDTO(any());
    }

    @Test
    @DisplayName("Delete Category with valid Category ID returns void")
    void deleteCategory_successfulDelete() {
        Long categoryId = 1L;
        Category category = this.setupCategory(categoryId, "Old Category Name");

        when(this.categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(this.categoryRepository.isCategoryInUse(category.getId())).thenReturn(false);
        doNothing().when(this.categoryRepository).delete(category);

        this.categoyService.deleteCategory(categoryId);

        InOrder inOrder = inOrder(this.categoryRepository);
        inOrder.verify(this.categoryRepository).findById(categoryId);
        inOrder.verify(this.categoryRepository).isCategoryInUse(category.getId());
        inOrder.verify(this.categoryRepository).delete(category);
    }

    @Test
    @DisplayName("Delete Category but categoryId is NULL throws IllegalArgumentException")
    void deleteCategory_nullCategoryId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> this.categoyService.deleteCategory(null)
        );
        assertEquals("Category ID cannot be null", exception.getMessage());
        verify(this.categoryRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Delete Category with Category ID which is not found throws ResourceNotFoundException")
    void deleteCategory_categoryNotFound_throwsException() {
        Long categoryId = 1L;

        when(this.categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> this.categoyService.deleteCategory(categoryId)
        );
        assertEquals("Category not found with ID: " + categoryId, exception.getMessage());

        verify(this.categoryRepository).findById(categoryId);
        verify(this.categoryRepository, never()).isCategoryInUse(anyLong());
        verify(this.categoryRepository, never()).delete(any(Category.class));
    }

    @Test
    @DisplayName("Delete Category with correct Category ID but category is in use throws CategoryInUseException")
    void deleteCategory_categoryInUse_throwsException() {
        Long categoryId = 1L;
        Category category = this.setupCategory(categoryId, "Old Category Name");

        when(this.categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(this.categoryRepository.isCategoryInUse(category.getId())).thenReturn(true);

        CategoryInUseException exception = assertThrows(
                CategoryInUseException.class,
                () -> this.categoyService.deleteCategory(category.getId())
        );
        assertEquals("Category cannot be deleted as it is assigned to one or more tasks.", exception.getMessage());

        verify(this.categoryRepository).findById(category.getId());
        verify(this.categoryRepository).isCategoryInUse(category.getId());
        verify(this.categoryRepository, never()).delete(any(Category.class));
    }
}
