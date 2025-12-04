package com.it.ai_mentoring_system.repository;

import com.it.ai_mentoring_system.model.QuizResult;
import com.it.ai_mentoring_system.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizResultRepository extends JpaRepository<QuizResult, Long> {
    List<QuizResult> findByStudent(Student student);
    List<QuizResult> findByStudentOrderByCompletedAtDesc(Student student);
}




