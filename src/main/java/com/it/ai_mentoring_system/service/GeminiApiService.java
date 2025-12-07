package com.it.ai_mentoring_system.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

@Service
public class GeminiApiService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiApiService.class);

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.model:gemini-2.5-flash}")
    private String defaultModel;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private long lastRequestTime = 0;
    private static final long MIN_REQUEST_INTERVAL = 2000; // 2 seconds between requests

    public GeminiApiService(@Value("${gemini.api.key}") String apiKey) {
        // Initialize WebClient with API key in header (correct method)
        this.webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("x-goog-api-key", apiKey)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Wait to avoid rate limiting
     */
    private void waitForRateLimit() {
        long now = System.currentTimeMillis();
        long timeSinceLastRequest = now - lastRequestTime;

        if (timeSinceLastRequest < MIN_REQUEST_INTERVAL) {
            long waitTime = MIN_REQUEST_INTERVAL - timeSinceLastRequest;
            try {
                logger.debug("Rate limiting: waiting {}ms", waitTime);
                Thread.sleep(waitTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        lastRequestTime = System.currentTimeMillis();
    }

    public String generateQuiz(String topic, String difficulty, int numberOfQuestions) {
        // Improved prompt with stricter JSON formatting instructions
        String prompt = String.format(
                "Generate a quiz with exactly %d multiple choice questions about '%s' at '%s' difficulty level.\n\n" +
                        "CRITICAL: Return ONLY pure JSON with NO markdown, NO explanations, NO code blocks.\n\n" +
                        "Format (use this EXACT structure):\n" +
                        "{\"questions\":[{\"question\":\"text\",\"options\":[\"a\",\"b\",\"c\",\"d\"],\"correctAnswer\":0,\"explanation\":\"text\"}]}\n\n" +
                        "Rules:\n" +
                        "- correctAnswer must be 0, 1, 2, or 3 (index of correct option)\n" +
                        "- Keep questions concise (under 100 characters)\n" +
                        "- Keep options concise (under 50 characters each)\n" +
                        "- Keep explanations brief (under 100 characters)\n" +
                        "- Use double quotes for all strings\n" +
                        "- Start response with { and end with }\n" +
                        "- No trailing commas",
                numberOfQuestions, topic, difficulty
        );

        Map<String, Object> content = new HashMap<>();
        content.put("parts", List.of(Map.of("text", prompt)));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(content));

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.4);  // Lower temperature for more consistent JSON
        generationConfig.put("maxOutputTokens", 4000);  // Increased token limit
        generationConfig.put("topP", 0.8);
        generationConfig.put("topK", 40);
        requestBody.put("generationConfig", generationConfig);

        // Try multiple models with fallback
        String[] modelsToTry = {
                defaultModel,
                "gemini-2.5-flash",
                "gemini-1.5-flash",
                "gemini-1.5-pro",
                "gemini-2.0-flash-exp"
        };

        for (String model : modelsToTry) {
            try {
                // Wait to avoid rate limiting
                waitForRateLimit();

                // API key is now in header, not query parameter
                String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent";

                logger.debug("Attempting quiz generation with model: {}", model);

                String response = webClient.post()
                        .uri(apiUrl)
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(30))
                        .block();

                JsonNode jsonResponse = objectMapper.readTree(response);

                if (jsonResponse.has("candidates") && jsonResponse.get("candidates").isArray()
                        && jsonResponse.get("candidates").size() > 0) {

                    JsonNode candidate = jsonResponse.get("candidates").get(0);
                    if (candidate.has("content") && candidate.get("content").has("parts")) {
                        String content_text = candidate.get("content").get("parts").get(0).get("text").asText();

                        // Clean up the response - remove markdown code blocks if present
                        content_text = cleanJsonResponse(content_text);

                        // Additional cleaning - remove any text before first { and after last }
                        int firstBrace = content_text.indexOf('{');
                        int lastBrace = content_text.lastIndexOf('}');

                        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
                            content_text = content_text.substring(firstBrace, lastBrace + 1);
                        }

                        // Validate JSON structure
                        try {
                            JsonNode quizData = objectMapper.readTree(content_text);
                            if (quizData.has("questions") && quizData.get("questions").isArray()) {
                                int questionCount = quizData.get("questions").size();
                                if (questionCount > 0) {
                                    logger.info("Quiz generated successfully using model: {} ({} questions)", model, questionCount);
                                    return content_text;
                                } else {
                                    logger.warn("Quiz JSON is valid but contains 0 questions");
                                }
                            } else {
                                logger.warn("Quiz JSON missing 'questions' array");
                            }
                        } catch (Exception jsonEx) {
                            logger.error("Invalid JSON from model {}: {}", model, jsonEx.getMessage());
                            logger.debug("Problematic JSON: {}", content_text.substring(0, Math.min(200, content_text.length())));
                        }
                    }
                }

            } catch (WebClientResponseException e) {
                if (e.getStatusCode().value() == 404) {
                    logger.warn("Model {} not found, trying next model...", model);
                } else if (e.getStatusCode().value() == 429) {
                    logger.error("Rate limit exceeded on model {}. Skipping remaining attempts.", model);
                    // Don't throw exception, just return fallback
                    break;
                } else {
                    logger.error("Error with model {}: {} - {}", model, e.getStatusCode(), e.getMessage());
                }
            } catch (Exception e) {
                logger.error("Error generating quiz with model {}: {}", model, e.getMessage());
                logger.debug("Full error: ", e);
            }

            // Small delay between model attempts to avoid rate limiting
            if (model.equals("gemini-2.5-flash") && modelsToTry.length > 1) {
                try {
                    Thread.sleep(2000); // Longer delay for primary model
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            } else {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        // If all models fail, return fallback
        logger.warn("All models failed, returning fallback quiz");
        return generateFallbackQuiz(topic, numberOfQuestions);
    }

    public String analyzeQuizResults(String questionsJson, String answersJson) {
        String prompt = String.format(
                "Analyze the following quiz results. " +
                        "Questions and correct answers: %s\n" +
                        "Student's answers: %s\n\n" +
                        "Provide a detailed analysis. " +
                        "Return ONLY valid JSON in this exact format (no markdown, no extra text): " +
                        "{\"strengths\": [\"strength1\", \"strength2\"], \"weaknesses\": [\"weakness1\", \"weakness2\"], \"analysis\": \"detailed analysis text\"} " +
                        "In strengths, list topics/concepts the student answered correctly. " +
                        "In weaknesses, list topics/concepts the student answered incorrectly. " +
                        "In analysis, provide constructive feedback and study recommendations.",
                questionsJson, answersJson
        );

        Map<String, Object> content = new HashMap<>();
        content.put("parts", List.of(Map.of("text", prompt)));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(content));

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.5);
        generationConfig.put("maxOutputTokens", 1500);
        requestBody.put("generationConfig", generationConfig);

        // Try multiple models with fallback
        String[] modelsToTry = {
                defaultModel,
                "gemini-2.5-flash",
                "gemini-1.5-flash",
                "gemini-1.5-pro",
                "gemini-2.0-flash-exp"
        };

        for (String model : modelsToTry) {
            try {
                // Wait to avoid rate limiting
                waitForRateLimit();

                // API key is now in header, not query parameter
                String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent";

                logger.debug("Attempting quiz analysis with model: {}", model);

                String response = webClient.post()
                        .uri(apiUrl)
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(30))
                        .block();

                JsonNode jsonResponse = objectMapper.readTree(response);

                if (jsonResponse.has("candidates") && jsonResponse.get("candidates").isArray()
                        && jsonResponse.get("candidates").size() > 0) {

                    JsonNode candidate = jsonResponse.get("candidates").get(0);
                    if (candidate.has("content") && candidate.get("content").has("parts")) {
                        String content_text = candidate.get("content").get("parts").get(0).get("text").asText();

                        // Clean up the response
                        content_text = cleanJsonResponse(content_text);

                        // Validate it's valid JSON
                        objectMapper.readTree(content_text);

                        logger.info("Quiz analysis completed successfully using model: {}", model);
                        return content_text;
                    }
                }

            } catch (WebClientResponseException e) {
                if (e.getStatusCode().value() == 404) {
                    logger.warn("Model {} not found for analysis, trying next model...", model);
                } else if (e.getStatusCode().value() == 429) {
                    logger.error("Rate limit exceeded. Please wait and try again later.");
                    throw new RuntimeException("Rate limit exceeded. Please try again in a few minutes.");
                } else {
                    logger.error("Error with model {}: {} - {}", model, e.getStatusCode(), e.getMessage());
                }
            } catch (Exception e) {
                logger.error("Error analyzing quiz with model {}: {}", model, e.getMessage());
            }

            // Small delay between model attempts
            try {
                Thread.sleep(500);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }

        // Fallback if all models fail
        logger.warn("All models failed for analysis, returning fallback response");
        return "{\"strengths\": [], \"weaknesses\": [], \"analysis\": \"Analysis temporarily unavailable. Please try again.\"}";
    }

    /**
     * Clean JSON response by removing markdown code blocks
     */
    private String cleanJsonResponse(String content) {
        content = content.trim();
        if (content.startsWith("```json")) {
            content = content.substring(7);
        }
        if (content.startsWith("```")) {
            content = content.substring(3);
        }
        if (content.endsWith("```")) {
            content = content.substring(0, content.length() - 3);
        }
        return content.trim();
    }

    /**
     * Generate fallback quiz when API fails
     */
    private String generateFallbackQuiz(String topic, int numberOfQuestions) {
        logger.info("Generating fallback quiz for topic: {}", topic);
        return String.format(
                "{\"questions\": [{\"question\": \"Sample question about %s (API temporarily unavailable)\", " +
                        "\"options\": [\"Option A\", \"Option B\", \"Option C\", \"Option D\"], " +
                        "\"correctAnswer\": 0, " +
                        "\"explanation\": \"This is a fallback question. The AI quiz generation service is temporarily unavailable. Please try again later.\"}]}",
                topic
        );
    }
}