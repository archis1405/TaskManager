package com.taskflow.taskflow_backend.services;



import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * AI-powered task description and summary generation using a generative AI model.
 * Integrates with an external LLM API (configurable endpoint).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiTaskService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.ai.api-url:https://api.anthropic.com/v1/messages}")
    private String aiApiUrl;

    @Value("${app.ai.api-key:}")
    private String apiKey;

    @Value("${app.ai.model:claude-3-haiku-20240307}")
    private String model;

    @Value("${app.ai.enabled:false}")
    private boolean aiEnabled;

    /**
     * Generate a task description based on a brief user input.
     *
     * @param userInput  Short description of what the task is about
     * @param context    Optional project/team context
     * @return           AI-generated description and suggested title
     */
    public Map<String, String> generateTaskDescription(String userInput, String context) {
        if (!aiEnabled || apiKey.isBlank()) {
            return Map.of(
                    "title", userInput,
                    "description", "AI generation is not configured. Please set app.ai.api-key and app.ai.enabled=true.",
                    "note", "AI feature disabled"
            );
        }

        String systemPrompt = """
                You are an expert project manager helping to create clear, actionable task descriptions.
                Given a brief user input, generate:
                1. A clear, concise task title (max 100 chars)
                2. A detailed description with acceptance criteria
                3. Suggested priority (LOW, MEDIUM, HIGH, URGENT)
                
                Respond ONLY with valid JSON in this format:
                {"title": "...", "description": "...", "priority": "MEDIUM", "summary": "..."}
                """;

        String userMessage = context != null && !context.isBlank()
                ? String.format("Context: %s\n\nTask input: %s", context, userInput)
                : "Task input: " + userInput;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", apiKey);
            headers.set("anthropic-version", "2023-06-01");

            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "max_tokens", 500,
                    "system", systemPrompt,
                    "messages", List.of(Map.of("role", "user", "content", userMessage))
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(aiApiUrl, HttpMethod.POST, entity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            String content = root.path("content").get(0).path("text").asText();

            // Parse JSON response from AI
            JsonNode result = objectMapper.readTree(content);
            return Map.of(
                    "title", result.path("title").asText(userInput),
                    "description", result.path("description").asText(""),
                    "priority", result.path("priority").asText("MEDIUM"),
                    "summary", result.path("summary").asText("")
            );

        } catch (Exception e) {
            log.error("AI task generation failed: {}", e.getMessage());
            return Map.of(
                    "title", userInput,
                    "description", "Failed to generate AI description. Please write manually.",
                    "error", e.getMessage()
            );
        }
    }
}
