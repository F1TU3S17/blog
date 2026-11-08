package ru.sfedu.blog.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.sfedu.blog.dto.CommentResponse;

import java.time.Instant;

@Entity
@Table(name="Comments")
@Getter
@Setter
@NoArgsConstructor
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String text;

    @Column(nullable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    public Comment(String text, Post post, User author) {
        this.text = text;
        this.post = post;
        this.author = author;
        this.createdAt = Instant.now();
    }

    public CommentResponse toDto() {
        return new CommentResponse(
                id,
                text,
                author.getId(),
                author.getUsername(),
                createdAt
        );
    }
}
