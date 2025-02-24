package todo.list.todo_list.dto.Task;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import todo.list.todo_list.model.Status;
import todo.list.todo_list.validation.EnumValidator;

public class TaskRequest {
    private Long parentId;

    @NotEmpty(message = "Title cannot be empty")
    @Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
    private String title;

    @Size(min = 1, max = 255, message = "Description must be between 1 and 255 characters")
    private String description;

    @NotNull(message = "Status is required")
    @EnumValidator(enumClass = Status.class, message = "Status must be one of: TODO, IN_PROGRESS, DONE")
    private Status status;

    @NotEmpty(message = "Category names cannot be empty")
    @Size(min = 1, message = "At least one category name must be provided")
    private List<@Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Each category name must contain only letters, numbers, and underscores") String> categoryNames;

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
