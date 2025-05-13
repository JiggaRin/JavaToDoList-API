package todo.list.todo_list.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import todo.list.todo_list.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE (u.username = :username) AND (:userId IS NULL OR u.id <> :userId)")
    boolean existsByUsername(@Param("username") String username, @Param("userId") Long userId);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE (u.email = :email) AND (:userId IS NULL OR u.id <> :userId)")
    boolean existsByEmail(@Param("email") String email, @Param("userId") Long userId);

    Optional<User> findByUsername(String username);
}
