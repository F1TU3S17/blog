package ru.sfedu.blog.dto;

import ru.sfedu.blog.entity.UserRoleEnum;

import java.util.List;

public record UserWithPostsResponse(
        Long id,
        String username,
        String email,
        UserRoleEnum role,
        List<PostResponse> postResponse
) {
}
