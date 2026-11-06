package ru.sfedu.blog.entity;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum UserRoleEnum {
    USER("USER"),
    ADMIN("ADMIN");

    private final String name;

    public String withPrefix() {
        return "ROLE_" + name;
    }
}
