package ru.moysklad.intern.exception;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class ErrorForm {
    public static Map<String, Object> generateErrorForm(
            String errorName, String message, HttpStatus status) {
        Map<String, Object> res = new HashMap<>();
        res.put("error", errorName);
        res.put("message", message);
        res.put("status", status.value());
        res.put("timestamp", LocalDateTime.now());
        return res;
    }
}
