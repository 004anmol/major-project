package com.it.ai_mentoring_system.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.it.ai_mentoring_system.model.*;
import com.it.ai_mentoring_system.repository.QuizRepository;
import com.it.ai_mentoring_system.repository.QuizResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class QuizService {

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private QuizResultRepository quizResultRepository;

    @Autowired
    private GeminiApiService geminiApiService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Quiz generateAiQuiz(Student student, String topic, String difficulty, int numberOfQuestions, Integer timeLimit) {
        String quizJson = geminiApiService.generateQuiz(topic, difficulty, numberOfQuestions);

        System.out.println("Generated Quiz: " + quizJson);
        Quiz quiz = new Quiz();
        quiz.setTitle("AI Generated Quiz: " + topic);
        quiz.setDescription("Auto-generated quiz on " + topic + " (" + difficulty + " difficulty)");
        quiz.setQuestions(quizJson);
        quiz.setStudent(student);
        quiz.setIsAiGenerated(true);
        quiz.setTimeLimit(timeLimit);

        return quizRepository.save(quiz);
    }

    public Quiz createManualQuiz(Teacher teacher, Student student, String title, String description, String questionsJson, Integer timeLimit) {
        Quiz quiz = new Quiz();
        quiz.setTitle(title);
        quiz.setDescription(description);
        quiz.setQuestions(questionsJson);
        quiz.setTeacher(teacher);
        quiz.setStudent(student);
        quiz.setIsAiGenerated(false);
        quiz.setTimeLimit(timeLimit);

        return quizRepository.save(quiz);
    }

    @Transactional
    public QuizResult submitQuiz(Quiz quiz, Student student, String answersJson) {
        try {
            JsonNode questionsNode = objectMapper.readTree(quiz.getQuestions());
            JsonNode answersNode = objectMapper.readTree(answersJson);

            int score = 0;
            int totalQuestions = 0;

            if (questionsNode.has("questions") && questionsNode.get("questions").isArray()) {
                for (JsonNode question : questionsNode.get("questions")) {
                    totalQuestions++;
                    int correctAnswer = question.get("correctAnswer").asInt();

                    String questionKey = "q" + (totalQuestions - 1);
                    int studentAnswer = answersNode.has(questionKey)
                            ? answersNode.get(questionKey).asInt()
                            : -1;

                    if (studentAnswer == correctAnswer) {
                        score++;
                    }
                }
            }

            String analysisJson = geminiApiService.analyzeQuizResults(quiz.getQuestions(), answersJson);
            JsonNode analysisNode = objectMapper.readTree(analysisJson);

            QuizResult result = new QuizResult();
            result.setQuiz(quiz);
            result.setStudent(student);
            result.setScore(score);
            result.setTotalQuestions(totalQuestions);
            result.setAnswers(answersJson);

            // FIXED: Convert JSON arrays to comma-separated strings for easier parsing
            if (analysisNode.has("strengths") && analysisNode.get("strengths").isArray()) {
                List<String> strengthsList = new ArrayList<>();
                for (JsonNode strength : analysisNode.get("strengths")) {
                    strengthsList.add(strength.asText());
                }
                // Store as comma-separated values
                result.setStrengths(String.join("|||", strengthsList));
            }

            if (analysisNode.has("weaknesses") && analysisNode.get("weaknesses").isArray()) {
                List<String> weaknessesList = new ArrayList<>();
                for (JsonNode weakness : analysisNode.get("weaknesses")) {
                    weaknessesList.add(weakness.asText());
                }
                // Store as comma-separated values
                result.setWeaknesses(String.join("|||", weaknessesList));
            }

            if (analysisNode.has("analysis")) {
                result.setDetailedAnalysis(analysisNode.get("analysis").asText());
            }

            return quizResultRepository.save(result);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error processing quiz submission", e);
        }
    }

    public List<Quiz> getQuizzesForStudent(Student student) {
        return quizRepository.findByStudent(student);
    }

    public List<Quiz> getQuizzesForTeacher(Teacher teacher) {
        return quizRepository.findByTeacher(teacher);
    }

    public List<QuizResult> getQuizResultsForStudent(Student student) {
        return quizResultRepository.findByStudentOrderByCompletedAtDesc(student);
    }

    public Quiz getQuizById(Long id) {
        return quizRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));
    }
}