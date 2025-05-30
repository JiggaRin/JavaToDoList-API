package todo.list.todo_list.service;

import todo.list.todo_list.entity.RefreshToken;

public interface RefreshTokenService {

    RefreshToken createRefreshToken(String username);

    void deleteByUsername(String username);
}
