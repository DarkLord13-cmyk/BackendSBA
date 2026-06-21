package com.assignment.coreapi.dto;

import com.assignment.coreapi.entity.AuthorType;
import lombok.Data;

@Data
public class CreatePostRequest {
    private Long authorId;
    private AuthorType authorType;
    private String content;
}
