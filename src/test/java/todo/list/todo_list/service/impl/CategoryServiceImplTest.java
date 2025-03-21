package todo.list.todo_list.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

import todo.list.todo_list.dto.Category.CategoryDTO;
import todo.list.todo_list.dto.Category.CategoryRequest;
import todo.list.todo_list.entity.Category;
import todo.list.todo_list.exception.ResourceConflictException;
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
}
