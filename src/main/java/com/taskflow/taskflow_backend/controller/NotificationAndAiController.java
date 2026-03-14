package com.taskflow.taskflow_backend.controller;


import com.taskflow.taskflow_backend.dto.request.AiTaskGenerationRequest;
import com.taskflow.taskflow_backend.dto.response.ApiResponse;
import com.taskflow.taskflow_backend.dto.response.NotificationResponse;
import com.taskflow.taskflow_backend.services.AiTaskService;
import com.taskflow.taskflow_backend.services.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Endpoints for notifications and AI-powered task generation.
 */
@RestController
@RequiredArgsConstructor
public class NotificationAndAiController {

    private final NotificationService notificationService;
    private final AiTaskService aiTaskService;

    // ─── Notifications ────────────────────────────────────────────────────────

    @GetMapping("/api/notifications")
    @Tag(name = "Notifications")
    @Operation(summary = "Get notifications for current user")
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getNotifications(
            @RequestParam(required = false, defaultValue = "false") Boolean unreadOnly,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.getNotifications(userDetails.getId(), unreadOnly, pageable)));
    }

    @GetMapping("/api/notifications/unread-count")
    @Tag(name = "Notifications")
    @Operation(summary = "Get count of unread notifications")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.getUnreadCount(userDetails.getId())));
    }

    @PatchMapping("/api/notifications/{id}/read")
    @Tag(name = "Notifications")
    @Operation(summary = "Mark a notification as read")
    public ResponseEntity<ApiResponse<Void>> markRead(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        notificationService.markRead(id, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", null));
    }

    @PatchMapping("/api/notifications/read-all")
    @Tag(name = "Notifications")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<ApiResponse<Void>> markAllRead(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        notificationService.markAllRead(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", null));
    }

    // ─── AI Task Generation ───────────────────────────────────────────────────

    @PostMapping("/api/ai/generate-task")
    @Tag(name = "AI Features")
    @Operation(summary = "AI-powered task description generation")
    public ResponseEntity<ApiResponse<Map<String, String>>> generateTask(
            @Valid @RequestBody AiTaskGenerationRequest request) {
        Map<String, String> result = aiTaskService.generateTaskDescription(
                request.getUserInput(), request.getContext());
        return ResponseEntity.ok(ApiResponse.success("Task generated", result));
    }
}

