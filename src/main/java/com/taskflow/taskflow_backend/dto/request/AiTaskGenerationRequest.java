package com.taskflow.taskflow_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiTaskGenerationRequest {

    @NotBlank(message = "Input is required")
    @Size(min = 5, max = 500, message = "Input must be 5-500 characters")
    private String userInput;

    private String context; // Optional additional context
}