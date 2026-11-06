package ru.sfedu.blog.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AppErrorEnum implements AppError {
    USER_NOT_FOUND(1, HttpStatus.NOT_FOUND, "User not found"),
    USER_PASSWORD_INCORRECT(2, HttpStatus.BAD_REQUEST, "User entered an incorrect password");

    private final int code;
    private final HttpStatus status;
    private final String message;

}
