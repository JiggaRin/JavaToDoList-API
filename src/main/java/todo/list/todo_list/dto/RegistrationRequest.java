package todo.list.todo_list.dto;

public class RegistrationRequest {
    private String username;
    private String email;
    private String password;


    public String getUsername() {
        return this.username;
    }

    public String getEmail() {
        return this.email;
    }

    public String getPassword() {
        return this.password;
    }
}