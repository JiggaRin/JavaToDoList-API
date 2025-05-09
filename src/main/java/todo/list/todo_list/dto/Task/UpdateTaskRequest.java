package todo.list.todo_list.dto.Task;

import java.util.List;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import todo.list.todo_list.model.Status;
import todo.list.todo_list.validation.EnumValidator;

public class UpdateTaskRequest {
    private Long parentId;

    @Size(max = 255, message = "Title must be between 1 and 255 characters")
    private String title;

    @Size(max = 255, message = "Description must be between 1 and 255 characters")
    private String description;

    @EnumValidator(enumClass = Status.class, message = "Invalid status value. Status must be one of: TODO, IN_PROGRESS, DONE")
    private Status status;

    @Size(min = 1, message = "At least one category name must be provided")
    private List<@Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Each category name must contain only letters, numbers, and underscores") String> categoryNames;

    public UpdateTaskRequest() {
    }

    public UpdateTaskRequest(String title, List<String> categoryNames, Long parentId) {
        this.title = title;
        this.categoryNames = categoryNames;
        this.parentId = parentId;
    }

    public Long getParentId() {
        return this.parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
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

    public List<String> getCategoryNames() {
        return this.categoryNames;
    }

    public void setCategoryNames(List<String> categoryNames) {
        this.categoryNames = categoryNames;
    }
}