package todo.list.todo_list.exception;

import org.springframework.http.HttpStatus;

public class ResourceConflictException extends CustomException {

    public ResourceConflictException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
