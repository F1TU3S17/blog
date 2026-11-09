package ru.sfedu.blog.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sfedu.blog.entity.Post;
import ru.sfedu.blog.entity.PostLike;
import ru.sfedu.blog.entity.User;
import ru.sfedu.blog.repository.PostLikeRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class LikeService {
    private final PostLikeRepository postLikeRepository;

    @Transactional
    public void addLike(User user, Post post) {
        PostLike postLike = new PostLike(
                user,
                post
        );

        postLike = postLikeRepository.save(postLike);
        log.info("Post with id: {} liked by user with id: {}. Like id: {}", post.getId(), user.getId(), postLike.getId());
    }

    @Transactional
    public void deleteLikeByUserAndPost(User user, Post post) {
        postLikeRepository.deleteByPost_IdAndUser_Id(post.getId(), user.getId());
        log.info("Like user with id: {} deleted in post with id: {}", user.getId(), post.getId());
    }
}
