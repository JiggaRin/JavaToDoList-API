package todo.list.todo_list.service;

import todo.list.todo_list.dto.Registration.RegistrationRequest;
import todo.list.todo_list.dto.Registration.RegistrationResponse;
import todo.list.todo_list.entity.User;

public interface UserService {
    RegistrationResponse registerUser(RegistrationRequest request);
    User getUserById(Long id);
    User getUserByUsername(String username);
}