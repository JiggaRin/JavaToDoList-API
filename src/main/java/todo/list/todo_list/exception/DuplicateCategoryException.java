package todo.list.todo_list.exception;

import org.springframework.http.HttpStatus;

public class DuplicateCategoryException extends CustomException {

    public DuplicateCategoryException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
