package com.it.ai_mentoring_system.repository;

import com.it.ai_mentoring_system.model.Assignment;
import com.it.ai_mentoring_system.model.AssignmentSubmission;
import com.it.ai_mentoring_system.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, Long> {
    List<AssignmentSubmission> findByStudent(Student student);
    Optional<AssignmentSubmission> findByAssignmentAndStudent(Assignment assignment, Student student);
}




