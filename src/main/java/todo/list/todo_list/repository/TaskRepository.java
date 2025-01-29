package todo.list.todo_list.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import todo.list.todo_list.entity.Task;
import todo.list.todo_list.entity.User;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByUser(User user);
}