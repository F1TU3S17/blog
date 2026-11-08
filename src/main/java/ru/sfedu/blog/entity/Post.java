package ru.sfedu.blog.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import ru.sfedu.blog.dto.PostResponse;
import ru.sfedu.blog.dto.PostWithCommentsResponse;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="posts")
@RequiredArgsConstructor
@Setter
@Getter
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @OneToMany(mappedBy="post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    @Column(nullable = false)
    private Instant createdAt;

    public Post(String title, String content, User author) {
        this.title = title;
        this.content = content;
        this.author = author;
        this.createdAt = Instant.now();
    }

    public PostResponse toDto() {
        return new PostResponse(
                id,
                title,
                content,
                author.toDto(),
                createdAt
        );
    }

    public PostWithCommentsResponse toDtoWithComments() {
        return new PostWithCommentsResponse(
                id,
                title,
                content,
                author.toDto(),
                createdAt,
                comments.stream()
                        .map(Comment::toDto)
                        .toList()
        );
    }
}
