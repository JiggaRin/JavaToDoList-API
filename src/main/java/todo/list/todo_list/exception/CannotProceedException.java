package todo.list.todo_list.exception;

import org.springframework.http.HttpStatus;

public class CannotProceedException extends CustomException {

    public CannotProceedException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}