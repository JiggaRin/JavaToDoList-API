package todo.list.todo_list.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import todo.list.todo_list.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByName(String name);
}