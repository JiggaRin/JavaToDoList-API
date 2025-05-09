package todo.list.todo_list.service;

import java.util.List;

import org.springframework.data.domain.Page;

import todo.list.todo_list.dto.Task.CreateTaskRequest;
import todo.list.todo_list.dto.Task.TaskDTO;
import todo.list.todo_list.dto.Task.TaskStatusUpdateRequest;
import todo.list.todo_list.dto.Task.UpdateTaskRequest;

public interface TaskService {

    TaskDTO createTask(CreateTaskRequest request);

    TaskDTO getTask(Long taskId);

    TaskDTO updateTask(Long taskId, UpdateTaskRequest request);

    TaskDTO updateTaskStatus(Long taskId, TaskStatusUpdateRequest request);

    void deleteTask(Long taskId);

    Page<TaskDTO> getAllTasks(Long userId, String search, int page, int size, String sortBy, String direction);

    List<TaskDTO> getTasksByUser(Long userId);

    boolean isOwner(Long taskId, String username);
}