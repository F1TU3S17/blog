package ru.sfedu.blog.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.sfedu.blog.entity.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {
}