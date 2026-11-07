package ru.sfedu.blog.exception;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.WebUtils;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ModelAndView handleAppException(
            AppException ex,
            HttpServletRequest request
    ) {
        AppError error = ex.getAppError();
        Cookie cookie = WebUtils.getCookie(request, "theme");
        String theme = cookie != null ? cookie.getValue() : "light";

        if (error == AppErrorEnum.USERNAME_ALREADY_EXISTS || error == AppErrorEnum.EMAIL_ALREADY_EXISTS) {
            ModelAndView mav = new ModelAndView("register");
            mav.addObject("currentTheme", theme);
            mav.addObject("error", ex.getMessage());
            mav.setStatus(error.getStatus());

            return mav;
        }


        ModelAndView mav = new ModelAndView("errors/app-error");

        mav.setStatus(error.getStatus());
        mav.addObject("status", error.getStatus().value());
        mav.addObject("message", error.getMessage());
        mav.addObject("currentTheme", theme);

        return mav;
    }
}