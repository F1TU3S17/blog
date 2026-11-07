package ru.sfedu.blog.dto;

import jakarta.validation.constraints.Size;

public record PostCreationRequest(
        @Size(min=3, max=100)
        String title,
        String content
) {
}
