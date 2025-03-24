package todo.list.todo_list.service.impl;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import org.mockito.MockitoAnnotations;

import todo.list.todo_list.dto.Category.CategoryDTO;
import todo.list.todo_list.dto.Category.CategoryRequest;
import todo.list.todo_list.entity.Category;
import todo.list.todo_list.exception.CategoryInUseException;
import todo.list.todo_list.exception.ResourceConflictException;
import todo.list.todo_list.exception.ResourceNotFoundException;
import todo.list.todo_list.mapper.CategoryMapper;
import todo.list.todo_list.repository.CategoryRepository;

class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createCategory_successfulCreation() {
        CategoryRequest request = new CategoryRequest();
        request.setName("Category Name");
        Category savedCategory = new Category("Category Name");
        CategoryDTO dto = new CategoryDTO();
        dto.setId(1L);
        dto.setName("Category Name");

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
    void createCategory_duplicateCategoryName_throwsException() {
        CategoryRequest request = new CategoryRequest();
        request.setName("Category Name");

        when(categoryRepository.isCategoryNameUnique(request.getName(), null)).thenReturn(false);

        ResourceConflictException exception = assertThrows(ResourceConflictException.class, () -> {
            categoryService.createCategory(request);
        });

        assertEquals("Category name must be unique.", exception.getMessage());

        verify(categoryRepository).isCategoryNameUnique(request.getName(), null);
        verify(categoryRepository, never()).save(any(Category.class));
        verify(categoryMapper, never()).toCategoryDTO(any(Category.class));
    }

    @Test
    void updateCategory_successfulUpdate() {
        Long categoryId = 1L;
        CategoryRequest request = new CategoryRequest();
        request.setName("New Category Name");
        Category savedCategory = new Category("New Category Name");
        CategoryDTO dto = new CategoryDTO();
        dto.setId(1L);
        dto.setName("New Category Name");

        Category category = new Category("Old Category Name");
        category.setId(categoryId);
        
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
    void updateCategory_categoryNotFound_throwsException() {
        Long categoryId = 1L;
        CategoryRequest request = new CategoryRequest();
        request.setName("New Category Name");

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            categoryService.updateCategory(categoryId, request);
        });
        assertEquals("Category not found with ID: " + categoryId, exception.getMessage());

        verify(categoryRepository).findById(categoryId);
        verify(categoryRepository, never()).isCategoryNameUnique(anyString(), anyLong());
        verify(categoryRepository, never()).save(any(Category.class));
        verify(categoryMapper, never()).toCategoryDTO(any(Category.class));
    }

    @Test
    void updateCategory_duplicateCategoryName_throwsException() {
        Long categoryId = 1L;
        CategoryRequest request = new CategoryRequest();
        request.setName("New Category Name");
        Category category = new Category("Old Category Name");
        category.setId(categoryId);

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        when(categoryRepository.isCategoryNameUnique(request.getName(), category.getId())).thenReturn(false);
        
        ResourceConflictException exception = assertThrows(ResourceConflictException.class, () -> {
            categoryService.updateCategory(categoryId, request);
        });
        assertEquals("Category name must be unique.", exception.getMessage());

        verify(categoryRepository).findById(categoryId);
        verify(categoryRepository).isCategoryNameUnique(request.getName(), categoryId);
        verify(categoryRepository, never()).save(any(Category.class));
        verify(categoryMapper, never()).toCategoryDTO(any(Category.class));
    }

    @Test
    void deleteCategory_successfulDelete() {
        Long categoryId = 1L;
        Category category = new Category("Old Category Name");
        category.setId(categoryId);

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryRepository.isCategoryInUse(category.getId())).thenReturn(false);
        doNothing().when(categoryRepository).delete(category);

        categoryService.deleteCategory(categoryId);

        InOrder inOrder = inOrder(categoryRepository);
        inOrder.verify(categoryRepository).findById(categoryId);
        inOrder.verify(categoryRepository).isCategoryInUse(category.getId());
        inOrder.verify(categoryRepository).delete(category);

        verify(categoryRepository).findById(categoryId);
        verify(categoryRepository).isCategoryInUse(category.getId());
        verify(categoryRepository).delete(category);
    }

    @Test
    void deleteCategory_categoryNotFound_categoryNotFound_throwsException() {
        Long categoryId = 1L;

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            categoryService.deleteCategory(categoryId);
        });
        assertEquals("Category not found with ID: " + categoryId, exception.getMessage());

        verify(categoryRepository).findById(categoryId);
        verify(categoryRepository, never()).isCategoryInUse(anyLong());
        verify(categoryRepository, never()).delete(any(Category.class));
    }

    @Test
    void deleteCategory_categoryInUser_throwsException() {
        Long categoryId = 1L;
        Category category = new Category("Old Category Name");
        category.setId(categoryId);

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryRepository.isCategoryInUse(category.getId())).thenReturn(true);
        
        CategoryInUseException exception = assertThrows(CategoryInUseException.class, () -> {
            categoryService.deleteCategory(category.getId());
        });
        assertEquals("Category cannot be deleted as it is assigned to one or more tasks.", exception.getMessage());

        verify(categoryRepository).findById(category.getId());
        verify(categoryRepository).isCategoryInUse(category.getId());
        verify(categoryRepository, never()).delete(any(Category.class));
    }   
}
