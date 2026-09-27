package com.example.team_workspace.activity.dto;

import java.time.Instant;

import com.example.team_workspace.activity.domain.TaskComment;

public record CommentResponse(
        Long id,
        Long taskId,
        String content,
        Long authorId,
        String authorName,
        String authorEmail,
        Instant createdAt
) {

    public static CommentResponse from(TaskComment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getTask().getId(),
                comment.getContent(),
                comment.getAuthor().getId(),
                authorName(comment),
                comment.getAuthor().getEmail(),
                comment.getCreatedAt()
        );
    }

    private static String authorName(TaskComment comment) {
        String fullName = (
                comment.getAuthor().getFirstName() + " " + comment.getAuthor().getLastName()
        ).trim();
        return fullName.isEmpty() ? comment.getAuthor().getEmail() : fullName;
    }
}
