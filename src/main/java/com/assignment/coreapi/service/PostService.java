package com.assignment.coreapi.service;

import com.assignment.coreapi.dto.CreateCommentRequest;
import com.assignment.coreapi.dto.CreatePostRequest;
import com.assignment.coreapi.dto.LikePostRequest;
import com.assignment.coreapi.entity.AuthorType;
import com.assignment.coreapi.entity.Bot;
import com.assignment.coreapi.entity.Comment;
import com.assignment.coreapi.entity.Post;
import com.assignment.coreapi.repository.BotRepository;
import com.assignment.coreapi.repository.CommentRepository;
import com.assignment.coreapi.repository.PostRepository;
import com.assignment.coreapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final BotRepository botRepository;
    private final UserRepository userRepository;
    private final ViralityService viralityService;
    private final NotificationService notificationService;

    @Transactional
    public Post createPost(CreatePostRequest request) {
        // Basic validation could go here (e.g. checking if author exists)
        Post post = new Post();
        post.setAuthorId(request.getAuthorId());
        post.setAuthorType(request.getAuthorType());
        post.setContent(request.getContent());
        return postRepository.save(post);
    }

    @Transactional
    public Comment addComment(Long postId, CreateCommentRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));

        int depthLevel = 1;
        if (request.getParentCommentId() != null) {
            Comment parentComment = commentRepository.findById(request.getParentCommentId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent comment not found"));
            depthLevel = parentComment.getDepthLevel() + 1;
        }

        // 1. Guardrails
        if (request.getAuthorType() == AuthorType.BOT) {
            viralityService.checkAndIncrementBotHorizontalCap(postId);
            
            // If post is by a human, check bot cooldown and notify
            if (post.getAuthorType() == AuthorType.USER) {
                viralityService.checkAndSetBotCooldownCap(request.getAuthorId(), post.getAuthorId());
                
                Bot bot = botRepository.findById(request.getAuthorId()).orElse(null);
                String botName = bot != null ? bot.getName() : "UnknownBot";
                notificationService.handleBotInteractionNotification(post.getAuthorId(), botName);
            }
            
            viralityService.updateViralityScore(postId, 1);
        } else {
            viralityService.updateViralityScore(postId, 50);
        }

        viralityService.checkVerticalCap(depthLevel);

        // 2. Database Action
        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setAuthorId(request.getAuthorId());
        comment.setAuthorType(request.getAuthorType());
        comment.setContent(request.getContent());
        comment.setDepthLevel(depthLevel);
        
        return commentRepository.save(comment);
    }

    @Transactional
    public void likePost(Long postId, LikePostRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));

        if (request.getAuthorType() == AuthorType.USER) {
            viralityService.updateViralityScore(postId, 20);
        }
        
        // If liked by bot, requirements don't explicitly specify points, assuming none or handle bot notifications if needed.
        if (request.getAuthorType() == AuthorType.BOT && post.getAuthorType() == AuthorType.USER) {
             viralityService.checkAndSetBotCooldownCap(request.getAuthorId(), post.getAuthorId());
             Bot bot = botRepository.findById(request.getAuthorId()).orElse(null);
             String botName = bot != null ? bot.getName() : "UnknownBot";
             notificationService.handleBotInteractionNotification(post.getAuthorId(), botName);
        }
    }
}
