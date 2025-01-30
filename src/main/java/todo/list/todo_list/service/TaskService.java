package todo.list.todo_list.service;

import java.util.List;

import todo.list.todo_list.dto.Task.TaskDTO;
import todo.list.todo_list.dto.Task.TaskRequest;

public interface TaskService {
    TaskDTO createTask(TaskRequest request);
    TaskDTO getTask(Long taskId);
    TaskDTO updateTask(Long taskId, TaskRequest request);
    void deleteTask(Long taskId);
    List<TaskDTO> getAllParentTasks();
    List<TaskDTO> getTasksByUser(Long taskId);
}