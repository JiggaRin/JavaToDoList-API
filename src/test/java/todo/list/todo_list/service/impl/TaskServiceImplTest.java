package todo.list.todo_list.service.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

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
import todo.list.todo_list.service.UserService;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final String USERNAME = "testuser";
    private static final String OTHER_USERNAME = "otheruser";
    private static final Long TASK_ID = 1L;
    private static final Long PARENT_TASK_ID = 2L;
    private static final Long CHILD_TASK_ID_1 = 2L;
    private static final Long CHILD_TASK_ID_2 = 3L;
    private static final String TITLE = "My New Task";
    private static final String UPDATED_TITLE = "My Updated Task";
    private static final String DESCRIPTION = "A simple task";
    private static final List<String> CATEGORY_NAMES = Arrays.asList("Work", "Urgent");
    private static final List<String> DUPLICATE_CATEGORY_NAMES = Arrays.asList("Cat1", "Cat1");
    private static final String CATEGORY_NAME_1 = "Work";
    private static final String CATEGORY_NAME_2 = "Urgent";
    private static final Long CATEGORY_ID_1 = 1L;
    private static final Long CATEGORY_ID_2 = 2L;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserService userService;

    @Mock
    private TaskMapper taskMapper;

    @InjectMocks
    private TaskServiceImpl taskService;

    private User defaultUser;
    private User otherUser;
    private Task defaultTask;
    private Category category1;
    private Category category2;

    @BeforeEach
    @SuppressWarnings("unused")
    void setup() {
        defaultUser = new User(USERNAME, null, null);
        defaultUser.setId(USER_ID);

        otherUser = new User(OTHER_USERNAME, null, null);
        otherUser.setId(2L);

        defaultTask = new Task(TASK_ID, defaultUser, Status.TODO);
        defaultTask.setTitle(TITLE);

        category1 = new Category(CATEGORY_NAME_1);
        category1.setId(CATEGORY_ID_1);

        category2 = new Category(CATEGORY_NAME_2);
        category2.setId(CATEGORY_ID_2);
    }

    private CreateTaskRequest setupCreateTaskRequest(String title, List<String> categoryNames, Long parentTaskId) {
        return new CreateTaskRequest(title, categoryNames, parentTaskId);
    }

    private UpdateTaskRequest setupUpdateTaskRequest(String title, List<String> categoryNames, Long parentTaskId, String description, Status status) {
        UpdateTaskRequest request = new UpdateTaskRequest(title, categoryNames, parentTaskId);
        request.setDescription(description);
        request.setStatus(status);
        return request;
    }

    private TaskStatusUpdateRequest setupTaskStatusUpdateRequest(Status status) {
        TaskStatusUpdateRequest request = new TaskStatusUpdateRequest();
        request.setStatus(status);
        return request;
    }

    private TaskDTO setupTaskDTO(Long taskId, String title, Status status, Set<String> categories) {
        TaskDTO dto = new TaskDTO(taskId, title, status);
        dto.setCategories(categories != null ? new HashSet<>(categories) : new HashSet<>());
        return dto;
    }

    private void setupSuccessfulCreateTaskMocks(CreateTaskRequest request, Task task, TaskDTO dto) {
        when(userService.getUserByUsername(USERNAME)).thenReturn(defaultUser);
        when(taskRepository.isTitleUnique(request.getTitle(), USER_ID, null)).thenReturn(true);
        when(categoryRepository.findByName(CATEGORY_NAME_1)).thenReturn(Optional.of(category1));
        when(categoryRepository.findByName(CATEGORY_NAME_2)).thenReturn(Optional.of(category2));
        when(taskMapper.createTaskFromRequest(request)).thenReturn(task);
        when(taskRepository.save(any(Task.class))).thenReturn(task);
        when(taskMapper.toTaskDTO(task)).thenReturn(dto);
    }

    private void setupSuccessfulUpdateTaskMocks(UpdateTaskRequest request, Task task, TaskDTO dto) {
        when(userService.getUserByUsername(USERNAME)).thenReturn(defaultUser);
        when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
        when(taskRepository.isTitleUnique(request.getTitle(), USER_ID, TASK_ID)).thenReturn(true);
        when(categoryRepository.findByName(CATEGORY_NAME_1)).thenReturn(Optional.of(category1));
        when(categoryRepository.findByName(CATEGORY_NAME_2)).thenReturn(Optional.of(category2));
        doNothing().when(taskMapper).updateTaskFromRequest(any(UpdateTaskRequest.class), any(Task.class));
        when(taskRepository.save(any(Task.class))).thenReturn(task);
        when(taskMapper.toTaskDTO(task)).thenReturn(dto);
    }

    private void setupSuccessfulUpdateStatusMocks(Task task, Task updatedTask, TaskDTO dto, boolean checkChildTasks) {
        when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
        if (checkChildTasks) {
            when(taskRepository.findByParentTaskId(TASK_ID)).thenReturn(Collections.emptyList());
        }
        when(taskRepository.save(any(Task.class))).thenReturn(updatedTask);
        when(taskMapper.toTaskDTO(updatedTask)).thenReturn(dto);
    }

    @Test
    @DisplayName("Create Task with valid data returns TaskDTO")
    void createTask_successfulCreation() {
        // Arrange
        try (MockedStatic<SecurityContextHolder> mockedStatic = Mockito.mockStatic(SecurityContextHolder.class)) {
            SecurityContext securityContext = Mockito.mock(SecurityContext.class);
            Authentication authentication = Mockito.mock(Authentication.class);
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn(USERNAME);

            CreateTaskRequest request = setupCreateTaskRequest(TITLE, CATEGORY_NAMES, null);
            Task task = new Task(null, defaultUser, null);
            task.setTitle(TITLE);
            task.setCategories(new HashSet<>(Arrays.asList(category1, category2)));
            TaskDTO dto = setupTaskDTO(null, TITLE, null, new HashSet<>(CATEGORY_NAMES));
            setupSuccessfulCreateTaskMocks(request, task, dto);

            // Act
            TaskDTO result = taskService.createTask(request);

            // Assert
            assertNotNull(result);
            assertEquals(TITLE, result.getTitle());
            assertEquals(new HashSet<>(CATEGORY_NAMES), result.getCategories());
            verify(userService).getUserByUsername(USERNAME);
            verify(taskRepository).isTitleUnique(TITLE, USER_ID, null);
            verify(categoryRepository).findByName(CATEGORY_NAME_1);
            verify(categoryRepository).findByName(CATEGORY_NAME_2);
            verify(taskMapper).createTaskFromRequest(request);
            verify(taskRepository).save(any(Task.class));
            verify(taskMapper).toTaskDTO(task);
        }
    }

    @Test
    @DisplayName("Create Task with non-unique title throws ResourceConflictException")
    void createTask_nonUniqueTitle_throwsException() {
        // Arrange
        try (MockedStatic<SecurityContextHolder> mockedStatic = Mockito.mockStatic(SecurityContextHolder.class)) {
            SecurityContext securityContext = Mockito.mock(SecurityContext.class);
            Authentication authentication = Mockito.mock(Authentication.class);
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn(USERNAME);

            CreateTaskRequest request = setupCreateTaskRequest(TITLE, null, null);
            when(userService.getUserByUsername(USERNAME)).thenReturn(defaultUser);
            when(taskRepository.isTitleUnique(TITLE, USER_ID, null)).thenReturn(false);

            // Act & Assert
            ResourceConflictException exception = assertThrows(
                    ResourceConflictException.class,
                    () -> taskService.createTask(request)
            );
            assertEquals("Title must be unique for the user.", exception.getMessage());
            verify(userService).getUserByUsername(USERNAME);
            verify(taskRepository).isTitleUnique(TITLE, USER_ID, null);
            this.verifyNoTaskCreation();
        }
    }

    @Test
    @DisplayName("Create Task with duplicate categories throws DuplicateCategoryException")
    void createTask_duplicateCategories_throwsException() {
        // Arrange
        try (MockedStatic<SecurityContextHolder> mockedStatic = Mockito.mockStatic(SecurityContextHolder.class)) {
            SecurityContext securityContext = Mockito.mock(SecurityContext.class);
            Authentication authentication = Mockito.mock(Authentication.class);
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn(USERNAME);

            CreateTaskRequest request = setupCreateTaskRequest(TITLE, DUPLICATE_CATEGORY_NAMES, null);
            when(userService.getUserByUsername(USERNAME)).thenReturn(defaultUser);
            when(taskRepository.isTitleUnique(TITLE, USER_ID, null)).thenReturn(true);

            // Act & Assert
            DuplicateCategoryException exception = assertThrows(
                    DuplicateCategoryException.class,
                    () -> taskService.createTask(request)
            );
            assertEquals("A task cannot have duplicate categories.", exception.getMessage());
            verify(userService).getUserByUsername(USERNAME);
            verify(taskRepository).isTitleUnique(TITLE, USER_ID, null);
            this.verifyNoTaskCreation();
        }
    }

    @Test
    @DisplayName("Create Task with non-existent parent task throws ResourceNotFoundException")
    void createTask_parentTaskNotFound_throwsException() {
        // Arrange
        try (MockedStatic<SecurityContextHolder> mockedStatic = Mockito.mockStatic(SecurityContextHolder.class)) {
            SecurityContext securityContext = Mockito.mock(SecurityContext.class);
            Authentication authentication = Mockito.mock(Authentication.class);
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn(USERNAME);

            CreateTaskRequest request = setupCreateTaskRequest(TITLE, CATEGORY_NAMES, PARENT_TASK_ID);
            Task task = new Task(null, defaultUser, null);
            when(userService.getUserByUsername(USERNAME)).thenReturn(defaultUser);
            when(taskRepository.isTitleUnique(TITLE, USER_ID, null)).thenReturn(true);
            when(taskMapper.createTaskFromRequest(request)).thenReturn(task);
            when(taskRepository.findById(PARENT_TASK_ID)).thenReturn(Optional.empty());

            // Act & Assert
            ResourceNotFoundException exception = assertThrows(
                    ResourceNotFoundException.class,
                    () -> taskService.createTask(request)
            );
            assertEquals("Parent Task not found with ID: " + PARENT_TASK_ID, exception.getMessage());
            verify(userService).getUserByUsername(USERNAME);
            verify(taskRepository).isTitleUnique(TITLE, USER_ID, null);
            verify(taskMapper).createTaskFromRequest(request);
            verify(taskRepository).findById(PARENT_TASK_ID);
            this.verifyNoTaskCreation();
        }
    }

    @Test
    @DisplayName("Create Task with parent task not owned by user throws AccessDeniedException")
    void createTask_parentTaskNotOwned_throwsException() {
        // Arrange
        try (MockedStatic<SecurityContextHolder> mockedStatic = Mockito.mockStatic(SecurityContextHolder.class)) {
            SecurityContext securityContext = Mockito.mock(SecurityContext.class);
            Authentication authentication = Mockito.mock(Authentication.class);
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn(USERNAME);

            CreateTaskRequest request = setupCreateTaskRequest(TITLE, CATEGORY_NAMES, PARENT_TASK_ID);
            Task task = new Task(null, defaultUser, null);
            Task parentTask = new Task(PARENT_TASK_ID, otherUser, null);
            when(userService.getUserByUsername(USERNAME)).thenReturn(defaultUser);
            when(taskRepository.isTitleUnique(TITLE, USER_ID, null)).thenReturn(true);
            when(taskMapper.createTaskFromRequest(request)).thenReturn(task);
            when(taskRepository.findById(PARENT_TASK_ID)).thenReturn(Optional.of(parentTask));

            // Act & Assert
            AccessDeniedException exception = assertThrows(
                    AccessDeniedException.class,
                    () -> taskService.createTask(request)
            );
            assertEquals("Parent task must belong to the authenticated user.", exception.getMessage());
            verify(userService).getUserByUsername(USERNAME);
            verify(taskRepository).isTitleUnique(TITLE, USER_ID, null);
            verify(taskMapper).createTaskFromRequest(request);
            verify(taskRepository).findById(PARENT_TASK_ID);
            this.verifyNoTaskCreation();
        }
    }

    @Test
    @DisplayName("Create Task with valid data but failed to save throws IllegalStateException")
    void createTask_failedToSave_throwsException() {
        // Arrange
        try (MockedStatic<SecurityContextHolder> mockedStatic = Mockito.mockStatic(SecurityContextHolder.class)) {
            SecurityContext securityContext = Mockito.mock(SecurityContext.class);
            Authentication authentication = Mockito.mock(Authentication.class);
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn(USERNAME);

            CreateTaskRequest request = setupCreateTaskRequest(TITLE, CATEGORY_NAMES, PARENT_TASK_ID);
            Task task = new Task(null, defaultUser, null);
            Task parentTask = new Task(PARENT_TASK_ID, defaultUser, null);
            when(userService.getUserByUsername(USERNAME)).thenReturn(defaultUser);
            when(taskRepository.isTitleUnique(TITLE, USER_ID, null)).thenReturn(true);
            when(categoryRepository.findByName(CATEGORY_NAME_1)).thenReturn(Optional.of(category1));
            when(categoryRepository.findByName(CATEGORY_NAME_2)).thenReturn(Optional.of(category2));
            when(taskMapper.createTaskFromRequest(request)).thenReturn(task);
            when(taskRepository.findById(PARENT_TASK_ID)).thenReturn(Optional.of(parentTask));
            when(taskRepository.save(any(Task.class))).thenReturn(null);

            // Act & Assert
            IllegalStateException exception = assertThrows(
                    IllegalStateException.class,
                    () -> taskService.createTask(request)
            );
            assertEquals("Failed to save task with ID: null", exception.getMessage());
            verify(userService).getUserByUsername(USERNAME);
            verify(taskRepository).isTitleUnique(TITLE, USER_ID, null);
            verify(categoryRepository).findByName(CATEGORY_NAME_1);
            verify(categoryRepository).findByName(CATEGORY_NAME_2);
            verify(taskMapper).createTaskFromRequest(request);
            verify(taskRepository).findById(PARENT_TASK_ID);
            verify(taskRepository).save(any(Task.class));
            verify(taskMapper, never()).toTaskDTO(any(Task.class));
        }
    }

    @Test
    @DisplayName("Create Task with null request throws IllegalArgumentException")
    void createTask_nullRequest_throwsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> taskService.createTask(null)
        );
        assertEquals("Task request cannot be null.", exception.getMessage());
        verify(userService, never()).getUserByUsername(anyString());
        this.verifyNoTaskCreation();
    }

    @Test
    @DisplayName("Get Task with valid ID returns TaskDTO")
    void getTask_successfulRetrieval() {
        // Arrange
        TaskDTO dto = setupTaskDTO(TASK_ID, TITLE, Status.TODO, new HashSet<>());
        when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(defaultTask));
        when(taskMapper.toTaskDTO(defaultTask)).thenReturn(dto);

        // Act
        TaskDTO result = taskService.getTask(TASK_ID);

        // Assert
        assertNotNull(result);
        assertEquals(TASK_ID, result.getId());
        assertEquals(TITLE, result.getTitle());
        assertEquals(Status.TODO, result.getStatus());
        verify(taskRepository).findById(TASK_ID);
        verify(taskMapper).toTaskDTO(defaultTask);
    }

    @Test
    @DisplayName("Get Task with null ID throws IllegalArgumentException")
    void getTask_nullTaskId_throwsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> taskService.getTask(null)
        );
        assertEquals("Task ID cannot be null.", exception.getMessage());
        verify(taskRepository, never()).findById(anyLong());
        verify(taskMapper, never()).toTaskDTO(any(Task.class));
    }

    @Test
    @DisplayName("Update Task with valid data returns TaskDTO")
    void updateTask_successfulUpdate() {
        // Arrange
        try (MockedStatic<SecurityContextHolder> mockedStatic = Mockito.mockStatic(SecurityContextHolder.class)) {
            SecurityContext securityContext = Mockito.mock(SecurityContext.class);
            Authentication authentication = Mockito.mock(Authentication.class);
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn(USERNAME);

            UpdateTaskRequest request = setupUpdateTaskRequest(UPDATED_TITLE, CATEGORY_NAMES, null, DESCRIPTION, Status.TODO);
            Task existingTask = new Task(TASK_ID, defaultUser, null);
            existingTask.setCategories(new HashSet<>(Arrays.asList(category1, category2)));
            TaskDTO dto = setupTaskDTO(TASK_ID, UPDATED_TITLE, Status.TODO, new HashSet<>(CATEGORY_NAMES));
            setupSuccessfulUpdateTaskMocks(request, existingTask, dto);

            // Act
            TaskDTO result = taskService.updateTask(TASK_ID, request);

            // Assert
            assertNotNull(result);
            assertEquals(UPDATED_TITLE, result.getTitle());
            assertEquals(new HashSet<>(CATEGORY_NAMES), result.getCategories());
            verify(userService).getUserByUsername(USERNAME);
            verify(taskRepository).findById(TASK_ID);
            verify(taskRepository).isTitleUnique(UPDATED_TITLE, USER_ID, TASK_ID);
            verify(categoryRepository).findByName(CATEGORY_NAME_1);
            verify(categoryRepository).findByName(CATEGORY_NAME_2);
            verify(taskMapper).updateTaskFromRequest(any(UpdateTaskRequest.class), any(Task.class));
            verify(taskRepository).save(any(Task.class));
            verify(taskMapper).toTaskDTO(existingTask);
        }
    }

    @Test
    @DisplayName("Update Task with non-unique title throws ResourceConflictException")
    void updateTask_nonUniqueTitle_throwsException() {
        // Arrange
        try (MockedStatic<SecurityContextHolder> mockedStatic = Mockito.mockStatic(SecurityContextHolder.class)) {
            SecurityContext securityContext = Mockito.mock(SecurityContext.class);
            Authentication authentication = Mockito.mock(Authentication.class);
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn(USERNAME);

            UpdateTaskRequest request = setupUpdateTaskRequest(UPDATED_TITLE, null, null, null, null);
            Task existingTask = new Task(TASK_ID, defaultUser, null);
            when(userService.getUserByUsername(USERNAME)).thenReturn(defaultUser);
            when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(existingTask));
            when(taskRepository.isTitleUnique(UPDATED_TITLE, USER_ID, TASK_ID)).thenReturn(false);

            // Act & Assert
            ResourceConflictException exception = assertThrows(
                    ResourceConflictException.class,
                    () -> taskService.updateTask(TASK_ID, request)
            );
            assertEquals("Title must be unique for the user.", exception.getMessage());
            verify(userService).getUserByUsername(USERNAME);
            verify(taskRepository).findById(TASK_ID);
            verify(taskRepository).isTitleUnique(UPDATED_TITLE, USER_ID, TASK_ID);
            this.verifyNoTaskUpdate();
        }
    }

    @Test
    @DisplayName("Update Task with duplicate categories throws DuplicateCategoryException")
    void updateTask_duplicateCategories_throwsException() {
        // Arrange
        try (MockedStatic<SecurityContextHolder> mockedStatic = Mockito.mockStatic(SecurityContextHolder.class)) {
            SecurityContext securityContext = Mockito.mock(SecurityContext.class);
            Authentication authentication = Mockito.mock(Authentication.class);
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn(USERNAME);

            UpdateTaskRequest request = setupUpdateTaskRequest(UPDATED_TITLE, DUPLICATE_CATEGORY_NAMES, null, null, null);
            Task existingTask = new Task(TASK_ID, defaultUser, null);
            when(userService.getUserByUsername(USERNAME)).thenReturn(defaultUser);
            when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(existingTask));
            when(taskRepository.isTitleUnique(UPDATED_TITLE, USER_ID, TASK_ID)).thenReturn(true);

            // Act & Assert
            DuplicateCategoryException exception = assertThrows(
                    DuplicateCategoryException.class,
                    () -> taskService.updateTask(TASK_ID, request)
            );
            assertEquals("A task cannot have duplicate categories.", exception.getMessage());
            verify(userService).getUserByUsername(USERNAME);
            verify(taskRepository).findById(TASK_ID);
            verify(taskRepository).isTitleUnique(UPDATED_TITLE, USER_ID, TASK_ID);
            this.verifyNoTaskUpdate();
        }
    }

    @Test
    @DisplayName("Update Task with incomplete child tasks throws CannotProceedException")
    void updateTask_incompleteChildTasks_throwsException() {
        // Arrange
        try (MockedStatic<SecurityContextHolder> mockedStatic = Mockito.mockStatic(SecurityContextHolder.class)) {
            SecurityContext securityContext = Mockito.mock(SecurityContext.class);
            Authentication authentication = Mockito.mock(Authentication.class);
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn(USERNAME);

            UpdateTaskRequest request = setupUpdateTaskRequest(UPDATED_TITLE, CATEGORY_NAMES, null, null, Status.DONE);
            Task existingTask = new Task(TASK_ID, defaultUser, null);
            Task childTask = new Task(CHILD_TASK_ID_1, defaultUser, Status.TODO);
            List<Task> childTasks = Arrays.asList(childTask);
            when(userService.getUserByUsername(USERNAME)).thenReturn(defaultUser);
            when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(existingTask));
            when(taskRepository.isTitleUnique(UPDATED_TITLE, USER_ID, TASK_ID)).thenReturn(true);
            when(categoryRepository.findByName(CATEGORY_NAME_1)).thenReturn(Optional.of(category1));
            when(categoryRepository.findByName(CATEGORY_NAME_2)).thenReturn(Optional.of(category2));
            when(taskRepository.findByParentTaskId(TASK_ID)).thenReturn(childTasks);

            // Act & Assert
            CannotProceedException exception = assertThrows(
                    CannotProceedException.class,
                    () -> taskService.updateTask(TASK_ID, request)
            );
            assertEquals("Cannot proceed with task " + TASK_ID + " while child tasks are not completed.", exception.getMessage());
            verify(userService).getUserByUsername(USERNAME);
            verify(taskRepository).findById(TASK_ID);
            verify(taskRepository).isTitleUnique(UPDATED_TITLE, USER_ID, TASK_ID);
            verify(categoryRepository).findByName(CATEGORY_NAME_1);
            verify(categoryRepository).findByName(CATEGORY_NAME_2);
            verify(taskRepository).findByParentTaskId(TASK_ID);
            this.verifyNoTaskUpdate();
        }
    }

    @Test
    @DisplayName("Update Task with non-existent parent task throws ResourceNotFoundException")
    void updateTask_parentTaskNotFound_throwsException() {
        // Arrange
        try (MockedStatic<SecurityContextHolder> mockedStatic = Mockito.mockStatic(SecurityContextHolder.class)) {
            SecurityContext securityContext = Mockito.mock(SecurityContext.class);
            Authentication authentication = Mockito.mock(Authentication.class);
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn(USERNAME);

            UpdateTaskRequest request = setupUpdateTaskRequest(UPDATED_TITLE, CATEGORY_NAMES, PARENT_TASK_ID, null, null);
            Task existingTask = new Task(TASK_ID, defaultUser, null);
            when(userService.getUserByUsername(USERNAME)).thenReturn(defaultUser);
            when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(existingTask));
            when(taskRepository.isTitleUnique(UPDATED_TITLE, USER_ID, TASK_ID)).thenReturn(true);
            when(categoryRepository.findByName(CATEGORY_NAME_1)).thenReturn(Optional.of(category1));
            when(categoryRepository.findByName(CATEGORY_NAME_2)).thenReturn(Optional.of(category2));
            when(taskRepository.findById(PARENT_TASK_ID)).thenReturn(Optional.empty());

            // Act & Assert
            ResourceNotFoundException exception = assertThrows(
                    ResourceNotFoundException.class,
                    () -> taskService.updateTask(TASK_ID, request)
            );
            assertEquals("Parent Task not found with ID: " + PARENT_TASK_ID, exception.getMessage());
            verify(userService).getUserByUsername(USERNAME);
            verify(taskRepository).findById(TASK_ID);
            verify(taskRepository).isTitleUnique(UPDATED_TITLE, USER_ID, TASK_ID);
            verify(categoryRepository).findByName(CATEGORY_NAME_1);
            verify(categoryRepository).findByName(CATEGORY_NAME_2);
            verify(taskRepository).findById(PARENT_TASK_ID);
            this.verifyNoTaskUpdate();
        }
    }

    @Test
    @DisplayName("Update Task with parent task not owned by user throws AccessDeniedException")
    void updateTask_parentTaskNotOwned_throwsException() {
        // Arrange
        try (MockedStatic<SecurityContextHolder> mockedStatic = Mockito.mockStatic(SecurityContextHolder.class)) {
            SecurityContext securityContext = Mockito.mock(SecurityContext.class);
            Authentication authentication = Mockito.mock(Authentication.class);
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn(USERNAME);

            UpdateTaskRequest request = setupUpdateTaskRequest(UPDATED_TITLE, CATEGORY_NAMES, PARENT_TASK_ID, null, null);
            Task existingTask = new Task(TASK_ID, defaultUser, null);
            Task parentTask = new Task(PARENT_TASK_ID, otherUser, null);
            when(userService.getUserByUsername(USERNAME)).thenReturn(defaultUser);
            when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(existingTask));
            when(taskRepository.isTitleUnique(UPDATED_TITLE, USER_ID, TASK_ID)).thenReturn(true);
            when(categoryRepository.findByName(CATEGORY_NAME_1)).thenReturn(Optional.of(category1));
            when(categoryRepository.findByName(CATEGORY_NAME_2)).thenReturn(Optional.of(category2));
            when(taskRepository.findById(PARENT_TASK_ID)).thenReturn(Optional.of(parentTask));

            // Act & Assert
            AccessDeniedException exception = assertThrows(
                    AccessDeniedException.class,
                    () -> taskService.updateTask(TASK_ID, request)
            );
            assertEquals("Parent task must belong to the authenticated user.", exception.getMessage());
            verify(userService).getUserByUsername(USERNAME);
            verify(taskRepository).findById(TASK_ID);
            verify(taskRepository).isTitleUnique(UPDATED_TITLE, USER_ID, TASK_ID);
            verify(categoryRepository).findByName(CATEGORY_NAME_1);
            verify(categoryRepository).findByName(CATEGORY_NAME_2);
            verify(taskRepository).findById(PARENT_TASK_ID);
            this.verifyNoTaskUpdate();
        }
    }

    @Test
    @DisplayName("Update Task with valid data but failed to save throws IllegalStateException")
    void updateTask_failedToSave_throwsException() {
        // Arrange
        try (MockedStatic<SecurityContextHolder> mockedStatic = Mockito.mockStatic(SecurityContextHolder.class)) {
            SecurityContext securityContext = Mockito.mock(SecurityContext.class);
            Authentication authentication = Mockito.mock(Authentication.class);
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn(USERNAME);

            UpdateTaskRequest request = setupUpdateTaskRequest(UPDATED_TITLE, CATEGORY_NAMES, PARENT_TASK_ID, null, null);
            Task existingTask = new Task(TASK_ID, defaultUser, null);
            Task parentTask = new Task(PARENT_TASK_ID, defaultUser, null);
            when(userService.getUserByUsername(USERNAME)).thenReturn(defaultUser);
            when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(existingTask));
            when(taskRepository.isTitleUnique(UPDATED_TITLE, USER_ID, TASK_ID)).thenReturn(true);
            when(categoryRepository.findByName(CATEGORY_NAME_1)).thenReturn(Optional.of(category1));
            when(categoryRepository.findByName(CATEGORY_NAME_2)).thenReturn(Optional.of(category2));
            doNothing().when(taskMapper).updateTaskFromRequest(any(UpdateTaskRequest.class), any(Task.class));
            when(taskRepository.findById(PARENT_TASK_ID)).thenReturn(Optional.of(parentTask));
            when(taskRepository.save(any(Task.class))).thenReturn(null);

            // Act & Assert
            IllegalStateException exception = assertThrows(
                    IllegalStateException.class,
                    () -> taskService.updateTask(TASK_ID, request)
            );
            assertEquals("Failed to save task with ID: " + TASK_ID, exception.getMessage());
            verify(userService).getUserByUsername(USERNAME);
            verify(taskRepository).findById(TASK_ID);
            verify(taskRepository).isTitleUnique(UPDATED_TITLE, USER_ID, TASK_ID);
            verify(categoryRepository).findByName(CATEGORY_NAME_1);
            verify(categoryRepository).findByName(CATEGORY_NAME_2);
            verify(taskMapper).updateTaskFromRequest(any(UpdateTaskRequest.class), any(Task.class));
            verify(taskRepository).findById(PARENT_TASK_ID);
            verify(taskRepository).save(any(Task.class));
            verify(taskMapper, never()).toTaskDTO(any(Task.class));
        }
    }

    @Test
    @DisplayName("Update Task with null ID throws IllegalArgumentException")
    void updateTask_nullTaskId_throwsException() {
        // Arrange
        UpdateTaskRequest request = setupUpdateTaskRequest(UPDATED_TITLE, null, null, null, null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> taskService.updateTask(null, request)
        );
        assertEquals("Task ID cannot be null.", exception.getMessage());
        verify(taskRepository, never()).findById(anyLong());
        this.verifyNoTaskUpdate();
    }

    @Test
    @DisplayName("Update Task with null request throws IllegalArgumentException")
    void updateTask_nullRequest_throwsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> taskService.updateTask(TASK_ID, null)
        );
        assertEquals("Task request cannot be null.", exception.getMessage());
        verify(taskRepository, never()).findById(anyLong());
        this.verifyNoTaskUpdate();
    }

    @Test
    @DisplayName("Update Task Status to DONE with valid data returns TaskDTO")
    void updateTaskStatus_toDone_successful() {
        // Arrange
        TaskStatusUpdateRequest request = setupTaskStatusUpdateRequest(Status.DONE);
        Task updatedTask = new Task(TASK_ID, defaultUser, Status.DONE);
        TaskDTO dto = setupTaskDTO(TASK_ID, TITLE, Status.DONE, new HashSet<>());
        setupSuccessfulUpdateStatusMocks(defaultTask, updatedTask, dto, true);

        // Act
        TaskDTO result = taskService.updateTaskStatus(TASK_ID, request);

        // Assert
        assertNotNull(result);
        assertEquals(Status.DONE, result.getStatus());
        assertEquals(TASK_ID, result.getId());
        verify(taskRepository).findById(TASK_ID);
        verify(taskRepository).findByParentTaskId(TASK_ID);
        verify(taskRepository).save(any(Task.class));
        verify(taskMapper).toTaskDTO(updatedTask);
    }

    @Test
    @DisplayName("Update Task Status to IN_PROGRESS with valid data returns TaskDTO")
    void updateTaskStatus_toInProgress_successful() {
        // Arrange
        TaskStatusUpdateRequest request = setupTaskStatusUpdateRequest(Status.IN_PROGRESS);
        Task updatedTask = new Task(TASK_ID, defaultUser, Status.IN_PROGRESS);
        TaskDTO dto = setupTaskDTO(TASK_ID, TITLE, Status.IN_PROGRESS, new HashSet<>());
        setupSuccessfulUpdateStatusMocks(defaultTask, updatedTask, dto, false);

        // Act
        TaskDTO result = taskService.updateTaskStatus(TASK_ID, request);

        // Assert
        assertNotNull(result);
        assertEquals(Status.IN_PROGRESS, result.getStatus());
        assertEquals(TASK_ID, result.getId());
        verify(taskRepository).findById(TASK_ID);
        verify(taskRepository, never()).findByParentTaskId(anyLong());
        verify(taskRepository).save(any(Task.class));
        verify(taskMapper).toTaskDTO(updatedTask);
    }

    @Test
    @DisplayName("Update Task Status with non-existent task throws ResourceNotFoundException")
    void updateTaskStatus_taskNotFound_throwsException() {
        // Arrange
        TaskStatusUpdateRequest request = setupTaskStatusUpdateRequest(Status.DONE);
        when(taskRepository.findById(TASK_ID)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> taskService.updateTaskStatus(TASK_ID, request)
        );
        assertEquals("Task not found with ID: " + TASK_ID, exception.getMessage());
        verify(taskRepository).findById(TASK_ID);
        this.verifyNoTaskStatusUpdate();
    }

    @Test
    @DisplayName("Update Task Status with incomplete child tasks throws CannotProceedException")
    void updateTaskStatus_incompleteChildTasks_throwsException() {
        // Arrange
        TaskStatusUpdateRequest request = setupTaskStatusUpdateRequest(Status.DONE);
        Task childTask = new Task(CHILD_TASK_ID_1, defaultUser, Status.TODO);
        List<Task> childTasks = Arrays.asList(childTask);
        when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(defaultTask));
        when(taskRepository.findByParentTaskId(TASK_ID)).thenReturn(childTasks);

        // Act & Assert
        CannotProceedException exception = assertThrows(
                CannotProceedException.class,
                () -> taskService.updateTaskStatus(TASK_ID, request)
        );
        assertEquals("Cannot proceed with task " + TASK_ID + " while child tasks are not completed.", exception.getMessage());
        verify(taskRepository).findById(TASK_ID);
        verify(taskRepository).findByParentTaskId(TASK_ID);
        this.verifyNoTaskStatusUpdate();
    }

    @Test
    @DisplayName("Update Task Status with null ID throws IllegalArgumentException")
    void updateTaskStatus_nullTaskId_throwsException() {
        // Arrange
        TaskStatusUpdateRequest request = setupTaskStatusUpdateRequest(Status.DONE);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> taskService.updateTaskStatus(null, request)
        );
        assertEquals("Task ID cannot be null.", exception.getMessage());
        verify(taskRepository, never()).findById(anyLong());
        this.verifyNoTaskStatusUpdate();
    }

    @Test
    @DisplayName("Update Task Status with null request throws IllegalArgumentException")
    void updateTaskStatus_nullRequest_throwsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> taskService.updateTaskStatus(TASK_ID, null)
        );
        assertEquals("Task Status Update request cannot be null.", exception.getMessage());
        verify(taskRepository, never()).findById(anyLong());
        this.verifyNoTaskStatusUpdate();
    }

    @Test
    @DisplayName("Delete Task with child tasks succeeds")
    void deleteTask_withChildTasks_successful() {
        // Arrange
        Task childTask1 = new Task(CHILD_TASK_ID_1, defaultUser, Status.DONE);
        Task childTask2 = new Task(CHILD_TASK_ID_2, defaultUser, Status.DONE);
        List<Task> childTasks = Arrays.asList(childTask1, childTask2);
        when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(defaultTask));
        when(taskRepository.findByParentTaskId(TASK_ID)).thenReturn(childTasks);

        // Act
        taskService.deleteTask(TASK_ID);

        // Assert
        verify(taskRepository).findById(TASK_ID);
        verify(taskRepository, times(2)).findByParentTaskId(TASK_ID); // Once in deleteTask, once in validateChildTaskCompletion
        verify(taskRepository).delete(childTask1);
        verify(taskRepository).delete(childTask2);
        verify(taskRepository).delete(defaultTask);
    }

    @Test
    @DisplayName("Delete Task without child tasks succeeds")
    void deleteTask_noChildTasks_successful() {
        // Arrange
        when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(defaultTask));
        when(taskRepository.findByParentTaskId(TASK_ID)).thenReturn(Collections.emptyList());

        // Act
        taskService.deleteTask(TASK_ID);

        // Assert
        verify(taskRepository).findById(TASK_ID);
        verify(taskRepository).findByParentTaskId(TASK_ID);
        verify(taskRepository).delete(defaultTask);
    }

    @Test
    @DisplayName("Delete Task with incomplete child tasks throws CannotProceedException")
    void deleteTask_incompleteChildTasks_throwsException() {
        // Arrange
        Task childTask1 = new Task(CHILD_TASK_ID_1, defaultUser, Status.TODO);
        Task childTask2 = new Task(CHILD_TASK_ID_2, defaultUser, Status.IN_PROGRESS);
        List<Task> childTasks = Arrays.asList(childTask1, childTask2);
        when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(defaultTask));
        when(taskRepository.findByParentTaskId(TASK_ID)).thenReturn(childTasks);

        // Act & Assert
        CannotProceedException exception = assertThrows(
                CannotProceedException.class,
                () -> taskService.deleteTask(TASK_ID)
        );
        assertEquals("Cannot proceed with task " + TASK_ID + " while child tasks are not completed.", exception.getMessage());
        verify(taskRepository).findById(TASK_ID);
        verify(taskRepository, times(2)).findByParentTaskId(TASK_ID); // Once in deleteTask, once in validateChildTaskCompletion
        verify(taskRepository, never()).delete(any(Task.class));
    }

    @Test
    @DisplayName("Delete Task with non-existent ID throws ResourceNotFoundException")
    void deleteTask_taskNotFound_throwsException() {
        // Arrange
        when(taskRepository.findById(TASK_ID)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> taskService.deleteTask(TASK_ID)
        );
        assertEquals("Task not found with ID: " + TASK_ID, exception.getMessage());
        verify(taskRepository).findById(TASK_ID);
        verify(taskRepository, never()).findByParentTaskId(anyLong());
        verify(taskRepository, never()).delete(any(Task.class));
    }

    @Test
    @DisplayName("Delete Task with null ID throws IllegalArgumentException")
    void deleteTask_nullTaskId_throwsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> taskService.deleteTask(null)
        );
        assertEquals("Task ID cannot be null.", exception.getMessage());
        verify(taskRepository, never()).findById(anyLong());
        verify(taskRepository, never()).delete(any(Task.class));
    }

    @Test
    @DisplayName("Get All Tasks with null sort by throws IllegalArgumentException")
    void getAllTasks_nullSortBy_throwsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> taskService.getAllTasks(USER_ID, "search", 0, 10, null, "asc")
        );
        assertEquals("Sort by field cannot be null.", exception.getMessage());
        verify(taskRepository, never()).findParentTasks(any(), any(), any());
        verify(taskMapper, never()).toTaskDTO(any(Task.class));
    }

    @Test
    @DisplayName("Get All Tasks with null direction throws IllegalArgumentException")
    void getAllTasks_nullDirection_throwsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> taskService.getAllTasks(USER_ID, "search", 0, 10, "title", null)
        );
        assertEquals("Sort direction cannot be null.", exception.getMessage());
        verify(taskRepository, never()).findParentTasks(any(), any(), any());
        verify(taskMapper, never()).toTaskDTO(any(Task.class));
    }

    @Test
    @DisplayName("Get Tasks By User with non-existent user throws ResourceNotFoundException")
    void getTasksByUser_userNotFound_throwsException() {
        // Arrange
        when(userService.getUserById(USER_ID)).thenThrow(new ResourceNotFoundException("User not found with ID: " + USER_ID));

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> taskService.getTasksByUser(USER_ID)
        );
        assertEquals("User not found with ID: " + USER_ID, exception.getMessage());
        verify(userService).getUserById(USER_ID);
        verify(taskRepository, never()).findByOwner(any(User.class));
        verify(taskMapper, never()).toTaskDTO(any(Task.class));
    }

    private void verifyNoTaskCreation() {
        verify(categoryRepository, never()).findByName(anyString());
        verify(taskRepository, never()).save(any(Task.class));
        verify(taskMapper, never()).toTaskDTO(any(Task.class));
    }

    private void verifyNoTaskUpdate() {
        verify(taskMapper, never()).updateTaskFromRequest(any(), any());
        verify(taskRepository, never()).save(any(Task.class));
        verify(taskMapper, never()).toTaskDTO(any(Task.class));
    }

    private void verifyNoTaskStatusUpdate() {
        verify(taskRepository, never()).save(any(Task.class));
        verify(taskMapper, never()).toTaskDTO(any(Task.class));
    }
}