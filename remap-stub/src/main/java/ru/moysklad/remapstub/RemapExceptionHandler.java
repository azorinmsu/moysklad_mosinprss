package ru.moysklad.remapstub;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RemapExceptionHandler {

    private final ObjectMapper mapper;

    public RemapExceptionHandler(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @ExceptionHandler(RemapException.class)
    public ResponseEntity<ObjectNode> handle(RemapException exception, HttpServletRequest request) {
        if (!request.getRequestURI().startsWith("/api/remap/")) {
            ObjectNode body = mapper.createObjectNode();
            body.put("error", exception.getMessage());
            return ResponseEntity.status(exception.httpStatus()).body(body);
        }
        ObjectNode error = mapper.createObjectNode();
        error.put("error", exception.getMessage());
        error.put("error_message", exception.getMessage());
        if (exception.code() != null) {
            error.put("code", exception.code());
        }
        ArrayNode errors = mapper.createArrayNode().add(error);
        ObjectNode body = mapper.createObjectNode();
        body.set("errors", errors);
        return ResponseEntity.status(exception.httpStatus()).body(body);
    }
}
