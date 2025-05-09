package todo.list.todo_list.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import todo.list.todo_list.dto.Task.CreateTaskRequest;
import todo.list.todo_list.dto.Task.TaskDTO;
import todo.list.todo_list.dto.Task.UpdateTaskRequest;
import todo.list.todo_list.entity.Task;

@Mapper(componentModel = "spring")
public interface TaskMapper {

    @Mapping(target = "userId", source = "owner.id")
    @Mapping(target = "parentId", source = "parentTask.id")
    @Mapping(target = "categories", expression = "java(task.getCategories().stream().map(category -> category.getName()).collect(java.util.stream.Collectors.toSet()))")
    @Mapping(target = "subTasks", source = "subTasks")
    TaskDTO toTaskDTO(Task task);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "parentTask", ignore = true)
    @Mapping(target = "categories", ignore = true)
    @Mapping(target = "subTasks", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Task createTaskFromRequest(CreateTaskRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "parentTask", ignore = true)
    @Mapping(target = "categories", ignore = true)
    @Mapping(target = "subTasks", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "title", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "description", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "status", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateTaskFromRequest(UpdateTaskRequest request, @MappingTarget Task task);
}