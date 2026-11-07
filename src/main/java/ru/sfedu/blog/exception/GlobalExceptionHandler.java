package ru.sfedu.blog.exception;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ModelAndView handleAppException(AppException ex) {

        AppError error = ex.getAppError();

        ModelAndView mav = new ModelAndView("errors/app-error");

        mav.setStatus(error.getStatus());
        mav.addObject("status", error.getStatus().value());
        mav.addObject("message", error.getMessage());

        return mav;
    }
}