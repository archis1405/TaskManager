package com.taskflow.taskflow_backend.dto.request;


import com.taskflow.taskflow_backend.entity.Task;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TaskRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 200, message = "Title must be 1-200 characters")
    private String title;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    private Task.TaskStatus status;

    private Task.TaskPriority priority;

    private LocalDate dueDate;

    private Long projectId;

    private Long assigneeId;
}

