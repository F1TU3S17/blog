package ru.sfedu.blog.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {
    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/change-theme")
    public String changeTheme(
            @RequestParam String theme,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        Cookie themeCookie = new Cookie("theme", theme);
        themeCookie.setMaxAge(365 * 24 * 60 * 60);
        themeCookie.setPath("/");
        response.addCookie(themeCookie);

        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/");
    }

}
