package com.taskflow.taskflow_backend.dto.response;

import com.taskflow.taskflow_backend.entity.Task;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {
    private Long id;
    private String title;
    private String description;
    private Task.TaskStatus status;
    private Task.TaskPriority priority;
    private LocalDate dueDate;
    private LocalDateTime completedAt;
    private Long projectId;
    private String projectName;
    private UserSummaryResponse creator;
    private UserSummaryResponse assignee;
    private long commentCount;
    private long attachmentCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

