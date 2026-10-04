package ru.netology.nosql.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Тело запроса на создание или изменение пользователя.
 */
public record UserRequest(
        @NotBlank(message = "name must not be blank")
        @Size(min = 2, max = 100, message = "name length must be between 2 and 100")
        String name,

        @NotBlank(message = "email must not be blank")
        @Email(message = "email must be a valid email address")
        String email,

        @NotNull(message = "age is required")
        @Min(value = 0, message = "age must be >= 0")
        @Max(value = 150, message = "age must be <= 150")
        Integer age
) {
}
