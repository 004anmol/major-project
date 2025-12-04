package com.it.ai_mentoring_system.repository;

import com.it.ai_mentoring_system.model.Assignment;
import com.it.ai_mentoring_system.model.Student;
import com.it.ai_mentoring_system.model.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findByTeacher(Teacher teacher);
    List<Assignment> findByStudent(Student student);
}




