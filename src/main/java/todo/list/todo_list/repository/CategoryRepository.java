package todo.list.todo_list.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import todo.list.todo_list.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByName(String name);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN false ELSE true END FROM Category c WHERE c.name = :name AND (:categoryId IS NULL OR c.id != :categoryId)")
    boolean isCategoryNameUnique(@Param("name") String name, @Param("categoryId") Long categoryId);

    @Query("SELECT COUNT(t) > 0 FROM Task t JOIN t.categories c WHERE c.id = :categoryId")
    boolean isCategoryInUse(@Param("categoryId") Long categoryId);

}
