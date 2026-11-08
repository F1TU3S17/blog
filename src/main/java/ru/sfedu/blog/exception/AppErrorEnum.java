package ru.sfedu.blog.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AppErrorEnum implements AppError {
    USER_NOT_FOUND(1, HttpStatus.NOT_FOUND, "User not found"),
    USER_PASSWORD_INCORRECT(2, HttpStatus.BAD_REQUEST, "User entered an incorrect password"),
    POST_NOT_FOUND(3, HttpStatus.NOT_FOUND, "Post not found"),
    USER_CANT_EDIT_POST(4, HttpStatus.FORBIDDEN, "User can`t edit current post"),
    USER_CANT_DELETE_POST(5, HttpStatus.FORBIDDEN, "User can`t delete post"),
    USERNAME_ALREADY_EXISTS(6, HttpStatus.CONFLICT, "Username already exists"),
    EMAIL_ALREADY_EXISTS(7, HttpStatus.CONFLICT, "Email already exists"),
    COMMENT_NOT_FOUND(8, HttpStatus.NOT_FOUND, "Comment not found"),
    USER_CANT_EDIT_COMMENT(9, HttpStatus.FORBIDDEN, "User can`t edit current comment"),
    ;

    private final int code;
    private final HttpStatus status;
    private final String message;

}
