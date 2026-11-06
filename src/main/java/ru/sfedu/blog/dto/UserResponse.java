package ru.sfedu.blog.dto;


import ru.sfedu.blog.entity.UserRoleEnum;

public record UserResponse (
    Long id,
    String username,
    String email,
    UserRoleEnum role,
    String passwordHash
) {
}