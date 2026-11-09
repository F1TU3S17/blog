package ru.sfedu.blog.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.sfedu.blog.entity.PostLike;

import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    Optional<PostLike> findByPost_Id(Long postId);
    void deleteByPost_IdAndUser_Id(Long postId, Long userId);
}