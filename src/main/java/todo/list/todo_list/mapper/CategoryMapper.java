package todo.list.todo_list.mapper;

import org.mapstruct.Mapper;

import todo.list.todo_list.dto.Category.CategoryDTO;
import todo.list.todo_list.entity.Category;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    CategoryDTO toCategoryDTO(Category category);
}
