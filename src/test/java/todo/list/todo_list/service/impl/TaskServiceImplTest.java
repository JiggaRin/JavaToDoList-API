package todo.list.todo_list.service.impl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import todo.list.todo_list.dto.Task.TaskDTO;
import todo.list.todo_list.dto.Task.TaskRequest;
import todo.list.todo_list.entity.Category;
import todo.list.todo_list.entity.Task;
import todo.list.todo_list.entity.User;
import todo.list.todo_list.exception.AccessDeniedException;
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
}
