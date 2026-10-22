package ru.moysklad.intern.remapstub.web;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.moysklad.intern.remapstub.service.RemapStubException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice(basePackages = "ru.moysklad.intern.remapstub")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RemapStubExceptionHandler {

    @ExceptionHandler(RemapStubException.class)
    public ResponseEntity<Map<String, Object>> handle(RemapStubException ex) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("error", ex.getMessage());
        error.put("code", ex.getErrorCode());
        error.put("moreInfo", "https://dev.moysklad.ru/doc/api/remap/1.2/#/errors");
        Map<String, Object> body = Map.of("errors", List.of(error));
        return ResponseEntity.status(ex.getStatus()).body(body);
    }
}
