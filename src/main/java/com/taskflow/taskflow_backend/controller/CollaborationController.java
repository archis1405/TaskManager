package com.taskflow.taskflow_backend.controller;


import com.taskflow.taskflow_backend.dto.request.CommentRequest;
import com.taskflow.taskflow_backend.dto.response.ApiResponse;
import com.taskflow.taskflow_backend.dto.response.AttachmentResponse;
import com.taskflow.taskflow_backend.dto.response.CommentResponse;
import com.taskflow.taskflow_backend.services.AttachmentService;
import com.taskflow.taskflow_backend.services.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@RestController
@RequiredArgsConstructor
@Tag(name = "Collaboration", description = "Comments and attachments")
public class CollaborationController {

    private final CommentService commentService;
    private final AttachmentService attachmentService;

    // ─── Comments ────────────────────────────────────────────────────────────

    @PostMapping("/api/tasks/{taskId}/comments")
    @Operation(summary = "Add a comment to a task")
    public ResponseEntity<ApiResponse<CommentResponse>> addComment(
            @PathVariable Long taskId,
            @Valid @RequestBody CommentRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        CommentResponse comment = commentService.addComment(taskId, request, userDetails.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Comment added", comment));
    }

    @GetMapping("/api/tasks/{taskId}/comments")
    @Operation(summary = "Get comments for a task")
    public ResponseEntity<ApiResponse<Page<CommentResponse>>> getComments(
            @PathVariable Long taskId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(commentService.getTaskComments(taskId, pageable)));
    }

    @PutMapping("/api/comments/{commentId}")
    @Operation(summary = "Edit a comment")
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        CommentResponse comment = commentService.updateComment(commentId, request, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Comment updated", comment));
    }

    @DeleteMapping("/api/comments/{commentId}")
    @Operation(summary = "Delete a comment")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        commentService.deleteComment(commentId, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Comment deleted", null));
    }

    // ─── Attachments ─────────────────────────────────────────────────────────

    @PostMapping(value = "/api/tasks/{taskId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a file attachment to a task")
    public ResponseEntity<ApiResponse<AttachmentResponse>> uploadAttachment(
            @PathVariable Long taskId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        AttachmentResponse attachment = attachmentService.uploadAttachment(taskId, file, userDetails.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("File uploaded", attachment));
    }

    @GetMapping("/api/tasks/{taskId}/attachments")
    @Operation(summary = "List attachments for a task")
    public ResponseEntity<ApiResponse<List<AttachmentResponse>>> getAttachments(@PathVariable Long taskId) {
        return ResponseEntity.ok(ApiResponse.success(attachmentService.getTaskAttachments(taskId)));
    }

    @GetMapping("/api/attachments/{attachmentId}/download")
    @Operation(summary = "Download an attachment")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long attachmentId) {
        Resource resource = attachmentService.loadAttachmentAsResource(attachmentId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @DeleteMapping("/api/attachments/{attachmentId}")
    @Operation(summary = "Delete an attachment")
    public ResponseEntity<ApiResponse<Void>> deleteAttachment(
            @PathVariable Long attachmentId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        attachmentService.deleteAttachment(attachmentId, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Attachment deleted", null));
    }
}
