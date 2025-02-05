package todo.list.todo_list.service.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import todo.list.todo_list.dto.Task.TaskDTO;
import todo.list.todo_list.dto.Task.TaskRequest;
import todo.list.todo_list.entity.Category;
import todo.list.todo_list.entity.Task;
import todo.list.todo_list.entity.User;
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
        if (!taskRepository.isTitleUnique(request.getTitle(), request.getUserId())) {
            throw new ResourceConflictException("Title must be unique for the user");
        }

        if (hasDuplicateCategories(request.getCategoryNames())) {
            throw new DuplicateCategoryException("A task cannot have duplicate categories.");
        }
        User user = userService.getUserById(request.getUserId());

        Task task = new Task();
        task.setUser(user);
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus() != null ? request.getStatus() : Status.TODO);

        if (request.getParentId() != null) {
            Task parentTask = taskRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent Task not found with ID: " + request.getParentId()));
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
    public TaskDTO updateTask(Long taskId, TaskRequest request) {
        User user = userService.getUserById(request.getUserId());

        Task existedTask = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with ID: " + taskId));

        if (request.getParentId() != null) {
            Task parentTask = taskRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent Task not found with ID: " + request.getParentId()));
            existedTask.setParentTask(parentTask);
        } else {
            existedTask.setParentTask(null);
        }

        existedTask.setUser(user);
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

        taskRepository.delete(existindTask);
    }

    @Override
    public List<TaskDTO> getAllParentTasks() {
        return taskRepository.findParentTasks().stream()
                .map(TaskDTO::new)
                .collect(Collectors.toList());
    }

    @Override
    public List<TaskDTO> getTasksByUser(Long userId) {
        User user = userService.getUserById(userId);
        List<Task> tasks = taskRepository.findByUser(user);

        return tasks.stream()
                .map(TaskDTO::new)
                .collect(Collectors.toList());
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
}
