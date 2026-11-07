package ru.sfedu.blog.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.sfedu.blog.dto.LoginRequest;
import ru.sfedu.blog.dto.RegisterRequest;
import ru.sfedu.blog.security.CustomUserDetails;
import ru.sfedu.blog.service.SessionService;
import ru.sfedu.blog.service.UserService;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;
    private final SessionService sessionService;

    @GetMapping("/register")
    public String getRegisterPage() {
        return "register";
    }

    @PostMapping("/register")
    public String register(
        @Valid @ModelAttribute RegisterRequest registerRequest,
        BindingResult bindingResult,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        if (bindingResult.hasErrors()) {
            return "register";
        }
        UserDetails userDetails = userService.registerUser(registerRequest);

        sessionService.authenticateSession(userDetails, request, response);

        return "redirect:/posts";
    }

    @GetMapping("/login")
    public String getLoginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String login(
        @Valid @ModelAttribute LoginRequest loginRequest,
        BindingResult bindingResult,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        if (bindingResult.hasErrors()) {
            return "login";
        }
        UserDetails userDetails = userService.loginUser(loginRequest);

        sessionService.authenticateSession(userDetails, request, response);

        return "redirect:/posts";
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        model.addAttribute("user", user);
        return "profile";
    }

}
