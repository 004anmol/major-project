package com.it.ai_mentoring_system.repository;

import com.it.ai_mentoring_system.model.Document;
import com.it.ai_mentoring_system.model.Student;
import com.it.ai_mentoring_system.model.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByStudent(Student student);
    List<Document> findByTeacher(Teacher teacher);
    List<Document> findByStudentAndStatus(Student student, Document.DocumentStatus status);
}




