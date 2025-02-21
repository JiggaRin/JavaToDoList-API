package todo.list.todo_list.service;

import todo.list.todo_list.entity.RefreshToken;

public interface RefreshTokenService {

    String generateNewAccessToken(String refreshToken);

    RefreshToken createRefreshToken(String username);

    void deleteByUsername(String username);
}
