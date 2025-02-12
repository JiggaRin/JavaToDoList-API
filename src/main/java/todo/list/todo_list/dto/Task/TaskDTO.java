package todo.list.todo_list.dto.Task;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;

import todo.list.todo_list.entity.Category;
import todo.list.todo_list.entity.Task;
import todo.list.todo_list.model.Status;

public class TaskDTO {
    private Long id;
    private Long userId;
    private Long parentId;
    private String title;
    private String description;
    private Status status;
    
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Set<String> categories;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<TaskDTO> subTasks;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public TaskDTO(Task task) {
        this.id = task.getId();
        this.userId = task.getUser().getId();
        this.parentId = (task.getParentTask() != null) ? task.getParentTask().getId() : null;
        this.title = task.getTitle();
        this.description = task.getDescription();
        this.status = task.getStatus();
        this.createdAt = task.getCreatedAt();
        this.updatedAt = task.getUpdatedAt();
        this.categories = task.getCategories().stream().map(Category::getName).collect(Collectors.toSet());

        if (task.getSubTasks() != null && !task.getSubTasks().isEmpty()) {
            this.subTasks = task.getSubTasks().stream()
                    .map(TaskDTO::new)
                    .collect(Collectors.toList());
        }
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getParentId() {
        return this.parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Long getUserId() {
        return this.userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Status getStatus() {
        return this.status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public List<TaskDTO> getSubTasks() {
        return this.subTasks;
    }

    public void setSubTasks(List<TaskDTO> subTasks) {
        this.subTasks = subTasks;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }


    public Set<String> getCategories() {
        return this.categories;
    }

    public void setCategories(Set<String> categories) {
        this.categories = categories;
    }
}
