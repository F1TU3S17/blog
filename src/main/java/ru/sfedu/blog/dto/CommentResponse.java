package ru.sfedu.blog.dto;

import java.time.Instant;

public record CommentResponse(
    Long id,
    String text,
    Long authorId,
    Instant createdAt
) {}