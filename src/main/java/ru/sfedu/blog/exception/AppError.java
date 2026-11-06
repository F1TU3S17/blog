package ru.sfedu.blog.exception;

import org.springframework.http.HttpStatus;

public interface AppError {
    int getCode();
    HttpStatus getStatus();
    String getMessage();
}
