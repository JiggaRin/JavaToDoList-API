package todo.list.todo_list.dto.Task;

import jakarta.validation.constraints.NotNull;
import todo.list.todo_list.model.Status;
import todo.list.todo_list.validation.EnumValidator;

public class TaskStatusUpdateRequest {

    @NotNull(message = "Status is required")
    @EnumValidator(enumClass = Status.class, message = "Invalid status value. Status must be one of: TODO, IN_PROGRESS, DONE")
    private Status status;

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
