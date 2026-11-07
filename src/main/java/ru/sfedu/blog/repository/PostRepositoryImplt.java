package ru.sfedu.blog.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import ru.sfedu.blog.entity.Post;
import ru.sfedu.blog.exception.AppErrorEnum;
import ru.sfedu.blog.exception.AppException;

@Repository
@RequiredArgsConstructor
@Slf4j
public class PostRepositoryImplt {
    private final PostRepository postRepository;

    public Post getById(Long id) {
        log.info("Find post by id: {}", id);
        return postRepository.findById(id).orElseThrow(() -> {
            log.warn("Post with id: {} is not found", id);
            return new AppException(AppErrorEnum.POST_NOT_FOUND);
        });
    }

    public Page<Post> getAll(Pageable pageable) {
        return postRepository.findAll(pageable);
    }

    public Post save(Post post) {
        post = postRepository.save(post);
        log.info("Post with id: {} saved", post.getId());
        return post;
    }

    public void delete(Long id) {
        postRepository.deleteById(id);
        log.info("Deleted post with id: {}", id);
    }
}
