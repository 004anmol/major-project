package com.it.ai_mentoring_system.repository;

import com.it.ai_mentoring_system.model.Quiz;
import com.it.ai_mentoring_system.model.Student;
import com.it.ai_mentoring_system.model.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {
    List<Quiz> findByTeacher(Teacher teacher);
    List<Quiz> findByStudent(Student student);
    List<Quiz> findByIsAiGenerated(Boolean isAiGenerated);
}




