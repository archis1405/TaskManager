package com.taskflow.taskflow_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentResponse {
    private Long id;
    private String originalName;
    private String contentType;
    private Long fileSize;
    private String downloadUrl;
    private UserSummaryResponse uploader;
    private LocalDateTime createdAt;
}

