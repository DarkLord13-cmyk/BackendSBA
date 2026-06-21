package com.assignment.coreapi.dto;

import com.assignment.coreapi.entity.AuthorType;
import lombok.Data;

@Data
public class CreateCommentRequest {
    private Long authorId;
    private AuthorType authorType;
    private String content;
    private Long parentCommentId; // optional, to determine depth level
}
