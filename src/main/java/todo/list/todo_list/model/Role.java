package todo.list.todo_list.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import todo.list.todo_list.serialization.RoleEnumDeserializer;

@JsonDeserialize(using = RoleEnumDeserializer.class)
public enum Role {USER, ADMIN, MODERATOR}