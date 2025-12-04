package com.it.ai_mentoring_system.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Service
public class GrokApiService {

    @Value("${grok.api.url}")
    private String apiUrl;

    @Value("${grok.api.key}")
    private String apiKey;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public GrokApiService() {
        this.webClient = WebClient.builder().build();
        this.objectMapper = new ObjectMapper();
    }

    public String generateQuiz(String topic, String difficulty, int numberOfQuestions) {
        String prompt = String.format(
            "Generate a quiz on the topic: %s with difficulty level: %s. " +
            "Create exactly %d questions. " +
            "Return the response in JSON format with the following structure: " +
            "{\"questions\": [{\"question\": \"...\", \"options\": [\"...\", \"...\", \"...\", \"...\"], \"correctAnswer\": 0, \"explanation\": \"...\"}]}",
            topic, difficulty, numberOfQuestions
        );

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "grok-beta");
        requestBody.put("messages", new Object[]{
            Map.of("role", "user", "content", prompt)
        });
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 2000);

        try {
            String response = webClient.post()
                    .uri(apiUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode jsonResponse = objectMapper.readTree(response);
            if (jsonResponse.has("choices") && jsonResponse.get("choices").isArray() && jsonResponse.get("choices").size() > 0) {
                String content = jsonResponse.get("choices").get(0).get("message").get("content").asText();
                return content;
            }
            return "{\"questions\": []}";
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"questions\": []}";
        }
    }

    public String analyzeQuizResults(String questionsJson, String answersJson) {
        String prompt = String.format(
            "Analyze the following quiz results. " +
            "Questions: %s\n" +
            "Student Answers: %s\n" +
            "Provide a detailed analysis including: " +
            "1. List of strengths (topics/questions answered correctly) " +
            "2. List of weaknesses (topics/questions answered incorrectly) " +
            "3. Overall performance analysis " +
            "Return the response in JSON format: " +
            "{\"strengths\": [\"...\"], \"weaknesses\": [\"...\"], \"analysis\": \"...\"}",
            questionsJson, answersJson
        );

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "grok-beta");
        requestBody.put("messages", new Object[]{
            Map.of("role", "user", "content", prompt)
        });
        requestBody.put("temperature", 0.5);
        requestBody.put("max_tokens", 1500);

        try {
            String response = webClient.post()
                    .uri(apiUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode jsonResponse = objectMapper.readTree(response);
            if (jsonResponse.has("choices") && jsonResponse.get("choices").isArray() && jsonResponse.get("choices").size() > 0) {
                return jsonResponse.get("choices").get(0).get("message").get("content").asText();
            }
            return "{\"strengths\": [], \"weaknesses\": [], \"analysis\": \"Analysis unavailable\"}";
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"strengths\": [], \"weaknesses\": [], \"analysis\": \"Analysis unavailable\"}";
        }
    }
}

