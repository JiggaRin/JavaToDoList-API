package todo.list.todo_list.service;

import todo.list.todo_list.dto.RegistrationRequest;
import todo.list.todo_list.entity.User;

public interface UserService {
    void registerUser(RegistrationRequest request);
    User getUserById(Long id);
}