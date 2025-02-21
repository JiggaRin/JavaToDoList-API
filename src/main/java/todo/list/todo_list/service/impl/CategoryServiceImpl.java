package todo.list.todo_list.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import todo.list.todo_list.dto.Category.CategoryDTO;
import todo.list.todo_list.dto.Category.CategoryRequest;
import todo.list.todo_list.entity.Category;
import todo.list.todo_list.exception.ResourceConflictException;
import todo.list.todo_list.exception.ResourceNotFoundException;
import todo.list.todo_list.repository.CategoryRepository;
import todo.list.todo_list.service.CategoryService;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private final CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public List<CategoryDTO> getAllCategories() {
        return categoryRepository.findAll()
                .stream().map(category -> new CategoryDTO(category))
                .collect(Collectors.toList());
    }

    @Override
    public CategoryDTO createCategory(CategoryRequest request) {
        if (!categoryRepository.isCategoryNameUnique(request.getName(), null)) {
            throw new ResourceConflictException("Category name must be unique.");
        }

        Category category = categoryRepository.findByName(request.getName())
                .orElseGet(() -> categoryRepository.save(new Category(request.getName())));

        return new CategoryDTO(category);
    }

    @Override
    public CategoryDTO getCategory(Long catId) {
        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + catId));

        return new CategoryDTO(category);
    }

    @Override
    public CategoryDTO updateCategory(Long catId, CategoryRequest request) {
        Category existedCategory = categoryRepository.findById(catId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + catId));

        if (!categoryRepository.isCategoryNameUnique(request.getName(), existedCategory.getId())) {
            throw new ResourceConflictException("Category name must be unique.");
        }

        existedCategory.setName(request.getName());
        categoryRepository.save(existedCategory);

        return new CategoryDTO(categoryRepository.save(existedCategory));
    }

    @Override
    public void deleteCategory(Long catId) {
        Category existingCategory = categoryRepository.findById(catId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + catId));

        categoryRepository.delete(existingCategory);
    }

}
