package todo.list.todo_list.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import todo.list.todo_list.entity.Task;
import todo.list.todo_list.entity.User;

public interface TaskRepository extends JpaRepository<Task, Long> {

    @Query("SELECT t FROM Task t WHERE (:userId IS NULL OR t.owner.id = :userId) "
            + "AND t.parentTask IS NULL "
            + "AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%'))) ")
    Page<Task> findParentTasks(@Param("userId") Long userId, @Param("search") String search, Pageable pageable);

    List<Task> findByOwner(User owner);

    List<Task> findByParentTaskId(Long taskId);

    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN false ELSE true END "
            + "FROM Task t WHERE t.title = :title AND t.owner.id = :userId "
            + "AND (t.id != :taskId OR :taskId IS NULL)")
    boolean isTitleUnique(@Param("title") String title, @Param("userId") Long userId, @Param("taskId") Long taskId);

}
