package com.taskflow.taskflow_backend.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(max = 100, message = "Full name must be at most 100 characters")
    private String fullName;

    @Size(max = 500, message = "Bio must be at most 500 characters")
    private String bio;
}

