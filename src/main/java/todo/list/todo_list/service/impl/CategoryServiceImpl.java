package todo.list.todo_list.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import todo.list.todo_list.dto.Category.CategoryDTO;
import todo.list.todo_list.dto.Category.CategoryRequest;
import todo.list.todo_list.entity.Category;
import todo.list.todo_list.exception.CategoryInUseException;
import todo.list.todo_list.exception.ResourceConflictException;
import todo.list.todo_list.exception.ResourceNotFoundException;
import todo.list.todo_list.mapper.CategoryMapper;
import todo.list.todo_list.repository.CategoryRepository;
import todo.list.todo_list.service.CategoryService;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    public CategoryServiceImpl(CategoryRepository categoryRepository, CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
    }

    @Override
    public List<CategoryDTO> getAllCategories() {
        return categoryRepository.findAll()
                .stream().map(categoryMapper::toCategoryDTO)
                .collect(Collectors.toList());
    }

    @Override
    public CategoryDTO createCategory(CategoryRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Category request cannot be null");
        }
        if (!categoryRepository.isCategoryNameUnique(request.getName(), null)) {
            throw new ResourceConflictException("Category name must be unique.");
        }

        Category category = categoryRepository.save(new Category(request.getName()));

        return categoryMapper.toCategoryDTO(category);
    }

    @Override
    public CategoryDTO getCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + categoryId));

        return categoryMapper.toCategoryDTO(category);
    }

    @Override
    public CategoryDTO updateCategory(Long categoryId, CategoryRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Category request cannot be null");
        }

        Category existedCategory = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + categoryId));

        if (!categoryRepository.isCategoryNameUnique(request.getName(), existedCategory.getId())) {
            throw new ResourceConflictException("Category name must be unique.");
        }

        existedCategory.setName(request.getName());

        return categoryMapper.toCategoryDTO(categoryRepository.save(existedCategory));
    }

    @Override
    public void deleteCategory(Long categoryId) {
        Category existingCategory = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + categoryId));
        if (categoryRepository.isCategoryInUse(existingCategory.getId())) {
            throw new CategoryInUseException("Category cannot be deleted as it is assigned to one or more tasks.");
        }
        categoryRepository.delete(existingCategory);
    }

}
