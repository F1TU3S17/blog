package ru.sfedu.blog.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sfedu.blog.dto.PostCreationRequest;
import ru.sfedu.blog.dto.PostResponse;
import ru.sfedu.blog.dto.PostUpdateRequest;
import ru.sfedu.blog.entity.Post;
import ru.sfedu.blog.entity.UserRoleEnum;
import ru.sfedu.blog.exception.AppErrorEnum;
import ru.sfedu.blog.exception.AppException;
import ru.sfedu.blog.repository.PostRepositoryImplt;
import ru.sfedu.blog.repository.UserRepositoryImplt;
import ru.sfedu.blog.security.CustomUserDetails;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostService {
    private final PostRepositoryImplt postRepository;
    private final UserRepositoryImplt userRepository;

    @Transactional
    public void create(PostCreationRequest request, CustomUserDetails user) {
        Post post = new Post(
                request.title(),
                request.content(),
                userRepository.getByLogin(user.getUsername())
        );

        post = postRepository.save(post);
        log.info("Post with id: {} created", post.getId());

    }

    @Transactional
    public void updatePost(Long id, PostUpdateRequest request, CustomUserDetails user) {
        Post post = postRepository.getById(id);

        if (!post.getAuthor().getId().equals(user.getId())) {
            log.warn("User with id: {}, can`t edit post with id; {}", user.getId(), id);
            throw new AppException(AppErrorEnum.USER_CANT_EDIT_POST);
        }

        if (request.title() != null && !request.title().isEmpty()) {
            post.setTitle(request.title());
        }

        if (request.content() != null && !request.content().isEmpty()) {
            post.setTitle(request.content());
        }

        post.toDto();
    }

    @Transactional(readOnly = true)
    public PostResponse getPostById(Long id) {
        return postRepository.getById(id).toDto();
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> getPosts(Pageable pageable) {
        return postRepository.getAll(pageable)
                .map(Post::toDto);
    }

    public void delete(Long id, CustomUserDetails user) {
        Post post = postRepository.getById(id);

        if (!user.hasRole(UserRoleEnum.ADMIN) && !post.getAuthor().getId().equals(user.getId())) {
            log.warn("User with id: {}, can`t delete post with id; {}", user.getId(), id);
            throw new AppException(AppErrorEnum.USER_CANT_EDIT_POST);
        }

        postRepository.delete(post.getId());

    }
}
