package todo.list.todo_list.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import todo.list.todo_list.dto.Category.CategoryDTO;
import todo.list.todo_list.dto.Category.CategoryRequest;
import todo.list.todo_list.service.CategoryService;

@RestController
@RequestMapping("/api/tasks/categories")
public class CategoryController {

    private static final Logger log = LoggerFactory.getLogger(CategoryController.class);
    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    public ResponseEntity<List<CategoryDTO>> getAllCategories() {
        log.debug("Received request to get All Categories");
         try {
            ResponseEntity<List<CategoryDTO>> categoriesList = this.gettingAllCategoriesList();

            return categoriesList;
        } catch (Exception e) {
            log.error("Get All Categories request failed", e);
            throw e;
        }
    }

    @PostMapping("/")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    public ResponseEntity<CategoryDTO> createCategory(@Valid @RequestBody CategoryRequest categoryRequest) {
        log.debug("Received request to Create New Category");
         try {
            ResponseEntity<CategoryDTO> createdCategory = this.creatingCategory(categoryRequest);

            return createdCategory;
        } catch (Exception e) {
            log.error("Create New Category request failed", e);
            throw e;
        }
    }

    @GetMapping("/{categoryId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    public ResponseEntity<CategoryDTO> getCategoryById(@PathVariable Long categoryId) {
        log.debug("Received request to GET Category by categoryId: {}", categoryId);
         try {
            ResponseEntity<CategoryDTO> category = this.gettingCategory(categoryId);

            return category;
        } catch (Exception e) {
            log.error("Get Category by ID: {} request failed", categoryId, e);
            throw e;
        }
    }

    @PutMapping("/{categoryId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    public ResponseEntity<CategoryDTO> updateCategory(@PathVariable Long categoryId, @Valid @RequestBody CategoryRequest request) {
        log.debug("Received request to UPDATE Category by ID: {}", categoryId);
         try {
            ResponseEntity<CategoryDTO> updatedCategory = this.updatingCategory(categoryId, request);

            return updatedCategory;
        } catch (Exception e) {
            log.error("Update Category by ID: {} request failed", categoryId, e);
            throw e;
        }
        
    }

    @DeleteMapping("/{categoryId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long categoryId) {
        log.debug("Received request to DELETE Category by ID: {}", categoryId);
         try {
            ResponseEntity<Void> response = this.deletingCategory(categoryId);
            
            return response;
        } catch (Exception e) {
            log.error("Update Category by ID: {} request failed", categoryId, e);
            throw e;
        }
    }

    private ResponseEntity<List<CategoryDTO>> gettingAllCategoriesList() {
        List<CategoryDTO> categories = categoryService.getAllCategories();
        log.info("Successfully Retreived List of Categories");

        return new ResponseEntity<>(categories, HttpStatus.OK);
    }

    private ResponseEntity<CategoryDTO> creatingCategory(CategoryRequest categoryRequest) {
        CategoryDTO category = categoryService.createCategory(categoryRequest);
        log.info("Successfully Created Category");

        return new ResponseEntity<>(category, HttpStatus.CREATED);
    }

    private ResponseEntity<CategoryDTO> gettingCategory(Long categoryId) {
        CategoryDTO category = categoryService.getCategory(categoryId);
        log.info("Successfully got Category by ID: {}", categoryId);

        return new ResponseEntity<>(category, HttpStatus.OK);
    }

    private ResponseEntity<CategoryDTO> updatingCategory(Long categoryId, CategoryRequest request) {
        CategoryDTO existedCategory = categoryService.updateCategory(categoryId, request);
        log.info("Successfully Updated Category by ID: {}", categoryId);

        return new ResponseEntity<>(existedCategory, HttpStatus.OK);
    }

    private ResponseEntity<Void> deletingCategory(Long categoryId) {
        categoryService.deleteCategory(categoryId);
        log.info("Successfully Deleted Category by ID: {}", categoryId);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}