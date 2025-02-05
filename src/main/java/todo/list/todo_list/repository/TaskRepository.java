package todo.list.todo_list.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import todo.list.todo_list.entity.Task;
import todo.list.todo_list.entity.User;

public interface TaskRepository extends JpaRepository<Task, Long> {

    @Query("SELECT t FROM Task t WHERE t.parentTask IS NULL")
    List<Task> findParentTasks();

    List<Task> findByUser(User user);

    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN false ELSE true END "
            + "FROM Task t WHERE t.title = :title AND t.user.id = :userId "
            + "AND (t.id != :taskId OR :taskId IS NULL)")
    boolean isTitleUnique(@Param("title") String title, @Param("userId") Long userId, @Param("taskId") Long taskId);

}
