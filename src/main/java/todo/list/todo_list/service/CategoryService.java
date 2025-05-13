package todo.list.todo_list.service;

import java.util.List;

import todo.list.todo_list.dto.Category.CategoryDTO;
import todo.list.todo_list.dto.Category.CategoryRequest;

public interface CategoryService {

    List<CategoryDTO> getAllCategories();

    CategoryDTO createCategory(CategoryRequest request);

    CategoryDTO getCategory(Long catId);

    CategoryDTO updateCategory(Long catId, CategoryRequest request);

    void deleteCategory(Long catId);
}
