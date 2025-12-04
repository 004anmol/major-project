package com.it.ai_mentoring_system.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.it.ai_mentoring_system.model.*;
import com.it.ai_mentoring_system.repository.QuizRepository;
import com.it.ai_mentoring_system.repository.QuizResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class QuizService {

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private QuizResultRepository quizResultRepository;

    @Autowired
    private GrokApiService grokApiService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Quiz generateAiQuiz(Student student, String topic, String difficulty, int numberOfQuestions) {
        String quizJson = grokApiService.generateQuiz(topic, difficulty, numberOfQuestions);
        
        Quiz quiz = new Quiz();
        quiz.setTitle("AI Generated Quiz: " + topic);
        quiz.setDescription("Auto-generated quiz on " + topic);
        quiz.setQuestions(quizJson);
        quiz.setStudent(student);
        quiz.setIsAiGenerated(true);
        
        return quizRepository.save(quiz);
    }

    public Quiz createManualQuiz(Teacher teacher, Student student, String title, String description, String questionsJson) {
        Quiz quiz = new Quiz();
        quiz.setTitle(title);
        quiz.setDescription(description);
        quiz.setQuestions(questionsJson);
        quiz.setTeacher(teacher);
        quiz.setStudent(student);
        quiz.setIsAiGenerated(false);
        
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
                    int studentAnswer = answersNode.has(String.valueOf(totalQuestions - 1)) 
                        ? answersNode.get(String.valueOf(totalQuestions - 1)).asInt() 
                        : -1;
                    
                    if (studentAnswer == correctAnswer) {
                        score++;
                    }
                }
            }
            
            // Analyze results using Grok API
            String analysisJson = grokApiService.analyzeQuizResults(quiz.getQuestions(), answersJson);
            JsonNode analysisNode = objectMapper.readTree(analysisJson);
            
            QuizResult result = new QuizResult();
            result.setQuiz(quiz);
            result.setStudent(student);
            result.setScore(score);
            result.setTotalQuestions(totalQuestions);
            result.setAnswers(answersJson);
            
            if (analysisNode.has("strengths")) {
                result.setStrengths(analysisNode.get("strengths").toString());
            }
            if (analysisNode.has("weaknesses")) {
                result.setWeaknesses(analysisNode.get("weaknesses").toString());
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

