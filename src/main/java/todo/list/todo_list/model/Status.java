package todo.list.todo_list.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import todo.list.todo_list.serialization.StatusEnumDeserializer;

@JsonDeserialize(using = StatusEnumDeserializer.class)
public enum Status {
    TODO, IN_PROGRESS, DONE
}
