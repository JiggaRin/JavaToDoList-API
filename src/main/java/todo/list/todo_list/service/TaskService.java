package todo.list.todo_list.service;

import java.util.List;

import todo.list.todo_list.dto.Task.TaskDTO;
import todo.list.todo_list.dto.Task.TaskRequest;
import todo.list.todo_list.dto.Task.TaskStatusUpdateRequest;

public interface TaskService {

    TaskDTO createTask(TaskRequest request);

    TaskDTO getTask(Long taskId);

    TaskDTO updateTask(Long taskId, TaskRequest request);

    TaskDTO updateTaskStatus(Long taskId, TaskStatusUpdateRequest request);

    void deleteTask(Long taskId);

    List<TaskDTO> getAllParentTasks();

    List<TaskDTO> getTasksByUser(Long userId);

    List<TaskDTO> getUserTasks(String username);

    boolean isOwner(Long taskId, String username);

}