package todo.list.todo_list.dto.User;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ChangePasswordRequest {

    @NotEmpty(message = "Old Password cannot be empty")
    private String oldPassword;

    @NotEmpty(message = "New Password cannot be empty")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,100}$",
            message = "New Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character")
    @Size(min = 8, max = 100, message = "New Password must be between 8 and 100 characters")
    private String newPassword;

    public String getOldPassword() {
        return this.oldPassword;
    }

    public String getNewPassword() {
        return this.newPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}