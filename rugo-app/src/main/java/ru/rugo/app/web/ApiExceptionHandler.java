package ru.rugo.app.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import ru.rugo.common.exception.ApiValidationException;
import ru.rugo.common.exception.DuplicateLeadException;
import ru.rugo.common.exception.EntityNotFoundException;
import ru.rugo.common.exception.RateLimitExceededException;
import ru.rugo.common.exception.UnauthorizedInitDataException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class ApiExceptionHandler {

    @ExceptionHandler(ApiValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(ApiValidationException e) {
        return errors(HttpStatus.BAD_REQUEST, e.getErrors());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleBeanValidation(MethodArgumentNotValidException e) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return errors(HttpStatus.BAD_REQUEST, fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadable(HttpMessageNotReadableException e) {
        return errors(HttpStatus.BAD_REQUEST, Map.of("request", "Некорректное тело запроса"));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParam(MissingServletRequestParameterException e) {
        return errors(HttpStatus.BAD_REQUEST, Map.of(e.getParameterName(), "Обязательный параметр"));
    }

    @ExceptionHandler(UnauthorizedInitDataException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedInitDataException e) {
        return errors(HttpStatus.UNAUTHORIZED, Map.of("initData", "Не пройдена проверка initData"));
    }

    @ExceptionHandler(DuplicateLeadException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicate(DuplicateLeadException e) {
        return errors(HttpStatus.CONFLICT, Map.of("phone", "Мы уже получили вашу заявку"));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEntityNotFound(EntityNotFoundException e) {
        return errors(HttpStatus.NOT_FOUND, Map.of("request", "Не найдено"));
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimit(RateLimitExceededException e) {
        return errors(HttpStatus.TOO_MANY_REQUESTS, Map.of("rateLimit", "Слишком много запросов"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleTooLarge(MaxUploadSizeExceededException e) {
        return errors(HttpStatus.BAD_REQUEST, Map.of("file", "Файл слишком большой"));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Map<String, Object>> handleMissingPart(MissingServletRequestPartException e) {
        return errors(HttpStatus.BAD_REQUEST, Map.of(e.getRequestPartName(), "Файл не выбран"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NoResourceFoundException e) {
        return errors(HttpStatus.NOT_FOUND, Map.of("request", "Не найдено"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception e) {
        log.error("Необработанная ошибка API", e);
        return errors(HttpStatus.INTERNAL_SERVER_ERROR, Map.of("server", "Внутренняя ошибка"));
    }

    private ResponseEntity<Map<String, Object>> errors(HttpStatus status, Map<String, String> errors) {
        return ResponseEntity.status(status).body(Map.of("errors", errors));
    }
}
