package ru.sfedu.blog.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.sfedu.blog.security.CustomUserDetails;

@Controller
@RequestMapping("/home")
public class HomeController {

    @GetMapping
    public String getHomePage(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        model.addAttribute("user", user);
        return "profile";
    }
}
