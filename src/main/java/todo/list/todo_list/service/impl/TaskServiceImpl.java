package todo.list.todo_list.service.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import todo.list.todo_list.dto.Task.CreateTaskRequest;
import todo.list.todo_list.dto.Task.TaskDTO;
import todo.list.todo_list.dto.Task.TaskStatusUpdateRequest;
import todo.list.todo_list.dto.Task.UpdateTaskRequest;
import todo.list.todo_list.entity.Category;
import todo.list.todo_list.entity.Task;
import todo.list.todo_list.entity.User;
import todo.list.todo_list.exception.AccessDeniedException;
import todo.list.todo_list.exception.CannotProceedException;
import todo.list.todo_list.exception.DuplicateCategoryException;
import todo.list.todo_list.exception.ResourceConflictException;
import todo.list.todo_list.exception.ResourceNotFoundException;
import todo.list.todo_list.mapper.TaskMapper;
import todo.list.todo_list.model.Status;
import todo.list.todo_list.repository.CategoryRepository;
import todo.list.todo_list.repository.TaskRepository;
import todo.list.todo_list.service.TaskService;
import todo.list.todo_list.service.UserService;

@Service
public class TaskServiceImpl implements TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskServiceImpl.class);
    private final UserService userService;
    private final TaskRepository taskRepository;
    private final CategoryRepository categoryRepository;
    private final TaskMapper taskMapper;

    public TaskServiceImpl(UserService userService, TaskRepository taskRepository, CategoryRepository categoryRepository, TaskMapper taskMapper) {
        this.userService = userService;
        this.taskRepository = taskRepository;
        this.categoryRepository = categoryRepository;
        this.taskMapper = taskMapper;
    }

    @Override
    public TaskDTO createTask(CreateTaskRequest request) {
        log.debug("Received task creation request");
        validateTaskRequest(request, "Task request cannot be null.");

        User user = getAuthenticatedUser();
        log.debug("Creating task for user: {}", user.getUsername());

        validateTitleUniqueness(request.getTitle(), user.getId(), null);
        validateCategories(request.getCategoryNames());

        Task task = taskMapper.createTaskFromRequest(request);
        task.setOwner(user);
        task.setStatus(request.getStatus() != null ? request.getStatus() : Status.TODO);

        if (request.getParentId() != null) {
            assignParentTask(task, request.getParentId(), user);
        }

        task.setCategories(fetchOrCreateCategories(request.getCategoryNames()));

        Task savedTask = saveTask(task);
        log.info("Successfully created task with ID: {} for user: {}", savedTask.getId(), user.getUsername());
        return taskMapper.toTaskDTO(savedTask);
    }

    @Override
    public TaskDTO getTask(Long taskId) {
        log.debug("Received request to retrieve task with ID: {}", taskId);
        validateTaskId(taskId);

        Task task = findTaskById(taskId);
        log.info("Successfully retrieved task with ID: {}", taskId);
        return taskMapper.toTaskDTO(task);
    }

    @Override
    public TaskDTO updateTask(Long taskId, UpdateTaskRequest request) {
        log.debug("Received request to update task with ID: {}", taskId);
        validateTaskId(taskId);
        validateTaskRequest(request, "Task request cannot be null.");

        Task existingTask = findTaskById(taskId);
        User user = getAuthenticatedUser();
        log.debug("Updating task for user: {}", user.getUsername());

        if (request.getTitle() != null) {
            validateTitleUniqueness(request.getTitle(), user.getId(), taskId);
        }

        if (request.getCategoryNames() != null) {
            validateCategories(request.getCategoryNames());
            existingTask.setCategories(fetchOrCreateCategories(request.getCategoryNames()));
        }

        if (request.getStatus() != null && request.getStatus() == Status.DONE) {
            validateChildTaskCompletion(taskId);
        }

        if (request.getParentId() != null) {
            assignParentTask(existingTask, request.getParentId(), user);
        }

        taskMapper.updateTaskFromRequest(request, existingTask);
        existingTask.setOwner(user);

        Task savedTask = saveTask(existingTask);
        log.info("Successfully updated task with ID: {}", taskId);
        return taskMapper.toTaskDTO(savedTask);
    }

    @Override
    public TaskDTO updateTaskStatus(Long taskId, TaskStatusUpdateRequest request) {
        log.debug("Received request to update status for task with ID: {}", taskId);
        validateTaskId(taskId);
        validateTaskRequest(request, "Task Status Update request cannot be null.");

        Task existingTask = findTaskById(taskId);
        if (request.getStatus() == Status.DONE) {
            validateChildTaskCompletion(taskId);
        }

        existingTask.setStatus(request.getStatus());
        Task savedTask = saveTask(existingTask);
        log.info("Successfully updated status for task with ID: {} to: {}", taskId, request.getStatus());
        return taskMapper.toTaskDTO(savedTask);
    }

    @Override
    public void deleteTask(Long taskId) {
        log.debug("Received request to delete task with ID: {}", taskId);
        validateTaskId(taskId);

        Task existingTask = findTaskById(taskId);
        List<Task> childTasks = taskRepository.findByParentTaskId(taskId);

        if (!childTasks.isEmpty()) {
            validateChildTaskCompletion(taskId);
        }

        childTasks.forEach(taskRepository::delete);
        taskRepository.delete(existingTask);
        log.info("Successfully deleted task with ID: {} and {} child tasks", taskId, childTasks.size());
    }

    @Override
    public Page<TaskDTO> getAllTasks(Long userId, String search, int page, int size, String sortBy, String direction) {
        log.debug("Received request to retrieve tasks for user ID: {}, search: {}, page: {}, size: {}", userId, search, page, size);
        validateSortParameters(sortBy, direction);

        Page<Task> tasks = getTasksAccordingAdditionalParams(userId, search, page, size, sortBy, direction);
        log.info("Successfully retrieved {} tasks for user ID: {}", tasks.getTotalElements(), userId);
        return tasks.map(taskMapper::toTaskDTO);
    }

    @Override
    public List<TaskDTO> getTasksByUser(Long userId) {
        log.debug("Received request to retrieve tasks for user ID: {}", userId);
        validateUserId(userId);

        User user = userService.getUserById(userId);
        List<Task> tasks = taskRepository.findByOwner(user);
        log.info("Successfully retrieved {} tasks for user ID: {}", tasks.size(), userId);
        return tasks.stream().map(taskMapper::toTaskDTO).collect(Collectors.toList());
    }

    @Override
    public boolean isOwner(Long taskId, String username) {
        log.debug("Checking ownership for task ID: {} by username: {}", taskId, username);
        validateTaskId(taskId);
        if (username == null) {
            log.warn("Null username provided for ownership check of task ID: {}", taskId);
            throw new IllegalArgumentException("Username cannot be null.");
        }

        Task task = findTaskById(taskId);
        boolean isOwner = task.getOwner().getUsername().equals(username);
        log.debug("Ownership check for task ID: {} by username: {} resulted in: {}", taskId, username, isOwner);
        return isOwner;
    }

    private User getAuthenticatedUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        log.debug("Fetching authenticated user: {}", username);
        return userService.getUserByUsername(username);
    }

    private void validateTaskId(Long taskId) {
        if (taskId == null) {
            log.warn("Task ID is null");
            throw new IllegalArgumentException("Task ID cannot be null.");
        }
    }

    private void validateUserId(Long userId) {
        if (userId == null) {
            log.warn("User ID is null");
            throw new IllegalArgumentException("User ID cannot be null.");
        }
    }

    private void validateTaskRequest(Object request, String errorMessage) {
        if (request == null) {
            log.warn("Invalid request: {}", errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private void validateSortParameters(String sortBy, String direction) {
        if (sortBy == null) {
            log.warn("Sort by field is null");
            throw new IllegalArgumentException("Sort by field cannot be null.");
        }
        if (direction == null) {
            log.warn("Sort direction is null");
            throw new IllegalArgumentException("Sort direction cannot be null.");
        }
    }

    private void validateTitleUniqueness(String title, Long userId, Long taskId) {
        if (!taskRepository.isTitleUnique(title, userId, taskId)) {
            log.warn("Non-unique title detected: {} for user ID: {}", title, userId);
            throw new ResourceConflictException("Title must be unique for the user.");
        }
    }

    private void validateCategories(List<String> categoryNames) {
        if (hasDuplicateCategories(categoryNames)) {
            log.warn("Duplicate categories detected: {}", categoryNames);
            throw new DuplicateCategoryException("A task cannot have duplicate categories.");
        }
    }

    private Task findTaskById(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> {
                    log.error("Task not found with ID: {}", taskId);
                    return new ResourceNotFoundException("Task not found with ID: " + taskId);
                });
    }

    private void assignParentTask(Task task, Long parentId, User user) {
        log.debug("Assigning parent task with ID: {} to task", parentId);
        Task parentTask = taskRepository.findById(parentId)
                .orElseThrow(() -> {
                    log.error("Parent task not found with ID: {}", parentId);
                    return new ResourceNotFoundException("Parent Task not found with ID: " + parentId);
                });
        if (!parentTask.getOwner().getId().equals(user.getId())) {
            log.warn("Parent task ID: {} does not belong to user: {}", parentId, user.getUsername());
            throw new AccessDeniedException("Parent task must belong to the authenticated user.");
        }
        task.setParentTask(parentTask);

    }

    private Set<Category> fetchOrCreateCategories(List<String> categoryNames) {
        if (categoryNames == null || categoryNames.isEmpty()) {
            log.debug("No categories provided, returning empty set");
            return new HashSet<>();
        }

        log.debug("Fetching or creating categories: {}", categoryNames);
        return categoryNames.stream()
                .map(name -> categoryRepository.findByName(name)
                .orElseGet(() -> {
                    log.debug("Creating new category: {}", name);
                    return categoryRepository.save(new Category(name));
                }))
                .collect(Collectors.toSet());
    }

    boolean hasDuplicateCategories(List<String> categoryNames) {
        if (categoryNames == null) {
            return false;
        }
        Set<String> uniqueCategories = new HashSet<>(categoryNames);
        boolean hasDuplicates = uniqueCategories.size() < categoryNames.size();
        if (hasDuplicates) {
            log.warn("Duplicate categories detected: {}", categoryNames);
        }
        return hasDuplicates;
    }

    void validateChildTaskCompletion(Long taskId) {
        List<Task> childTasks = taskRepository.findByParentTaskId(taskId);
        boolean hasIncompleteChildTasks = childTasks.stream()
                .anyMatch(task -> task.getStatus() != Status.DONE);

        if (hasIncompleteChildTasks) {
            log.warn("Incomplete child tasks detected for task ID: {}", taskId);
            throw new CannotProceedException("Cannot proceed with task " + taskId + " while child tasks are not completed.");
        }
    }

    private Task saveTask(Task task) {
        try {
            Task savedTask = taskRepository.save(task);
            if (savedTask == null) {
                log.error("Failed to save task: {}", task.getTitle());
                throw new IllegalStateException("Failed to save task with ID: " + task.getId());
            }
            return savedTask;
        } catch (IllegalStateException e) {
            log.error("Error saving task: {} due to: {}", task.getTitle(), e.getMessage());
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private Page<Task> getTasksAccordingAdditionalParams(Long userId, String search, int page, int size, String sortBy, String direction) {
        log.debug("Fetching tasks with userId: {}, search: {}, sortBy: {}, direction: {}", userId, search, sortBy, direction);
        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        PageRequest pageable = PageRequest.of(page, size, sort);
        return taskRepository.findParentTasks(userId, search, pageable);
    }
}
