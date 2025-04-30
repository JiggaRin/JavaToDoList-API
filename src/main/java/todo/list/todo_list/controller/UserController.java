package todo.list.todo_list.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import todo.list.todo_list.dto.User.ChangePasswordRequest;
import todo.list.todo_list.dto.User.UpdateRequest;
import todo.list.todo_list.dto.User.UserDTO;
import todo.list.todo_list.security.CustomUserDetails;
import todo.list.todo_list.service.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PutMapping("/update")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('MODERATOR')")
    public ResponseEntity<UserDTO> updateUser(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateRequest updateRequest) {

        log.debug("Received Update request for userID: {}", userDetails.getId());
        try {
            ResponseEntity<UserDTO> response = this.updatingUser(userDetails, updateRequest);

            return response;
        } catch (Exception e) {
            log.error("Update request failed for userID: {}", userDetails.getId(), e);
            throw e;
        }

    }

    @PutMapping("/change-password")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('MODERATOR')")
    public ResponseEntity<String> changePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest changePasswordRequest) {
        log.debug("Received Change password request for userID: {}", userDetails.getId());
        try {
            ResponseEntity<String> response = this.changingPasswordAndClearContext(userDetails, changePasswordRequest);

            return response;
        } catch (Exception e) {
            log.error("Change password request failed for userID: {}", userDetails.getId(), e);
            throw e;
        }
    }

    private ResponseEntity<UserDTO> updatingUser(CustomUserDetails userDetails, UpdateRequest updateRequest) {
        UserDTO user = userService.updateUser(userDetails.getId(), updateRequest);
        log.info("Successfully updated userID: {}", userDetails.getId());

        return ResponseEntity.ok(user);
    }

    private ResponseEntity<String> changingPasswordAndClearContext(CustomUserDetails userDetails, ChangePasswordRequest changePasswordRequest) {
        userService.changePassword(userDetails.getId(), changePasswordRequest);
        SecurityContextHolder.clearContext();
        log.info("Successfully changed password and clear context for userID: {}", userDetails.getId());

        return ResponseEntity.ok("Password updated successfully. Please log in again.");
    }
}