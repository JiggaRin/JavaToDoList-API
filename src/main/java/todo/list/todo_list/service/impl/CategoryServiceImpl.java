package todo.list.todo_list.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
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
        if (!categoryRepository.isCategoryNameUnique(request.getName(), null)) {
            throw new ResourceConflictException("Category name must be unique.");
        }

        Category category = categoryRepository.findByName(request.getName())
                .orElseGet(() -> categoryRepository.save(new Category(request.getName())));

        return categoryMapper.toCategoryDTO(category);
    }

    @Override
    public CategoryDTO getCategory(Long catId) {
        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + catId));

        return categoryMapper.toCategoryDTO(category);
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

        return categoryMapper.toCategoryDTO(categoryRepository.save(existedCategory));
    }

    @Override
    public void deleteCategory(Long catId) {
        Category existingCategory = categoryRepository.findById(catId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + catId));
        if (categoryRepository.isCategoryInUse(existingCategory.getId())) {
            throw new CategoryInUseException("Category cannot be deleted as it is assigned to one or more tasks.");
        }
        categoryRepository.delete(existingCategory);
    }

}
