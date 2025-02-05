package todo.list.todo_list.serialization;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import todo.list.todo_list.model.Status;

public class StatusEnumDeserializer extends JsonDeserializer<Status> {

    @Override
    public Status deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        String value = jp.getText().toUpperCase();
        try {
            return Status.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new JsonProcessingException("Invalid value for enum Status: " + value) {};
        }
    }
}
