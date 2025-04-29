package todo.list.todo_list.dto.Auth;

import jakarta.validation.constraints.NotBlank;

public class RefreshTokenRequest {
     @NotBlank(message = "Refresh token must not be blank")
    private String refreshToken;

    public RefreshTokenRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken() {
        return this.refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}