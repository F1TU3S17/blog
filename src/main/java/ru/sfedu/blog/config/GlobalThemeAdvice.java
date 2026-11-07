package ru.sfedu.blog.config;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalThemeAdvice {

    @ModelAttribute("currentTheme")
    public String addThemeToModel(@CookieValue(name = "theme", defaultValue = "light") String theme) {
        return theme;
    }
}