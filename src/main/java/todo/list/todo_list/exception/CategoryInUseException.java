package todo.list.todo_list.exception;

import org.springframework.http.HttpStatus;

public class CategoryInUseException extends CustomException {

    public CategoryInUseException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
