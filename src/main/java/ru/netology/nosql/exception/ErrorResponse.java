package ru.netology.nosql.exception;

import java.time.Instant;
import java.util.Map;

/**
 * Единый формат ответа об ошибке.
 *
 * @param details ошибки по полям (только для ошибок валидации), иначе пустая map
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> details
) {
}
