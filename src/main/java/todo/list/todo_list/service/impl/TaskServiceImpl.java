package todo.list.todo_list.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import todo.list.todo_list.dto.Task.TaskDTO;
import todo.list.todo_list.dto.Task.TaskRequest;
import todo.list.todo_list.entity.Task;
import todo.list.todo_list.entity.User;
import todo.list.todo_list.exception.ResourceNotFoundException;
import todo.list.todo_list.model.Status;
import todo.list.todo_list.repository.TaskRepository;
import todo.list.todo_list.service.TaskService;
import todo.list.todo_list.service.UserService;

@Service
public class TaskServiceImpl implements TaskService {

    @Autowired
    private final UserService userService;
    private final TaskRepository taskRepository;

    public TaskServiceImpl(UserService userService, TaskRepository taskRepository) {
        this.userService = userService;
        this.taskRepository = taskRepository;
    }

    @Override
    public TaskDTO createTask(TaskRequest request) {
        User user = userService.getUserById(request.getUserId());

        Task task = new Task();

        if (request.getParentId() != null) {
            Task parentTask = taskRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent Task not found with ID: " + request.getParentId()));
            task.setParentTask(parentTask);
        } else {
            task.setParentTask(null);
        }

        task.setUser(user);
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus() != null ? request.getStatus() : Status.TODO);

        Task savedTask = taskRepository.save(task);
        return new TaskDTO(savedTask);
    }

    @Override
    public TaskDTO getTask(Long taskId) {
        Optional<Task> taskOpt = taskRepository.findById(taskId);

        Task task = taskOpt.orElseThrow(() -> new ResourceNotFoundException("Task not found with ID: " + taskId));

        return new TaskDTO(task);
    }

    @Override
    public TaskDTO updateTask(Long taskId, TaskRequest request) {
        Optional<Task> taskOpt = taskRepository.findById(taskId);
        User user = userService.getUserById(request.getUserId());

        Task existedTask = taskOpt.orElseThrow(() -> new ResourceNotFoundException("Task not found with ID: " + taskId));

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

        return new TaskDTO(taskRepository.save(existedTask));
    }

    @Override
    public void deleteTask(Long taskId) {
        Optional<Task> taskOpt = taskRepository.findById(taskId);

        Task existindTask = taskOpt.orElseThrow(() -> new ResourceNotFoundException("Task not found with ID: " + taskId));

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
}
