package todo.list.todo_list.service.impl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(CategoryServiceImpl.class);
    private static final int MIN_CATEGORY_NAME_LENGTH = 3;
    private static final int MAX_CATEGORY_NAME_LENGTH = 50;
    private static final int MAX_UPDATE_ATTEMPTS = 3;
    private static final long UPDATE_ATTEMPT_WINDOW_SECONDS = 60;
    private static final Map<Long, List<Long>> UPDATE_ATTEMPTS = new ConcurrentHashMap<>();
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    public CategoryServiceImpl(CategoryRepository categoryRepository, CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
    }

    @Override
    public List<CategoryDTO> getAllCategories() {
        log.debug("Received request to get All Categories");

        List<CategoryDTO> categoriesList = categoryRepository.findAll()
                .stream().map(categoryMapper::toCategoryDTO)
                .collect(Collectors.toList());

        log.info("Successfully Retreived List of Categories");

        return categoriesList;
    }

    @Override
    public CategoryDTO createCategory(CategoryRequest request) {
        log.info("Received Create Category request");
        validateCategoryRequest(request);

        String categoryName = request.getName();
        if (categoryName.length() < MIN_CATEGORY_NAME_LENGTH) {
            log.warn("Short category name detected: {}", categoryName);
        }
        if (categoryName.length() > MAX_CATEGORY_NAME_LENGTH) {
            log.warn("Long category name detected: {}", categoryName);
        }

        validateCategoryNameUniqueness(categoryName, null);

        Category category = categoryRepository.save(new Category(categoryName));
        log.info("Successfully created Category and assigned categoryID: {}", category.getId());

        return categoryMapper.toCategoryDTO(category);
    }

    @Override
    public CategoryDTO getCategory(Long categoryId) {
        log.info("Received Get Category by categoryID request");
        validateCategoryId(categoryId);

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + categoryId));

        log.info("Successfully retreived Category by categoryID: {}", categoryId);

        return categoryMapper.toCategoryDTO(category);
    }

    @Override
    public CategoryDTO updateCategory(Long categoryId, CategoryRequest request) {
        log.info("Received UPDATE Category request by categoryID: {}", categoryId);

        validateCategoryId(categoryId);

        validateCategoryRequest(request);

        Category existedCategory = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + categoryId));
        String categoryName = request.getName();
        if (categoryName.length() < MIN_CATEGORY_NAME_LENGTH) {
            log.warn("Short category name detected: {}", categoryName);
        }
        if (categoryName.length() > MAX_CATEGORY_NAME_LENGTH) {
            log.warn("Long category name detected: {}", categoryName);
        }
        validateCategoryNameUniqueness(categoryName, existedCategory.getId());

        trackUpdateAttempt(categoryId);

        existedCategory.setName(categoryName);

        log.info("Successfully updated Category by categoryID: {}", categoryId);

        return categoryMapper.toCategoryDTO(categoryRepository.save(existedCategory));
    }

    @Override
    public void deleteCategory(Long categoryId) {
        log.info("Received DELETE Category request by categoryID: {}", categoryId);
        validateCategoryId(categoryId);

        Category existingCategory = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + categoryId));

        validateIfCategoryInUse(categoryId);

        categoryRepository.delete(existingCategory);
        log.info("Successfully deleted Category by categoryID: {}", categoryId);
    }

    private void validateCategoryRequest(CategoryRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Category request cannot be null");
        }
    }

    private void validateCategoryNameUniqueness(String categoryName, Long categoryId) {
        if (!categoryRepository.isCategoryNameUnique(categoryName, categoryId)) {
            throw new ResourceConflictException("Category name must be unique.");
        }
    }

    private void validateCategoryId(Long categoryId) {
        if (categoryId == null) {
            throw new IllegalArgumentException("Category ID cannot be null");
        }
    }

    private void validateIfCategoryInUse(Long categoryId) {
        if (categoryRepository.isCategoryInUse(categoryId)) {
            throw new CategoryInUseException("Category cannot be deleted as it is assigned to one or more tasks.");
        }
    }

    private void trackUpdateAttempt(Long categoryId) {
        long currentTime = Instant.now().getEpochSecond();
        List<Long> attempts = UPDATE_ATTEMPTS.computeIfAbsent(categoryId, k -> new ArrayList<>());

        attempts.removeIf(timestamp -> currentTime - timestamp > UPDATE_ATTEMPT_WINDOW_SECONDS);

        attempts.add(currentTime);

        if (attempts.size() > MAX_UPDATE_ATTEMPTS) {
            log.warn("Frequent update attempts detected for category ID: {}, attempts: {}", categoryId, attempts.size());
        }
    }
}
