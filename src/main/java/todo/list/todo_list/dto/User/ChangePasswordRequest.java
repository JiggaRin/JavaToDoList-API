package todo.list.todo_list.dto.User;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public class ChangePasswordRequest {
    @NotEmpty(message = "Password cannot be empty")
    private String oldPassword;

    @NotEmpty(message = "Password cannot be empty")
    @Size(min = 6, message = "Password must be at least 6 characters long")
    private String newPassword;

    public String getOldPassword() {
        return this.oldPassword;
    }

    public String getNewPassword() {
        return this.newPassword;
    }
}