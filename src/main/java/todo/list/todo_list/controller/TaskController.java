package todo.list.todo_list.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(TaskController.class);
    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping("/")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<TaskDTO> createTask(@Valid @RequestBody TaskRequest taskRequest) {
        log.debug("Received Create Task request");

        try {
            ResponseEntity<TaskDTO> response = this.creatingTask(taskRequest);

            return response;
        } catch (Exception e) {
            log.error("Create Task request failed", e);
            throw e;
        }
    }

    @GetMapping("/{taskId}")
    @PreAuthorize("hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<TaskDTO> getTaskById(@PathVariable Long taskId) {
        log.debug("Received Get Task request by taskID: {}", taskId);

        try {
            ResponseEntity<TaskDTO> response = this.gettingTask(taskId);

            return response;
        } catch (Exception e) {
            log.error("Get Task By taskID request failed", e);
            throw e;
        }
    }

    @PutMapping("/{taskId}")
    @PreAuthorize("(hasRole('USER') and @taskService.isOwner(#taskId, authentication.name)) or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<TaskDTO> updateTask(@PathVariable Long taskId, @Valid @RequestBody TaskRequest taskRequest) {
        log.debug("Received Update Task request by taskID: {}", taskId);

        try {
            ResponseEntity<TaskDTO> response = this.updatingTask(taskId, taskRequest);

            return response;
        } catch (Exception e) {
            log.error("Get Task By taskID request failed", e);
            throw e;
        }
    }

    @DeleteMapping("/{taskId}")
    @PreAuthorize("hasRole('USER') and @taskService.isOwner(#taskId, authentication.name) or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTask(@PathVariable Long taskId) {
        log.debug("Received DELETE Task request by taskID: {}", taskId);

        try {
            ResponseEntity<Void> response = this.deletingTask(taskId);

            return response;
        } catch (Exception e) {
            log.error("Delete Task By taskID request failed", e);
            throw e;
        }
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    public ResponseEntity<Page<TaskDTO>> getAllTasks(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        log.debug("Received request to get All Tasks");

        try {
            ResponseEntity<Page<TaskDTO>> response = this.gettingAllTasks(search, page, size, sortBy, direction);

            return response;
        } catch (Exception e) {
            log.error("Get All Tasks request failed", e);
            throw e;
        }
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    public ResponseEntity<List<TaskDTO>> getTasksByUser(@PathVariable Long userId) {
        log.debug("Received request to get All Tasks by userID: {}", userId);
        try {
            ResponseEntity<List<TaskDTO>> response = this.gettingTasksByUserId(userId);

            return response;
        } catch (Exception e) {
            log.error("Get All Tasks request failed", e);
            throw e;
        }
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
        log.debug("Received request to get All User's Tasks");

        try {
            ResponseEntity<Page<TaskDTO>> response = this.gettingAllUserTasks(userDetails.getId(), search, page, size, sortBy, direction);

            return response;
        } catch (Exception e) {
            log.error("Get All User's Tasks request failed", e);
            throw e;
        }

    }

    @PutMapping("/update-status/{taskId}")
    @PreAuthorize("(hasRole('USER') and @taskService.isOwner(#taskId, authentication.name)) or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<TaskDTO> updateTaskStatus(@PathVariable Long taskId, @Valid @RequestBody TaskStatusUpdateRequest request) {
        log.debug("Received request to Update Task Status by taskID: {}", taskId);

        try {
            ResponseEntity<TaskDTO> response = this.updatingTaskStatus(taskId, request);

            return response;
        } catch (Exception e) {
            log.error("Update Task Status request failed for taskID: {}", taskId, e);
            throw e;
        }
    }

    private ResponseEntity<TaskDTO> creatingTask(TaskRequest request) {
        TaskDTO createdTask = taskService.createTask(request);
        log.info("Successfully created Task with taskID: {}", createdTask.getId());

        return new ResponseEntity<>(createdTask, HttpStatus.CREATED);
    }

    private ResponseEntity<TaskDTO> gettingTask(Long taskId) {
        TaskDTO task = taskService.getTask(taskId);
        log.info("Successfully got Task by taskID: {}", taskId);

        return new ResponseEntity<>(task, HttpStatus.OK);
    }

    private ResponseEntity<TaskDTO> updatingTask(Long taskId, TaskRequest taskRequest) {
        TaskDTO task = taskService.updateTask(taskId, taskRequest);
        log.info("Successfully updated Task by taskID: {}", taskId);

        return new ResponseEntity<>(task, HttpStatus.OK);
    }

    private ResponseEntity<Void> deletingTask(Long taskId) {
        taskService.deleteTask(taskId);
        log.info("Successfully deleted Task by taskID: {}", taskId);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    private ResponseEntity<Page<TaskDTO>> gettingAllTasks(String search, int page, int size, String sortBy, String direction) {
        Page<TaskDTO> tasks = taskService.getAllTasks(null, search, page, size, sortBy, direction);
        log.info("Successfully retreived all Tasks");

        return new ResponseEntity<>(tasks, HttpStatus.OK);
    }

    private ResponseEntity<Page<TaskDTO>> gettingAllUserTasks(Long userId, String search, int page, int size, String sortBy, String direction) {
        Page<TaskDTO> tasks = taskService.getAllTasks(userId, search, page, size, sortBy, direction);
        log.info("Successfully retreived all User's Tasks");

        return new ResponseEntity<>(tasks, HttpStatus.OK);
    }

    private ResponseEntity<List<TaskDTO>> gettingTasksByUserId(Long userId) {
        List<TaskDTO> tasks = taskService.getTasksByUser(userId);
        log.info("Successfully retreived all Tasks by userID: {}", userId);

        return new ResponseEntity<>(tasks, HttpStatus.OK);
    }

    private ResponseEntity<TaskDTO> updatingTaskStatus(Long taskId, TaskStatusUpdateRequest request) {
        TaskDTO task = taskService.updateTaskStatus(taskId, request);
        log.info("Successfully updated Task Status for taskID: {}", taskId);

        return new ResponseEntity<>(task, HttpStatus.OK);
    }
}
