package ru.sfedu.blog.dto;

public record RegisterRequest(
        String username,
        String email,
        String password
) {}