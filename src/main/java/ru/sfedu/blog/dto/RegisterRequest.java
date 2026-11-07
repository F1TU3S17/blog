package ru.sfedu.blog.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RegisterRequest(
        @NotBlank(message = "Укажите имя пользователя")
        @Pattern(
                regexp = "^[a-zA-Z][a-zA-Z0-9_]{2,49}$",
                message = "От 3 до 50 символов: латинские буквы, цифры и _. Начните с буквы"
        )
        String username,

        @NotBlank(message = "Укажите email")
        @Email(message = "Введите корректный email")
        String email,

        @NotBlank
        String password
) {}