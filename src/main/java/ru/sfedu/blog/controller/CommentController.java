package ru.sfedu.blog.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import ru.sfedu.blog.dto.CreateCommentRequest;
import ru.sfedu.blog.entity.Post;
import ru.sfedu.blog.entity.User;
import ru.sfedu.blog.repository.PostRepositoryImplt;
import ru.sfedu.blog.repository.UserRepositoryImplt;
import ru.sfedu.blog.security.CustomUserDetails;
import ru.sfedu.blog.service.CommentService;

@Controller
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;
    private final UserRepositoryImplt userRepositoryImplt;
    private final PostRepositoryImplt postRepository;

    @PostMapping("/{postId}")
    public String createComment(
        @PathVariable("postId") Long postId,
        @Valid @ModelAttribute CreateCommentRequest createCommentRequest,
        @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        User user = userRepositoryImplt.getById(currentUser.getId());
        Post post = postRepository.getById(postId);
        commentService.create(createCommentRequest.text(), post, user);

        return "redirect:/posts/" + postId;
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment (
            @PathVariable("commentId") Long commentId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        commentService.delete(commentId, user);
    }
}
