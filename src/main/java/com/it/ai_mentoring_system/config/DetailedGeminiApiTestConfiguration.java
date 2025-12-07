//package com.it.ai_mentoring_system.config;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.MediaType;
//import org.springframework.web.reactive.function.client.WebClient;
//import org.springframework.web.reactive.function.client.WebClientResponseException;
//
//import java.time.Duration;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//
///**
// * Detailed Gemini API Test Configuration
// * Tests API connectivity and provides comprehensive diagnostics
// */
//@Configuration
//public class DetailedGeminiApiTestConfiguration {
//
//    private static final Logger logger = LoggerFactory.getLogger(DetailedGeminiApiTestConfiguration.class);
//
//    @Value("${gemini.api.key:}")
//    private String apiKey;
//
//    @Bean
//    public CommandLineRunner testGeminiApi() {
//        return args -> {
//            printBanner();
//
//            // Step 1: Check API Key Configuration
//            if (!checkApiKeyConfiguration()) {
//                return;
//            }
//
//            // Create WebClient with API key in header
//            WebClient webClient = WebClient.builder()
//                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
//                    .defaultHeader("x-goog-api-key", apiKey)
//                    .build();
//
//            // Add delay before starting tests
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//            }
//
//            // Step 2: List Available Models
//            List<String> availableModels = listAvailableModels(webClient);
//
//            // Step 3: Test API Connectivity with available models
//            if (!availableModels.isEmpty()) {
//                testApiConnectivity(webClient, availableModels);
//
//                // Step 4: Test Quiz Generation
//                testQuizGeneration(webClient, availableModels);
//            } else {
//                logger.error("  ❌ No models available to test");
//            }
//
//            printFooter();
//        };
//    }
//
//    private void printBanner() {
//        logger.info("╔════════════════════════════════════════════════════════╗");
//        logger.info("║         GEMINI API CONFIGURATION TEST                  ║");
//        logger.info("╚════════════════════════════════════════════════════════╝");
//    }
//
//    private void printFooter() {
//        logger.info("╔════════════════════════════════════════════════════════╗");
//        logger.info("║         API TEST COMPLETED                             ║");
//        logger.info("╚════════════════════════════════════════════════════════╝");
//    }
//
//    private boolean checkApiKeyConfiguration() {
//        logger.info("→ Step 1: Checking API Key Configuration...");
//
//        if (apiKey == null || apiKey.trim().isEmpty()) {
//            logger.error("  ❌ FAILED: API Key not configured");
//            logger.error("  → Add to application.properties: gemini.api.key=YOUR_API_KEY");
//            logger.error("  → Get key from: https://aistudio.google.com/app/apikey");
//            return false;
//        }
//
//        if (apiKey.length() < 20) {
//            logger.error("  ❌ FAILED: API Key appears to be invalid (too short)");
//            return false;
//        }
//
//        String maskedKey = maskApiKey(apiKey);
//        logger.info("  ✓ API Key found: {}", maskedKey);
//        logger.info("  ✓ API Key length: {} characters", apiKey.length());
//        return true;
//    }
//
//    private List<String> listAvailableModels(WebClient webClient) {
//        logger.info("");
//        logger.info("→ Step 2: Listing Available Models...");
//
//        List<String> modelNames = new ArrayList<>();
//
//        try {
//            String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models";
//
//            String response = webClient.get()
//                    .uri(apiUrl)
//                    .retrieve()
//                    .bodyToMono(String.class)
//                    .timeout(Duration.ofSeconds(10))
//                    .block();
//
//            ObjectMapper objectMapper = new ObjectMapper();
//            JsonNode jsonResponse = objectMapper.readTree(response);
//
//            if (jsonResponse.has("models")) {
//                JsonNode models = jsonResponse.get("models");
//                logger.info("  ✓ Found {} models", models.size());
//                logger.info("");
//                logger.info("  Available models that support generateContent:");
//
//                for (JsonNode model : models) {
//                    JsonNode supportedMethods = model.get("supportedGenerationMethods");
//                    String modelName = model.get("name").asText();
//
//                    // Extract just the model name (e.g., "models/gemini-2.0-flash" -> "gemini-2.0-flash")
//                    String shortName = modelName.replace("models/", "");
//
//                    boolean supportsGenerate = false;
//                    if (supportedMethods != null) {
//                        for (JsonNode method : supportedMethods) {
//                            if ("generateContent".equals(method.asText())) {
//                                supportsGenerate = true;
//                                break;
//                            }
//                        }
//                    }
//
//                    if (supportsGenerate) {
//                        logger.info("    • {}", shortName);
//                        modelNames.add(shortName);
//                    }
//                }
//
//                if (modelNames.isEmpty()) {
//                    logger.warn("  ⚠ No models support generateContent");
//                }
//            } else {
//                logger.warn("  ⚠ Unexpected response format");
//            }
//
//        } catch (WebClientResponseException e) {
//            logger.error("  ❌ Failed to list models!");
//            logger.error("  → Status Code: {}", e.getStatusCode().value());
//            logger.error("  → Error: {}", e.getMessage());
//
//            if (e.getStatusCode().value() == 429) {
//                logger.error("  → Rate Limit: You've exceeded your quota");
//                logger.error("  → Wait a few minutes and try again");
//                logger.error("  → Check quota at: https://aistudio.google.com/app/apikey");
//            }
//        } catch (Exception e) {
//            logger.error("  ❌ Error listing models: {}", e.getMessage());
//        }
//
//        return modelNames;
//    }
//
//    private void testApiConnectivity(WebClient webClient, List<String> availableModels) {
//        logger.info("");
//        logger.info("→ Step 3: Testing API Connectivity...");
//
//        // Add delay to avoid rate limiting
//        try {
//            Thread.sleep(2000);
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//        }
//
//        String testPrompt = "Reply with just the word 'SUCCESS' if you receive this.";
//        Map<String, Object> requestBody = Map.of(
//                "contents", List.of(Map.of("parts", List.of(Map.of("text", testPrompt))))
//        );
//
//        // Use the first available model
//        for (String model : availableModels) {
//            try {
//                logger.info("  → Testing with model: {}", model);
//
//                String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent";
//
//                long startTime = System.currentTimeMillis();
//
//                String response = webClient.post()
//                        .uri(apiUrl)
//                        .bodyValue(requestBody)
//                        .retrieve()
//                        .bodyToMono(String.class)
//                        .timeout(Duration.ofSeconds(10))
//                        .block();
//
//                long duration = System.currentTimeMillis() - startTime;
//
//                ObjectMapper objectMapper = new ObjectMapper();
//                JsonNode jsonResponse = objectMapper.readTree(response);
//
//                if (jsonResponse.has("candidates")) {
//                    String responseText = jsonResponse.get("candidates").get(0)
//                            .get("content").get("parts").get(0).get("text").asText();
//
//                    logger.info("  ✓ API Connection Successful!");
//                    logger.info("  ✓ Working Model: {}", model);
//                    logger.info("  ✓ Response time: {} ms", duration);
//                    logger.info("  ✓ API Response: {}", responseText.trim());
//                    return; // Success, stop testing
//                }
//
//            } catch (WebClientResponseException e) {
//                if (e.getStatusCode().value() == 429) {
//                    logger.error("  ❌ Rate Limit Exceeded on {}", model);
//                    logger.error("  → You've made too many requests");
//                    logger.error("  → Wait 1-2 minutes and restart the application");
//                    logger.error("  → Check quota at: https://aistudio.google.com/app/apikey");
//                    return; // Stop testing due to rate limit
//                } else {
//                    logger.warn("  ⚠ Error with {}: {} {}", model, e.getStatusCode().value(), e.getMessage());
//                }
//            } catch (Exception e) {
//                logger.error("  ❌ Connection error with {}: {}", model, e.getMessage());
//            }
//
//            // Add delay between attempts
//            try {
//                Thread.sleep(2000);
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//            }
//        }
//
//        logger.error("  ❌ All available models failed!");
//    }
//
//    private void testQuizGeneration(WebClient webClient, List<String> availableModels) {
//        logger.info("");
//        logger.info("→ Step 4: Testing Quiz Generation Capability...");
//
//        // Add delay before quiz test
//        try {
//            Thread.sleep(3000);
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//        }
//
//        String quizPrompt = "Generate a quiz with 2 questions about basic math. " +
//                "Return ONLY valid JSON in this format: " +
//                "{\"questions\": [{\"question\": \"...\", \"options\": [\"a\", \"b\", \"c\", \"d\"], \"correctAnswer\": 0, \"explanation\": \"...\"}]}";
//
//        Map<String, Object> requestBody = Map.of(
//                "contents", List.of(Map.of("parts", List.of(Map.of("text", quizPrompt))))
//        );
//
//        for (String model : availableModels) {
//            try {
//                logger.info("  → Testing quiz generation with: {}", model);
//
//                String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent";
//
//                String response = webClient.post()
//                        .uri(apiUrl)
//                        .bodyValue(requestBody)
//                        .retrieve()
//                        .bodyToMono(String.class)
//                        .timeout(Duration.ofSeconds(15))
//                        .block();
//
//                ObjectMapper objectMapper = new ObjectMapper();
//                JsonNode jsonResponse = objectMapper.readTree(response);
//
//                if (jsonResponse.has("candidates")) {
//                    String quizJson = jsonResponse.get("candidates").get(0)
//                            .get("content").get("parts").get(0).get("text").asText();
//
//                    // Clean up response
//                    quizJson = quizJson.trim();
//                    if (quizJson.startsWith("```json")) {
//                        quizJson = quizJson.substring(7);
//                    }
//                    if (quizJson.startsWith("```")) {
//                        quizJson = quizJson.substring(3);
//                    }
//                    if (quizJson.endsWith("```")) {
//                        quizJson = quizJson.substring(0, quizJson.length() - 3);
//                    }
//                    quizJson = quizJson.trim();
//
//                    // Try to parse as JSON
//                    try {
//                        JsonNode quizData = objectMapper.readTree(quizJson);
//                        if (quizData.has("questions") && quizData.get("questions").isArray()) {
//                            int questionCount = quizData.get("questions").size();
//                            logger.info("  ✓ Quiz Generation Successful!");
//                            logger.info("  ✓ Working Model: {}", model);
//                            logger.info("  ✓ Generated {} questions", questionCount);
//                            logger.info("  ✓ JSON format is valid");
//                            logger.info("  ✓ AI Quiz feature is ready to use!");
//                            return; // Success
//                        } else {
//                            logger.warn("  ⚠ Quiz generated but format is incorrect with {}", model);
//                        }
//                    } catch (Exception e) {
//                        logger.warn("  ⚠ Quiz generated but JSON is invalid with {}", model);
//                    }
//                }
//
//            } catch (WebClientResponseException e) {
//                if (e.getStatusCode().value() == 429) {
//                    logger.error("  ❌ Rate Limit Exceeded");
//                    logger.warn("  → Wait 1-2 minutes before trying again");
//                    return; // Stop due to rate limit
//                }
//                logger.warn("  ⚠ Error with {}: {}", model, e.getMessage());
//            } catch (Exception e) {
//                logger.error("  ❌ Quiz test failed with {}: {}", model, e.getMessage());
//            }
//
//            // Add delay between attempts
//            try {
//                Thread.sleep(2000);
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//            }
//        }
//
//        logger.warn("  ⚠ AI quiz generation may not work properly with available models");
//    }
//
//    private String maskApiKey(String key) {
//        if (key == null || key.length() < 15) {
//            return "***INVALID***";
//        }
//        String start = key.substring(0, 10);
//        String end = key.substring(key.length() - 4);
//        int middleLength = key.length() - 14;
//        String middle = "*".repeat(Math.min(middleLength, 20));
//        return start + middle + end;
//    }
//}