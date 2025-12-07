package com.it.ai_mentoring_system.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "quiz_results")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false)
    private Integer score;

    @Column(nullable = false)
    private Integer totalQuestions;

    @Column(columnDefinition = "TEXT")
    private String answers; // JSON string of student answers

    @Column(columnDefinition = "TEXT")
    private String strengths; // Delimited string of strengths

    @Column(columnDefinition = "TEXT")
    private String weaknesses; // Delimited string of weaknesses

    @Column(columnDefinition = "TEXT")
    private String detailedAnalysis; // Detailed analysis text

    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        completedAt = LocalDateTime.now();
    }

    // Helper methods to convert delimited strings to lists for Thymeleaf
    @Transient
    public List<String> getStrengthsList() {
        if (strengths == null || strengths.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(strengths.split("\\|\\|\\|"));
    }

    @Transient
    public List<String> getWeaknessesList() {
        if (weaknesses == null || weaknesses.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(weaknesses.split("\\|\\|\\|"));
    }
}