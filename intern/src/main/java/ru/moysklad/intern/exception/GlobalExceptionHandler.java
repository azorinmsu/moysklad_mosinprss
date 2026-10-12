package ru.moysklad.intern.exception;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.moysklad.intern.exception.auth.UnauthorizedException;

import java.util.Map;

/**
 * Референс брал здесь:
 * https://medium.com/@roshanfarakate/global-exception-handling-in-spring-boot-712593159a26
 * */

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        // Просто ловит все исключения, которые не заданы как либо иначе
        return new ResponseEntity<>(ErrorForm.generateErrorForm(
                "INTERNAL SERVER ERROR",
                ex.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR
        ), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(Exception ex) {
        return new ResponseEntity<>(ErrorForm.generateErrorForm(
                "NOT FOUND",
                "Такого ресурса не существует",
                HttpStatus.NOT_FOUND
        ), HttpStatus.NOT_FOUND);
    }

    // исключения, связанные с авторизацией
    @ExceptionHandler({
            UnauthorizedException.class,
            MissingRequestHeaderException.class
    })
    public ResponseEntity<Map<String, Object>> handleUnauthorized() {
        return new ResponseEntity<>(ErrorForm.generateErrorForm(
                "UNAUTHORIZED",
                "Не проведены данные для авторизации",
                HttpStatus.UNAUTHORIZED
        ), HttpStatus.UNAUTHORIZED);
    }
}
