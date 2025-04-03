package todo.list.todo_list.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

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
import todo.list.todo_list.mapper.TaskMapper;
import todo.list.todo_list.model.Status;
import todo.list.todo_list.repository.CategoryRepository;
import todo.list.todo_list.repository.TaskRepository;
import todo.list.todo_list.service.UserService;

class TaskServiceImplTest {

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

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createTask_successfulCreation() {
        TaskRequest request = new TaskRequest();
        request.setTitle("My New Task");
        request.setDescription("A simple task");
        request.setStatus(Status.TODO);
        request.setCategoryNames(Arrays.asList("Work", "Urgent"));
        request.setParentId(null);

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        Category work = new Category("Work");
        work.setId(1L);
        Category urgent = new Category("Urgent");
        urgent.setId(2L);
        Set<Category> categories = new HashSet<>(Arrays.asList(work, urgent));

        Task task = new Task();
        task.setOwner(user);
        task.setCategories(categories);

        TaskDTO dto = new TaskDTO();
        dto.setId(1L);
        dto.setTitle("My New Task");
        dto.setCategories(new HashSet<>(Arrays.asList("Work", "Urgent")));

        try (MockedStatic<SecurityContextHolder> mockedStatic = Mockito.mockStatic(SecurityContextHolder.class)) {
            SecurityContext securityContext = mock(SecurityContext.class);
            Authentication authentication = mock(Authentication.class);
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("testuser");

            when(userService.getUserByUsername("testuser")).thenReturn(user);
            when(taskRepository.isTitleUnique(request.getTitle(), user.getId(), null)).thenReturn(true);
            when(categoryRepository.findByName("Work")).thenReturn(Optional.of(work));
            when(categoryRepository.findByName("Urgent")).thenReturn(Optional.of(urgent));
            when(taskMapper.fromTaskRequest(request)).thenReturn(task);
            when(taskRepository.save(task)).thenReturn(task);
            when(taskMapper.toTaskDTO(task)).thenReturn(dto);

            TaskDTO result = taskService.createTask(request);
            assertNotNull(result);
            assertEquals(request.getTitle(), result.getTitle());
            assertEquals(new HashSet<>(request.getCategoryNames()), result.getCategories());

            verify(taskRepository).save(task);
        }
    }

    @Test
    void createTask_titleIsNotUnique_throwsException() {
        TaskRequest request = new TaskRequest();
        request.setTitle("My New Task");

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        try (MockedStatic<SecurityContextHolder> mockedStatic = Mockito.mockStatic(SecurityContextHolder.class)) {
            SecurityContext securityContext = mock(SecurityContext.class);
            Authentication authentication = mock(Authentication.class);
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("testuser");
            when(userService.getUserByUsername("testuser")).thenReturn(user);
            when(taskRepository.isTitleUnique(request.getTitle(), user.getId(), null)).thenReturn(false);

            ResourceConflictException exception = assertThrows(ResourceConflictException.class, () -> {
                taskService.createTask(request);
            });

            assertEquals("Title must be unique for the user", exception.getMessage());

            verify(userService).getUserByUsername("testuser");
            verify(taskRepository).isTitleUnique(request.getTitle(), user.getId(), null);
            verify(taskMapper, never()).fromTaskRequest(request);
            verify(taskRepository, never()).save(any(Task.class));
            verify(taskMapper, never()).toTaskDTO(any(Task.class));
        }
    }

    @Test
    void createTask_duplicateCategories_throwsException() {
        TaskRequest request = new TaskRequest();
        request.setTitle("My New Task");
        request.setCategoryNames(Arrays.asList("Cat1", "Cat1"));

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        try (MockedStatic<SecurityContextHolder> mockedStatic = Mockito.mockStatic(SecurityContextHolder.class)) {
            SecurityContext securityContext = mock(SecurityContext.class);
            Authentication authentication = mock(Authentication.class);
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("testuser");
            when(userService.getUserByUsername("testuser")).thenReturn(user);
            when(taskRepository.isTitleUnique(request.getTitle(), user.getId(), null)).thenReturn(true);

            TaskServiceImpl taskServiceSpy = spy(taskService);
            when(taskServiceSpy.hasDuplicateCategories(request.getCategoryNames())).thenReturn(true);

            DuplicateCategoryException exception = assertThrows(DuplicateCategoryException.class, () -> {
                taskServiceSpy.createTask(request);
            });

            assertEquals("A task cannot have duplicate categories.", exception.getMessage());

            verify(userService).getUserByUsername("testuser");
            verify(taskRepository).isTitleUnique(request.getTitle(), user.getId(), null);
            verify(taskServiceSpy).hasDuplicateCategories(request.getCategoryNames());
            verify(taskMapper, never()).fromTaskRequest(request);
            verify(taskRepository, never()).save(any(Task.class));
            verify(taskMapper, never()).toTaskDTO(any(Task.class));
        }
    }

    @Test
    void createTask_parentTaskNotFound_throwsException() {
        TaskRequest request = new TaskRequest();
        request.setTitle("My New Task");
        request.setParentId(2L);
        request.setCategoryNames(Arrays.asList("Cat1", "Cat2"));

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        Task task = new Task();
        task.setOwner(user);

        try (MockedStatic<SecurityContextHolder> mockedStatic = Mockito.mockStatic(SecurityContextHolder.class)) {
            SecurityContext securityContext = mock(SecurityContext.class);
            Authentication authentication = mock(Authentication.class);
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("testuser");
            when(userService.getUserByUsername("testuser")).thenReturn(user);
            when(taskRepository.isTitleUnique(request.getTitle(), user.getId(), null)).thenReturn(true);
            when(taskMapper.fromTaskRequest(request)).thenReturn(task);

            when(taskRepository.findById(request.getParentId())).thenReturn(Optional.empty());

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
                taskService.createTask(request);
            });

            assertEquals("Parent Task not found with ID: " + request.getParentId(), exception.getMessage());

            verify(userService).getUserByUsername("testuser");
            verify(taskRepository).isTitleUnique(request.getTitle(), user.getId(), null);
            verify(taskMapper).fromTaskRequest(request);
            verify(taskRepository).findById(request.getParentId());

            verify(categoryRepository, never()).findByName(anyString());
            verify(taskRepository, never()).save(any(Task.class));
            verify(taskMapper, never()).toTaskDTO(any(Task.class));
        }
    }

    @Test
    void createTask_parentTaskNotOwnedByUser_throwsException() {
        TaskRequest request = new TaskRequest();
        request.setTitle("My New Task");
        request.setParentId(2L);
        request.setCategoryNames(Arrays.asList("Cat1", "Cat2"));

        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setUsername("testuser");

        User otherUser = new User();
        otherUser.setId(3L);
        otherUser.setUsername("otheruser");

        Task parentTask = new Task();
        parentTask.setOwner(otherUser);

        Task task = new Task();
        task.setOwner(currentUser);

        try (MockedStatic<SecurityContextHolder> mockedStatic = Mockito.mockStatic(SecurityContextHolder.class)) {
            SecurityContext securityContext = mock(SecurityContext.class);
            Authentication authentication = mock(Authentication.class);
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("testuser");
            when(userService.getUserByUsername("testuser")).thenReturn(currentUser);
            when(taskRepository.isTitleUnique(request.getTitle(), currentUser.getId(), null)).thenReturn(true);
            when(taskMapper.fromTaskRequest(request)).thenReturn(task);
            when(taskRepository.findById(request.getParentId())).thenReturn(Optional.of(parentTask));

            AccessDeniedException exception = assertThrows(AccessDeniedException.class, () -> {
                taskService.createTask(request);
            });

            assertEquals("Parent task must belong to the authenticated user.", exception.getMessage());

            verify(userService).getUserByUsername("testuser");
            verify(taskRepository).isTitleUnique(request.getTitle(), currentUser.getId(), null);
            verify(taskMapper).fromTaskRequest(request);
            verify(taskRepository).findById(request.getParentId());
            verify(taskRepository, never()).save(any(Task.class));
            verify(taskMapper, never()).toTaskDTO(any(Task.class));
        }
    }

    @Test
    void createTask_failedToSaveTask_throwsException() {
        TaskRequest request = new TaskRequest();
        request.setTitle("My New Task");
        request.setCategoryNames(Arrays.asList("Work", "Urgent"));
        request.setParentId(1L);

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        Task task = new Task();
        task.setOwner(user);

        Task parentTask = new Task();
        parentTask.setOwner(user);

        Category work = new Category("Work");
        work.setId(1L);
        Category urgent = new Category("Urgent");
        urgent.setId(2L);

        try (MockedStatic<SecurityContextHolder> mockedStatic = Mockito.mockStatic(SecurityContextHolder.class)) {
            SecurityContext securityContext = mock(SecurityContext.class);
            Authentication authentication = mock(Authentication.class);
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("testuser");

            when(userService.getUserByUsername("testuser")).thenReturn(user);
            when(taskRepository.isTitleUnique(request.getTitle(), user.getId(), null)).thenReturn(true);
            when(categoryRepository.findByName("Work")).thenReturn(Optional.of(work));
            when(categoryRepository.findByName("Urgent")).thenReturn(Optional.of(urgent));
            when(taskMapper.fromTaskRequest(request)).thenReturn(task);
            when(taskRepository.findById(request.getParentId())).thenReturn(Optional.of(parentTask));
            when(taskRepository.save(task)).thenReturn(null);

            IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
                taskService.createTask(request);
            });

            assertEquals("Failed to save task", exception.getMessage());

            verify(userService).getUserByUsername("testuser");
            verify(taskRepository).isTitleUnique(request.getTitle(), user.getId(), null);
            verify(taskMapper).fromTaskRequest(request);
            verify(taskRepository).findById(request.getParentId());
            verify(taskRepository).save(any(Task.class));
            verify(taskMapper, never()).toTaskDTO(any(Task.class));
        }
    }

    @Test
    void updateTask_successfulUpdate() {
        TaskRequest request = new TaskRequest();
        request.setTitle("My Updated Task");
        request.setDescription("A simple task");
        request.setStatus(Status.TODO);
        request.setCategoryNames(Arrays.asList("Work", "Urgent"));
        request.setParentId(null);

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        Category work = new Category("Work");
        work.setId(1L);
        Category urgent = new Category("Urgent");
        urgent.setId(2L);
        Set<Category> categories = new HashSet<>(Arrays.asList(work, urgent));

        Long taskId = 1L;
        Task existedTask = new Task();
        existedTask.setId(taskId);
        existedTask.setOwner(user);
        existedTask.setCategories(categories);

        TaskDTO dto = new TaskDTO();
        dto.setId(1L);
        dto.setTitle("My Updated Task");
        dto.setCategories(new HashSet<>(Arrays.asList("Work", "Urgent")));

        try (MockedStatic<SecurityContextHolder> mockedStatic = Mockito.mockStatic(SecurityContextHolder.class)) {
            SecurityContext securityContext = mock(SecurityContext.class);
            Authentication authentication = mock(Authentication.class);
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("testuser");

            when(userService.getUserByUsername("testuser")).thenReturn(user);
            when(taskRepository.findById(taskId)).thenReturn(Optional.of(existedTask));
            when(taskRepository.isTitleUnique(request.getTitle(), user.getId(), taskId)).thenReturn(true);
            when(categoryRepository.findByName("Work")).thenReturn(Optional.of(work));
            when(categoryRepository.findByName("Urgent")).thenReturn(Optional.of(urgent));
            doNothing().when(taskMapper).updateTaskFromRequest(request, existedTask);
            when(taskRepository.save(existedTask)).thenReturn(existedTask);
            when(taskMapper.toTaskDTO(existedTask)).thenReturn(dto);

            TaskDTO result = taskService.updateTask(taskId, request);
            assertNotNull(result);
            assertEquals(request.getTitle(), result.getTitle());
            assertEquals(new HashSet<>(request.getCategoryNames()), result.getCategories());

            verify(taskRepository).save(existedTask);
        }
    }

    @Test
    void updateTask_titleIsNotUnique_throwsException() {
        TaskRequest request = new TaskRequest();
        request.setTitle("My Updated Task");

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        Long taskId = 1L;
        Task existedTask = new Task();
        existedTask.setId(taskId);
        existedTask.setOwner(user);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(existedTask));
        when(taskRepository.isTitleUnique(request.getTitle(), user.getId(), taskId)).thenReturn(false);

        ResourceConflictException exception = assertThrows(ResourceConflictException.class, () -> {
            taskService.updateTask(taskId, request);
        });

        assertEquals("Title must be unique for the user.", exception.getMessage());

        verify(taskRepository).findById(taskId);
        verify(taskRepository).isTitleUnique(request.getTitle(), user.getId(), taskId);
        verify(taskMapper, never()).updateTaskFromRequest(request, existedTask);
        verify(taskRepository, never()).save(any(Task.class));
        verify(taskMapper, never()).toTaskDTO(any(Task.class));
    }

    @Test
    void updateTask_duplicateCategories_throwsException() {
        TaskRequest request = new TaskRequest();
        request.setTitle("My Updated Task");
        request.setCategoryNames(Arrays.asList("Cat1", "Cat1"));

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        Long taskId = 1L;
        Task existedTask = new Task();
        existedTask.setId(taskId);
        existedTask.setOwner(user);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(existedTask));
        when(taskRepository.isTitleUnique(request.getTitle(), user.getId(), taskId)).thenReturn(true);

        TaskServiceImpl taskServiceSpy = spy(taskService);
        when(taskServiceSpy.hasDuplicateCategories(request.getCategoryNames())).thenReturn(true);

        DuplicateCategoryException exception = assertThrows(DuplicateCategoryException.class, () -> {
            taskServiceSpy.updateTask(taskId, request);
        });

        assertEquals("A task cannot have duplicate categories.", exception.getMessage());

        verify(taskRepository).findById(taskId);
        verify(taskRepository).isTitleUnique(request.getTitle(), user.getId(), taskId);
        verify(taskServiceSpy).hasDuplicateCategories(request.getCategoryNames());
        verify(taskMapper, never()).updateTaskFromRequest(request, existedTask);
        verify(taskRepository, never()).save(any(Task.class));
        verify(taskMapper, never()).toTaskDTO(any(Task.class));
    }

    @Test
    void updateTask_childTaskNotCompleted_throwsException() {
        TaskRequest request = new TaskRequest();
        request.setTitle("My Updated Task");
        request.setStatus(Status.DONE);
        request.setCategoryNames(Arrays.asList("Cat1", "Cat1"));

        User user = new User();
        user.setId(1L);

        Long taskId = 1L;
        Task existedTask = new Task();
        existedTask.setId(taskId);
        existedTask.setOwner(user);

        Task childTask = new Task();
        childTask.setId(2L);
        childTask.setOwner(user);
        childTask.setStatus(Status.TODO);

        List<Task> childTasks = Arrays.asList(childTask);
        TaskServiceImpl taskServiceSpy = spy(taskService);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(existedTask));
        when(taskRepository.isTitleUnique(request.getTitle(), user.getId(), taskId)).thenReturn(true);
        when(taskServiceSpy.hasDuplicateCategories(request.getCategoryNames())).thenReturn(false);
        when(taskRepository.findByParentTaskId(taskId)).thenReturn(childTasks);

        CannotProceedException exception = assertThrows(CannotProceedException.class, () -> {
            taskServiceSpy.updateTask(taskId, request);
        });

        assertEquals("Cannot proceed with task " + taskId + " while child tasks are not completed.", exception.getMessage());

        verify(taskRepository).findById(taskId);
        verify(taskRepository).isTitleUnique(request.getTitle(), user.getId(), taskId);
        verify(taskRepository).findByParentTaskId(taskId);
        verify(taskMapper, never()).updateTaskFromRequest(request, existedTask);
    }

    @Test
    void updateTask_parentTaskNotFound_throwsException() {
        TaskRequest request = new TaskRequest();
        request.setTitle("My Updated Task");
        request.setParentId(2L);
        request.setCategoryNames(Arrays.asList("Cat1", "Cat2"));

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        Long taskId = 1L;
        Task existedTask = new Task();
        existedTask.setId(taskId);
        existedTask.setOwner(user);

        try (MockedStatic<SecurityContextHolder> mockedStatic = Mockito.mockStatic(SecurityContextHolder.class)) {
            SecurityContext securityContext = mock(SecurityContext.class);
            Authentication authentication = mock(Authentication.class);
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("testuser");
            when(userService.getUserByUsername("testuser")).thenReturn(user);

            when(taskRepository.findById(taskId)).thenReturn(Optional.of(existedTask));
            when(taskRepository.isTitleUnique(request.getTitle(), user.getId(), taskId)).thenReturn(true);
            doNothing().when(taskMapper).updateTaskFromRequest(request, existedTask);

            when(taskRepository.findById(request.getParentId())).thenReturn(Optional.empty());

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
                taskService.updateTask(taskId, request);
            });

            assertEquals("Parent Task not found with ID: " + request.getParentId(), exception.getMessage());

            verify(userService).getUserByUsername("testuser");
            verify(taskRepository).findById(taskId);
            verify(taskRepository).isTitleUnique(request.getTitle(), user.getId(), taskId);
            verify(taskMapper).updateTaskFromRequest(request, existedTask);
            verify(taskRepository).findById(request.getParentId());
            verify(taskRepository, never()).save(any(Task.class));
            verify(taskMapper, never()).toTaskDTO(any(Task.class));
        }
    }

    @Test
    void updateTask_parentTaskNotOwnedByUser_throwsException() {
        TaskRequest request = new TaskRequest();
        request.setTitle("My Updated Task");
        request.setParentId(2L);
        request.setCategoryNames(Arrays.asList("Cat1", "Cat2"));

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        User otherUser = new User();
        otherUser.setId(3L);
        otherUser.setUsername("otheruser");

        Long taskId = 1L;
        Task existedTask = new Task();
        existedTask.setId(taskId);
        existedTask.setOwner(user);

        Task parentTask = new Task();
        parentTask.setId(2L);
        parentTask.setOwner(otherUser);

        try (MockedStatic<SecurityContextHolder> mockedStatic = Mockito.mockStatic(SecurityContextHolder.class)) {
            SecurityContext securityContext = mock(SecurityContext.class);
            Authentication authentication = mock(Authentication.class);
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("testuser");
            when(userService.getUserByUsername("testuser")).thenReturn(user);

            when(taskRepository.findById(taskId)).thenReturn(Optional.of(existedTask));
            when(taskRepository.isTitleUnique(request.getTitle(), user.getId(), taskId)).thenReturn(true);
            doNothing().when(taskMapper).updateTaskFromRequest(request, existedTask);

            when(taskRepository.findById(request.getParentId())).thenReturn(Optional.of(parentTask));

            AccessDeniedException exception = assertThrows(AccessDeniedException.class, () -> {
                taskService.updateTask(taskId, request);
            });

            assertEquals("Parent task must belong to the authenticated user.", exception.getMessage());

            verify(userService).getUserByUsername("testuser");
            verify(taskRepository).findById(taskId);
            verify(taskRepository).isTitleUnique(request.getTitle(), user.getId(), taskId);
            verify(taskMapper).updateTaskFromRequest(request, existedTask);
            verify(taskRepository).findById(request.getParentId());
            verify(taskRepository, never()).save(any(Task.class));
            verify(taskMapper, never()).toTaskDTO(any(Task.class));
        }
    }

    @Test
    void updateTask_failedToSaveTask_throwsException() {
        TaskRequest request = new TaskRequest();
        request.setTitle("My Updated Task");
        request.setCategoryNames(Arrays.asList("Work", "Urgent"));
        request.setParentId(2L);

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        Long taskId = 1L;
        Task existedTask = new Task();
        existedTask.setId(taskId);
        existedTask.setOwner(user);

        Task parentTask = new Task();
        parentTask.setId(2L);
        parentTask.setOwner(user);

        Category work = new Category("Work");
        work.setId(1L);
        Category urgent = new Category("Urgent");
        urgent.setId(2L);

        try (MockedStatic<SecurityContextHolder> mockedStatic = Mockito.mockStatic(SecurityContextHolder.class)) {
            SecurityContext securityContext = mock(SecurityContext.class);
            Authentication authentication = mock(Authentication.class);
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("testuser");

            when(userService.getUserByUsername("testuser")).thenReturn(user);
            when(taskRepository.findById(taskId)).thenReturn(Optional.of(existedTask));
            when(taskRepository.isTitleUnique(request.getTitle(), user.getId(), taskId)).thenReturn(true);
            when(categoryRepository.findByName("Work")).thenReturn(Optional.of(work));
            when(categoryRepository.findByName("Urgent")).thenReturn(Optional.of(urgent));
            doNothing().when(taskMapper).updateTaskFromRequest(request, existedTask);
            when(taskRepository.findById(request.getParentId())).thenReturn(Optional.of(parentTask));
            when(taskRepository.save(existedTask)).thenReturn(null);

            IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
                taskService.updateTask(taskId, request);
            });

            assertEquals("Failed to save task with ID: " + taskId, exception.getMessage());

            verify(userService).getUserByUsername("testuser");
            verify(taskRepository).findById(taskId);
            verify(taskRepository).isTitleUnique(request.getTitle(), user.getId(), taskId);
            verify(taskMapper).updateTaskFromRequest(request, existedTask);
            verify(taskRepository).findById(request.getParentId());
            verify(taskRepository).save(any(Task.class));
            verify(taskMapper, never()).toTaskDTO(any(Task.class));
        }
    }

    @Test
    void updateTaskStatus_successfulUpdateStatusToDone() {
        Long taskId = 1L;
        TaskStatusUpdateRequest request = new TaskStatusUpdateRequest();
        request.setStatus(Status.DONE);

        Task task = new Task();
        task.setId(taskId);
        task.setStatus(Status.TODO);

        Task updatedTask = new Task();
        updatedTask.setId(taskId);
        updatedTask.setStatus(Status.DONE);

        TaskDTO dto = new TaskDTO();
        dto.setId(taskId);
        dto.setStatus(Status.DONE);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(updatedTask);
        when(taskMapper.toTaskDTO(updatedTask)).thenReturn(dto);

        TaskServiceImpl taskServiceSpy = spy(taskService);
        doNothing().when(taskServiceSpy).validateChildTaskCompletion(taskId);

        TaskDTO result = taskServiceSpy.updateTaskStatus(taskId, request);
        assertNotNull(result);
        assertEquals(Status.DONE, result.getStatus());
        assertEquals(taskId, result.getId());

        verify(taskRepository).findById(taskId);
        verify(taskServiceSpy).validateChildTaskCompletion(taskId);
        verify(taskRepository).save(task);
        verify(taskMapper).toTaskDTO(updatedTask);
    }

    @Test
    void updateTaskStatus_successfulUpdateStatusToAnotherStatuses() {
        Long taskId = 1L;
        TaskStatusUpdateRequest request = new TaskStatusUpdateRequest();
        request.setStatus(Status.IN_PROGRESS);

        Task task = new Task();
        task.setId(taskId);
        task.setStatus(Status.TODO);

        Task updatedTask = new Task();
        updatedTask.setId(taskId);
        updatedTask.setStatus(Status.IN_PROGRESS);

        TaskDTO dto = new TaskDTO();
        dto.setId(taskId);
        dto.setStatus(Status.IN_PROGRESS);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(updatedTask);
        when(taskMapper.toTaskDTO(updatedTask)).thenReturn(dto);

        TaskDTO result = taskService.updateTaskStatus(taskId, request);
        assertNotNull(result);
        assertEquals(Status.IN_PROGRESS, result.getStatus());
        assertEquals(taskId, result.getId());

        verify(taskRepository).findById(taskId);
        verify(taskRepository).save(task);
        verify(taskMapper).toTaskDTO(updatedTask);
    }

    @Test
    void updateTaskStatus_taskNotFound_throwsException() {
        Long taskId = 1L;
        TaskStatusUpdateRequest request = new TaskStatusUpdateRequest();
        request.setStatus(Status.DONE);

        Task task = new Task();
        task.setId(taskId);
        task.setStatus(Status.TODO);

        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            taskService.updateTaskStatus(taskId, request);
        });

        assertEquals("Task not found with ID: " + taskId, exception.getMessage());

        verify(taskRepository).findById(taskId);
        verify(taskRepository, never()).save(any(Task.class));
        verify(taskMapper, never()).toTaskDTO(any(Task.class));
    }

    @Test
    void updateTaskStatus_incompletedChildTasks_throwsException() {
        Long taskId = 1L;
        TaskStatusUpdateRequest request = new TaskStatusUpdateRequest();
        request.setStatus(Status.DONE);

        Task task = new Task();
        task.setId(taskId);
        task.setStatus(Status.TODO);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        TaskServiceImpl taskServiceSpy = spy(taskService);
        doThrow(new CannotProceedException("Cannot proceed with task " + taskId + " while child tasks are not completed."))
                .when(taskServiceSpy).validateChildTaskCompletion(taskId);

        CannotProceedException exception = assertThrows(CannotProceedException.class, () -> {
            taskServiceSpy.updateTaskStatus(taskId, request);
        });

        assertEquals("Cannot proceed with task " + taskId + " while child tasks are not completed.", exception.getMessage());

        verify(taskRepository).findById(taskId);
        verify(taskServiceSpy).validateChildTaskCompletion(taskId);
        verify(taskRepository, never()).save(any(Task.class));
        verify(taskMapper, never()).toTaskDTO(any(Task.class));
    }

    @Test
    void deleteTask_successfulDelete() {
        Long taskId = 1L;
        Task parentTask = new Task();
        parentTask.setId(taskId);
        parentTask.setTitle("Parent Title");

        Task child1 = new Task();
        child1.setId(2L);
        child1.setStatus(Status.DONE);

        Task child2 = new Task();
        child2.setId(3L);
        child2.setStatus(Status.IN_PROGRESS);

        List<Task> childTasks = Arrays.asList(child1, child2);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(parentTask));
        when(taskRepository.findByParentTaskId(taskId)).thenReturn(childTasks);

        TaskServiceImpl taskServiceSpy = spy(taskService);
        doNothing().when(taskServiceSpy).validateChildTaskCompletion(taskId);
        taskServiceSpy.deleteTask(taskId);

        verify(taskRepository).findById(taskId);
        verify(taskRepository).findByParentTaskId(taskId);
        verify(taskRepository, times(childTasks.size() + 1)).delete(any(Task.class));
    }

    @Test
    void deleteTask_noChildTasks_successfulDelete() {
        Long taskId = 1L;
        Task parentTask = new Task();
        parentTask.setId(taskId);
        parentTask.setTitle("Parent task");

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(parentTask));
        when(taskRepository.findByParentTaskId(taskId)).thenReturn(Collections.emptyList());

        doNothing().when(taskRepository).delete(any(Task.class));
        taskService.deleteTask(taskId);

        verify(taskRepository).findById(taskId);
        verify(taskRepository).findByParentTaskId(taskId);
        verify(taskRepository).delete(any(Task.class));
    }

    @Test
    void deleteTask_childTaskNotCompleted_throwsExceptions() {
        Long taskId = 1L;
        Task parentTask = new Task();
        parentTask.setId(taskId);
        parentTask.setTitle("Parent Title");

        Task child1 = new Task();
        child1.setId(2L);
        child1.setStatus(Status.TODO);
        child1.setParentTask(parentTask);

        Task child2 = new Task();
        child2.setId(3L);
        child2.setStatus(Status.IN_PROGRESS);
        child2.setParentTask(parentTask);

        List<Task> childTasks = Arrays.asList(child1, child2);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(parentTask));

        when(taskRepository.findByParentTaskId(taskId)).thenReturn(childTasks);

        CannotProceedException exception = assertThrows(CannotProceedException.class, () -> {
            taskService.deleteTask(taskId);
        });

        assertEquals("Cannot proceed with task " + taskId + " while child tasks are not completed.", exception.getMessage());

        verify(taskRepository).findById(taskId);
        verify(taskRepository, times(2)).findByParentTaskId(taskId);
        verify(taskRepository, never()).delete(any(Task.class));
    }

    @Test
    void deleteTask_taskNotFound_throwsException() {
        Long taskId = 1L;
        Task parenTask = new Task();
        parenTask.setId(2L);
        parenTask.setTitle("parent Task");

        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            taskService.deleteTask(taskId);
        });

        assertEquals("Task not found with ID: " + taskId, exception.getMessage());

        verify(taskRepository).findById(taskId);
        verify(taskRepository, never()).findByParentTaskId(taskId);
        verify(taskRepository, never()).delete(any(Task.class));
    }
}
