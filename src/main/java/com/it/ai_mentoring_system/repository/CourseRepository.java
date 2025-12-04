package com.it.ai_mentoring_system.repository;

import com.it.ai_mentoring_system.model.Course;
import com.it.ai_mentoring_system.model.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByTeacher(Teacher teacher);
}




