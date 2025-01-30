package todo.list.todo_list.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import todo.list.todo_list.entity.Task;
import todo.list.todo_list.entity.User;

public interface TaskRepository extends JpaRepository<Task, Long> {
    @Query("SELECT t FROM Task t WHERE t.parentTask IS NULL")
    List<Task> findParentTasks();
    List<Task> findByUser(User user);
}