package todo.list.todo_list.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import todo.list.todo_list.dto.Task.TaskDTO;
import todo.list.todo_list.dto.Task.TaskRequest;
import todo.list.todo_list.dto.Task.TaskStatusUpdateRequest;
import todo.list.todo_list.security.CustomUserDetails;
import todo.list.todo_list.service.TaskService;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    @Autowired
    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping("/")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<TaskDTO> createTask(@Valid @RequestBody TaskRequest request) {
        TaskDTO createdTask = taskService.createTask(request);

        return new ResponseEntity<>(createdTask, HttpStatus.CREATED);
    }

    @GetMapping("/{taskId}")
    @PreAuthorize("hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<TaskDTO> getTaskById(@PathVariable Long taskId) {
        TaskDTO task = taskService.getTask(taskId);

        return new ResponseEntity<>(task, HttpStatus.OK);
    }

    @PutMapping("/{taskId}")
    @PreAuthorize("(hasRole('USER') and @taskService.isOwner(#taskId, authentication.name)) or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<TaskDTO> updateTask(@PathVariable Long taskId, @Valid @RequestBody TaskRequest request) {
        TaskDTO task = taskService.updateTask(taskId, request);

        return new ResponseEntity<>(task, HttpStatus.OK);
    }

    @DeleteMapping("/{taskId}")
    @PreAuthorize("hasRole('USER') and @taskService.isOwner(#taskId, authentication.name) or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTask(@PathVariable Long taskId) {
        taskService.deleteTask(taskId);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    public ResponseEntity<Page<TaskDTO>> getAllParentTasks(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        Page<TaskDTO> tasks = taskService.getAllTasks(null, search, page, size, sortBy, direction);
        return new ResponseEntity<>(tasks, HttpStatus.OK);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    public ResponseEntity<List<TaskDTO>> getTasksByUser(@PathVariable Long userId) {
        List<TaskDTO> tasks = taskService.getTasksByUser(userId);

        return new ResponseEntity<>(tasks, HttpStatus.OK);
    }

    @GetMapping("/my-tasks")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<TaskDTO>> getUserTasks(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        Page<TaskDTO> tasks = taskService.getAllTasks(userDetails.getId(), search, page, size, sortBy, direction);

        return new ResponseEntity<>(tasks, HttpStatus.OK);
    }

    @PutMapping("/update-status/{taskId}")
    @PreAuthorize("(hasRole('USER') and @taskService.isOwner(#taskId, authentication.name)) or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<TaskDTO> updateTaskStatus(@PathVariable Long taskId, @Valid @RequestBody TaskStatusUpdateRequest request) {
        TaskDTO task = taskService.updateTaskStatus(taskId, request);

        return new ResponseEntity<>(task, HttpStatus.OK);
    }
}
