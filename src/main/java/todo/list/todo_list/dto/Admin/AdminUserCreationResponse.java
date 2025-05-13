package todo.list.todo_list.dto.Admin;

import todo.list.todo_list.model.Role;

public class AdminUserCreationResponse {

    private String message;
    private String username;
    private String email;
    private Role role;

    public AdminUserCreationResponse(String message, String username, String email, Role role) {
        this.message = message;
        this.username = username;
        this.email = email;
        this.role = role;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}