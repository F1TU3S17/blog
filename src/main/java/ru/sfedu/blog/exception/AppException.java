package ru.sfedu.blog.exception;

import lombok.Getter;

@Getter
public class AppException extends RuntimeException {
    private final AppError appError;

    public AppException(AppError appError) {
        super(appError.getMessage());
        this.appError = appError;
    }

}
