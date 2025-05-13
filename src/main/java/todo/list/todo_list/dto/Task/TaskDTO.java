package todo.list.todo_list.dto.Task;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;

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

    public TaskDTO() {
    }

    public TaskDTO(Long taskId, String title, Status status) {
        this.id = taskId;
        this.title = title;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Set<String> getCategories() {
        return categories;
    }

    public void setCategories(Set<String> categories) {
        this.categories = categories;
    }

    public List<TaskDTO> getSubTasks() {
        return subTasks;
    }

    public void setSubTasks(List<TaskDTO> subTasks) {
        this.subTasks = subTasks;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
