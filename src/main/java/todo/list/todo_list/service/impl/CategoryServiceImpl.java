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
        return this.categoryRepository.findAll()
                .stream().map(this.categoryMapper::toCategoryDTO)
                .collect(Collectors.toList());
    }

    @Override
    public CategoryDTO createCategory(CategoryRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Category request cannot be null");
        }
        if (!this.categoryRepository.isCategoryNameUnique(request.getName(), null)) {
            throw new ResourceConflictException("Category name must be unique.");
        }

        Category category = this.categoryRepository.save(new Category(request.getName()));

        return this.categoryMapper.toCategoryDTO(category);
    }

    @Override
    public CategoryDTO getCategory(Long categoryId) {
        if (categoryId == null) {
            throw new IllegalArgumentException("Category ID cannot be null");
        }

        Category category = this.categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + categoryId));

        return this.categoryMapper.toCategoryDTO(category);
    }

    @Override
    public CategoryDTO updateCategory(Long categoryId, CategoryRequest request) {
        if (categoryId == null) {
            throw new IllegalArgumentException("Category ID cannot be null");
        }

        if (request == null) {
            throw new IllegalArgumentException("Category request cannot be null");
        }

        Category existedCategory = this.categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + categoryId));

        if (!this.categoryRepository.isCategoryNameUnique(request.getName(), existedCategory.getId())) {
            throw new ResourceConflictException("Category name must be unique.");
        }

        existedCategory.setName(request.getName());

        return this.categoryMapper.toCategoryDTO(this.categoryRepository.save(existedCategory));
    }

    @Override
    public void deleteCategory(Long categoryId) {
        if (categoryId == null) {
            throw new IllegalArgumentException("Category ID cannot be null");
        }
        
        Category existingCategory = this.categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + categoryId));
        if (this.categoryRepository.isCategoryInUse(existingCategory.getId())) {
            throw new CategoryInUseException("Category cannot be deleted as it is assigned to one or more tasks.");
        }
        this.categoryRepository.delete(existingCategory);
    }

}
