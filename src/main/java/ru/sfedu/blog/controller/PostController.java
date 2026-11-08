package ru.sfedu.blog.controller;

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
import ru.sfedu.blog.security.CustomUserDetails;
import ru.sfedu.blog.service.PostService;

@Controller
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {
    private final PostService postService;

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
        Page<PostResponse> posts = postService.getPosts(pageable);

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
        PostWithCommentsResponse post = postService.getPostById(id);
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


}
