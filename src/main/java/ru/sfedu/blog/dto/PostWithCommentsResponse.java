package ru.sfedu.blog.dto;

import java.time.Instant;
import java.util.List;

public record PostWithCommentsResponse(
        Long id,
        String title,
        String content,
        UserResponse author,
        Instant createdAt,
        List<CommentResponse> comments,
        int likesCount,
        boolean currentUserHasLike
) {
}
