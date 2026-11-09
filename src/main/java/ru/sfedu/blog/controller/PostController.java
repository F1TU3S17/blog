package ru.sfedu.blog.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.sfedu.blog.dto.PostCreationRequest;
import ru.sfedu.blog.dto.PostResponse;
import ru.sfedu.blog.dto.PostUpdateRequest;
import ru.sfedu.blog.dto.PostWithCommentsResponse;
import ru.sfedu.blog.entity.Post;
import ru.sfedu.blog.entity.User;
import ru.sfedu.blog.repository.PostRepositoryImplt;
import ru.sfedu.blog.repository.UserRepositoryImplt;
import ru.sfedu.blog.security.CustomUserDetails;
import ru.sfedu.blog.service.LikeService;
import ru.sfedu.blog.service.PostService;

@Controller
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {
    private final PostService postService;
    private final LikeService likeService;

    private final PostRepositoryImplt postRepository;
    private final UserRepositoryImplt userRepository;

    @GetMapping
    public String getLapePostsPage(
            @PageableDefault(
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable,
            @AuthenticationPrincipal
            CustomUserDetails user,
            Model model
    ) {
        Page<PostResponse> posts = postService.getPosts(pageable, getUserId(user));

        model.addAttribute("user", user);
        model.addAttribute("posts", posts);
        return "posts";
    }

    @GetMapping("/{id}")
    public String getPost(
            @AuthenticationPrincipal CustomUserDetails user,
            Model model,
            @PathVariable("id") Long id
    ) {
        PostWithCommentsResponse post = postService.getPostById(id, getUserId(user));
        model.addAttribute("post", post);
        model.addAttribute("user", user);

        return "post";
    }

    @GetMapping("/create")
    public String getCreatePostPage(
            @AuthenticationPrincipal CustomUserDetails user,
            Model model
    ) {
        model.addAttribute("user", user);
        return "post-create";
    }

    @PostMapping
    public String createPost(
            @ModelAttribute PostCreationRequest postCreationRequest,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        postService.create(postCreationRequest, user);
        return "redirect:/posts";
    }

    @PutMapping("/{id}")
    public String updatePost(
            @ModelAttribute PostUpdateRequest postUpdateRequest,
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable("id") Long id
    ) {
        postService.updatePost(id, postUpdateRequest, user);
        return "redirect:/posts/" + id;
    }

    @DeleteMapping("/{id}")
    public String deletePost(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable("id") Long id
    ) {
        postService.delete(id, user);

        return "redirect:/posts";
    }

    @PostMapping("/{id}/like")
    public String likePost(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            HttpServletRequest request
    ) {
        User user = userRepository.getById(currentUser.getId());
        Post post = postRepository.getById(id);

        likeService.addLike(user, post);

        String referer = request.getHeader("Referer");
        return "redirect:" + referer;
    }

    @DeleteMapping("/{id}/like")
    public String deletePost(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            HttpServletRequest request
    ) {
        User user = userRepository.getById(currentUser.getId());
        Post post = postRepository.getById(id);

        likeService.deleteLikeByUserAndPost(user, post);

        String referer = request.getHeader("Referer");
        return "redirect:" + referer;
    }

    private Long getUserId(CustomUserDetails user) {
        return user != null ? user.getId() : null;
    }

}
