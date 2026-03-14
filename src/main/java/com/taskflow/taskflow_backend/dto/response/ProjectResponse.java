package com.taskflow.taskflow_backend.dto.response;


import com.taskflow.taskflow_backend.entity.Project;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/** Project response DTO. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {
    private Long id;
    private String name;
    private String description;
    private Project.ProjectStatus status;
    private UserSummaryResponse owner;
    private List<UserSummaryResponse> members;
    private int memberCount;
    private long taskCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

