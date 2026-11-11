package ru.rugo.common.exception;

import java.util.Map;

/**
 * Ошибка бизнес-валидации. {@link #getErrors()} — карта «поле → сообщение»,
 * сериализуется в ответ 400 как {@code {"errors": {...}}}.
 */
public class ApiValidationException extends RuntimeException {

    private final transient Map<String, String> errors;

    public ApiValidationException(Map<String, String> errors) {
        super("Validation failed: " + errors.keySet());
        this.errors = Map.copyOf(errors);
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
