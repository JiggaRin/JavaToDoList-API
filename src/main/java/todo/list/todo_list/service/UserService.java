package todo.list.todo_list.service;

import todo.list.todo_list.dto.Admin.AdminUserCreationRequest;
import todo.list.todo_list.dto.Admin.AdminUserCreationResponse;
import todo.list.todo_list.dto.Registration.RegistrationRequest;
import todo.list.todo_list.dto.Registration.RegistrationResponse;
import todo.list.todo_list.dto.User.ChangePasswordRequest;
import todo.list.todo_list.dto.User.UpdateRequest;
import todo.list.todo_list.dto.User.UserDTO;
import todo.list.todo_list.entity.User;

public interface UserService {

    RegistrationResponse registerUser(RegistrationRequest request);

    AdminUserCreationResponse createUserWithAdminOrModeratorRole(AdminUserCreationRequest request);

    User getUserById(Long id);

    User getUserByUsername(String username);

    UserDTO updateUser(Long userId, UpdateRequest request);

    void changePassword(Long userId, ChangePasswordRequest request);
}
