package todo.list.todo_list.dto.User;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UpdateRequest {
    @Pattern(regexp = "^[A-Za-zà-ÿÀ-ß'\\- ]+$", message = "Invalid characters in first name")
    @Size(max = 50, message = "First name must be less than 50 characters")
    private String firstName;

    @Size(max = 50, message = "Last name must be less than 50 characters")
    @Pattern(regexp = "^[A-Za-zà-ÿÀ-ß'\\- ]+$", message = "Invalid characters in last name")
    private String lastName;

    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email must be less than 255 characters")
    private String email;

    public String getFirstName() {
        return this.firstName;
    }

    public String getLastName() {
        return this.lastName;
    }

    public String getEmail() {
        return this.email;
    }

}