package todo.list.todo_list.exception;

import org.springframework.http.HttpStatus;

public class BadCredentialsException extends CustomException {

    public BadCredentialsException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
