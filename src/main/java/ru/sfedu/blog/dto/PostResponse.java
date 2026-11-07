package ru.sfedu.blog.dto;

import java.time.Instant;

public record PostResponse(
        Long id,
        String title,
        String content,
        UserResponse author,
        Instant createdAt
) {
}