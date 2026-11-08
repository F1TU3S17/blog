package ru.sfedu.blog.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sfedu.blog.entity.Comment;
import ru.sfedu.blog.entity.Post;
import ru.sfedu.blog.entity.User;
import ru.sfedu.blog.entity.UserRoleEnum;
import ru.sfedu.blog.exception.AppErrorEnum;
import ru.sfedu.blog.exception.AppException;
import ru.sfedu.blog.repository.CommentRepository;
import ru.sfedu.blog.security.CustomUserDetails;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {
    private final CommentRepository commentRepository;

    @Transactional
    public void create(String text, Post post, User author) {
        Comment comment = new Comment(
            text,
            post,
            author
        );

        comment = commentRepository.save(comment);
        log.info("Comment with id: {} saved. Author user with id: {}", comment.getId(), author.getId());
    }

    @Transactional
    public void delete(Long id, CustomUserDetails user) {
        Comment comment = commentRepository.findById(id).orElseThrow(() -> {
            log.warn("Comment with id: {} is not exists", id);
            return new AppException(AppErrorEnum.COMMENT_NOT_FOUND);
        });

        if (!user.hasRole(UserRoleEnum.ADMIN) && !user.getId().equals(comment.getAuthor().getId())) {
            log.warn("Current user can`t edit comment with id: {}", id);
            throw new AppException(AppErrorEnum.USER_CANT_EDIT_COMMENT);
        }

        commentRepository.deleteById(id);

        log.info("Comment with id: {} deleted by {}", id, user.getId());
    }
}
