package com.assignment.coreapi.controller;

import com.assignment.coreapi.dto.CreateCommentRequest;
import com.assignment.coreapi.dto.CreatePostRequest;
import com.assignment.coreapi.dto.LikePostRequest;
import com.assignment.coreapi.entity.Comment;
import com.assignment.coreapi.entity.Post;
import com.assignment.coreapi.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Post createPost(@RequestBody CreatePostRequest request) {
        return postService.createPost(request);
    }

    @PostMapping("/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public Comment addComment(@PathVariable Long postId, @RequestBody CreateCommentRequest request) {
        return postService.addComment(postId, request);
    }

    @PostMapping("/{postId}/like")
    @ResponseStatus(HttpStatus.OK)
    public void likePost(@PathVariable Long postId, @RequestBody LikePostRequest request) {
        postService.likePost(postId, request);
    }
}
