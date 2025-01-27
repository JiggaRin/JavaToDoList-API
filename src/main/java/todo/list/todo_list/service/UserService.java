package todo.list.todo_list.service;

import todo.list.todo_list.dto.RegistrationRequest;

public interface UserService {
    void registerUser(RegistrationRequest request);
}