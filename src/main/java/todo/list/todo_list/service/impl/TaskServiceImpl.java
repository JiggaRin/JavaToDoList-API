package todo.list.todo_list.service.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import todo.list.todo_list.dto.Task.TaskDTO;
import todo.list.todo_list.dto.Task.TaskRequest;
import todo.list.todo_list.dto.Task.TaskStatusUpdateRequest;
import todo.list.todo_list.entity.Category;
import todo.list.todo_list.entity.Task;
import todo.list.todo_list.entity.User;
import todo.list.todo_list.exception.AccessDeniedException;
import todo.list.todo_list.exception.CannotProceedException;
import todo.list.todo_list.exception.DuplicateCategoryException;
import todo.list.todo_list.exception.ResourceConflictException;
import todo.list.todo_list.exception.ResourceNotFoundException;
import todo.list.todo_list.model.Status;
import todo.list.todo_list.repository.CategoryRepository;
import todo.list.todo_list.repository.TaskRepository;
import todo.list.todo_list.service.TaskService;
import todo.list.todo_list.service.UserService;

@Service
public class TaskServiceImpl implements TaskService {

    @Autowired
    private final UserService userService;
    private final TaskRepository taskRepository;
    private final CategoryRepository categoryRepository;

    public TaskServiceImpl(UserService userService, TaskRepository taskRepository, CategoryRepository categoryRepository) {
        this.userService = userService;
        this.taskRepository = taskRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public TaskDTO createTask(TaskRequest request) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserByUsername(username);

        if (!taskRepository.isTitleUnique(request.getTitle(), user.getId(), null)) {
            throw new ResourceConflictException("Title must be unique for the user");
        }

        if (hasDuplicateCategories(request.getCategoryNames())) {
            throw new DuplicateCategoryException("A task cannot have duplicate categories.");
        }

        Task task = new Task();
        task.setOwner(user);
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus() != null ? request.getStatus() : Status.TODO);

        if (request.getParentId() != null) {
            Task parentTask = taskRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent Task not found with ID: " + request.getParentId()));
            if (!parentTask.getOwner().getId().equals(user.getId())) {
                throw new AccessDeniedException("Parent task must belong to the authenticated user.");
            }
            task.setParentTask(parentTask);
        } else {
            task.setParentTask(null);
        }

        Set<Category> categories = fetchOrCreateCategories(request.getCategoryNames());
        task.setCategories(categories);

        return new TaskDTO(taskRepository.save(task));
    }

    @Override
    public TaskDTO getTask(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with ID: " + taskId));

        return new TaskDTO(task);
    }

    @Override
    public TaskDTO updateTaskStatus(Long taskId, TaskStatusUpdateRequest request) {
        Task existedTask = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with ID: " + taskId));

        if (request.getStatus() == Status.DONE) {
            validateChildTaskCompletion(taskId);
        }

        existedTask.setStatus(request.getStatus());

        return new TaskDTO(taskRepository.save(existedTask));
    }

    @Override
    public TaskDTO updateTask(Long taskId, TaskRequest request) {
        Task existedTask = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with ID: " + taskId));

        if (!taskRepository.isTitleUnique(request.getTitle(), existedTask.getOwner().getId(), taskId)) {
            throw new ResourceConflictException("Title must be unique for the user.");
        }

        if (hasDuplicateCategories(request.getCategoryNames())) {
            throw new DuplicateCategoryException("A task cannot have duplicate categories.");
        }

        if (request.getStatus() == Status.DONE) {
            validateChildTaskCompletion(taskId);
        }

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserByUsername(username);

        if (request.getParentId() != null) {
            Task parentTask = taskRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent Task not found with ID: " + request.getParentId()));
            if (!parentTask.getOwner().getId().equals(user.getId())) {
                throw new AccessDeniedException("Parent task must belong to the authenticated user.");
            }
            existedTask.setParentTask(parentTask);
        } else {
            existedTask.setParentTask(null);
        }

        existedTask.setOwner(user);
        existedTask.setTitle(request.getTitle());
        existedTask.setDescription(request.getDescription());
        existedTask.setStatus(request.getStatus());

        Set<Category> categories = request.getCategoryNames().stream()
                .map(name -> {
                    Category category = categoryRepository.findByName(name)
                            .orElseGet(() -> categoryRepository.save(new Category(name)));
                    return category;
                })
                .collect(Collectors.toSet());

        existedTask.setCategories(categories);

        return new TaskDTO(taskRepository.save(existedTask));
    }

    @Override
    public void deleteTask(Long taskId) {
        Task existindTask = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with ID: " + taskId));
        validateChildTaskCompletion(existindTask.getId());

        List<Task> childTasks = taskRepository.findByParentTaskId(taskId);
        
        for (Task childTask : childTasks) {
            taskRepository.delete(childTask);
        }

        taskRepository.delete(existindTask);
    }

    @Override
    public Page<TaskDTO> getAllTasks(Long userId, String search, int page, int size, String sortBy, String direction) {
        Page<Task> tasks = getTasksAccordingAdditionalParams(userId, search, page, size, sortBy, direction);

        return tasks.map(TaskDTO::new);
    }

    @Override
    public List<TaskDTO> getTasksByUser(Long userId) {
        User user = userService.getUserById(userId);
        List<Task> tasks = taskRepository.findByOwner(user);

        return tasks.stream()
                .map(TaskDTO::new)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isOwner(Long taskId, String username) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with ID: " + taskId));
                
        return task.getOwner().getUsername().equals(username);
    }

    private Set<Category> fetchOrCreateCategories(List<String> categoryNames) {
        return categoryNames.stream()
                .map(name -> categoryRepository.findByName(name)
                .orElseGet(() -> categoryRepository.save(new Category(name))))
                .collect(Collectors.toSet());
    }

    private boolean hasDuplicateCategories(List<String> categoryNames) {
        Set<String> uniqueCategories = new HashSet<>(categoryNames);
        return uniqueCategories.size() < categoryNames.size();
    }

    private void validateChildTaskCompletion(Long taskId) {
        List<Task> childTasks = taskRepository.findByParentTaskId(taskId);

        boolean hasIncompleteChildTasks = childTasks.stream()
                .anyMatch(task -> task.getStatus() != Status.DONE);

        if (hasIncompleteChildTasks) {
            throw new CannotProceedException("Cannot proceed with task " + taskId + " while child tasks are not completed.");
        }
    }

    private Page<Task> getTasksAccordingAdditionalParams(Long userId, String search, int page, int size, String sortBy, String direction) {
        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        PageRequest pageable = PageRequest.of(page, size, sort);
        
        return taskRepository.findParentTasks(userId, search, pageable);
    }
}
