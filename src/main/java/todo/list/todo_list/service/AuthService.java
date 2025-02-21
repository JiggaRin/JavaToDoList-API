package todo.list.todo_list.service;

import todo.list.todo_list.dto.Auth.AuthRequest;
import todo.list.todo_list.dto.Auth.AuthResponse;

public interface AuthService {

    AuthResponse authenticate(AuthRequest authRequest);
    
}
