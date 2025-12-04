package com.it.ai_mentoring_system.repository;

import com.it.ai_mentoring_system.model.Student;
import com.it.ai_mentoring_system.model.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByUserUsername(String username);
    List<Student> findByMentor(Teacher mentor);
}




