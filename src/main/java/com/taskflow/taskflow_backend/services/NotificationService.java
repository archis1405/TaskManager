package com.taskflow.taskflow_backend.services;


import com.taskflow.taskflow_backend.dto.response.NotificationResponse;
import com.taskflow.taskflow_backend.entity.Comment;
import com.taskflow.taskflow_backend.entity.Notification;
import com.taskflow.taskflow_backend.entity.Task;
import com.taskflow.taskflow_backend.entity.User;
import com.taskflow.taskflow_backend.exception.ResourceNotFoundException;
import com.taskflow.taskflow_backend.repository.NotificationRepository;
import com.taskflow.taskflow_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Async
    public void notifyTaskAssigned(Task task, User assignee, User assigner) {
        String title = "New Task Assigned";
        String message = String.format("%s assigned you a task: \"%s\"",
                assigner.getFullName() != null ? assigner.getFullName() : assigner.getUsername(),
                task.getTitle());

        createAndSend(assignee, title, message,
                Notification.NotificationType.TASK_ASSIGNED, task.getId(), "TASK");
    }

    @Async
    public void notifyCommentAdded(Task task, Comment comment, User commenter) {
        // Notify task creator if different from commenter
        if (task.getCreator() != null && !task.getCreator().getId().equals(commenter.getId())) {
            String message = String.format("%s commented on task: \"%s\"",
                    commenter.getFullName() != null ? commenter.getFullName() : commenter.getUsername(),
                    task.getTitle());
            createAndSend(task.getCreator(), "New Comment", message,
                    Notification.NotificationType.COMMENT_ADDED, task.getId(), "TASK");
        }

        // Also notify assignee if different from commenter and creator
        if (task.getAssignee() != null
                && !task.getAssignee().getId().equals(commenter.getId())
                && (task.getCreator() == null || !task.getAssignee().getId().equals(task.getCreator().getId()))) {
            String message = String.format("%s commented on task assigned to you: \"%s\"",
                    commenter.getFullName() != null ? commenter.getFullName() : commenter.getUsername(),
                    task.getTitle());
            createAndSend(task.getAssignee(), "New Comment", message,
                    Notification.NotificationType.COMMENT_ADDED, task.getId(), "TASK");
        }
    }

    @Transactional
    protected void createAndSend(User recipient, String title, String message,
                                 Notification.NotificationType type, Long refId, String refType) {
        Notification notification = Notification.builder()
                .user(recipient)
                .title(title)
                .message(message)
                .type(type)
                .referenceId(refId)
                .referenceType(refType)
                .build();

        notification = notificationRepository.save(notification);

        // Push via WebSocket
        NotificationResponse response = toResponse(notification);
        try {
            messagingTemplate.convertAndSendToUser(
                    recipient.getEmail(),
                    "/queue/notifications",
                    response);
        } catch (Exception e) {
            log.warn("Failed to send WebSocket notification to {}: {}", recipient.getEmail(), e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(Long userId, Boolean unreadOnly, Pageable pageable) {
        if (Boolean.TRUE.equals(unreadOnly)) {
            return notificationRepository.findByUserIdAndReadOrderByCreatedAtDesc(userId, false, pageable)
                    .map(this::toResponse);
        }
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndRead(userId, false);
    }

    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }

    @Transactional
    public void markRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));
        if (!notification.getUser().getId().equals(userId)) {
            throw new com.taskflow.exception.AccessDeniedException("Not your notification");
        }
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .type(n.getType())
                .referenceId(n.getReferenceId())
                .referenceType(n.getReferenceType())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}

