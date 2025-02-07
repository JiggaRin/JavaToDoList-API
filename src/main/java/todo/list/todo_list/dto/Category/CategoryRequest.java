package todo.list.todo_list.dto.Category;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public class CategoryRequest {
    @NotEmpty(message = "Category name cannot be empty")
    @Size(min = 1, max = 255, message = "Category name must be between 1 and 255 characters")
    private String name;

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }
}