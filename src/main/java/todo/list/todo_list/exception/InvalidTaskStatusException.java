package todo.list.todo_list.exception;

import org.springframework.http.HttpStatus;

public class InvalidTaskStatusException extends CustomException {

    public InvalidTaskStatusException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}