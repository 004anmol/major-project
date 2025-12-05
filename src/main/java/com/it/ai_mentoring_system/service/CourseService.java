package com.it.ai_mentoring_system.service;

import com.it.ai_mentoring_system.model.*;
import com.it.ai_mentoring_system.repository.CourseRepository;
import com.it.ai_mentoring_system.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CourseService {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private StudentRepository studentRepository;

    public Course createCourse(Teacher teacher, String title, String description, String youtubePlaylistUrl) {
        Course course = new Course();
        course.setTeacher(teacher);
        course.setTitle(title);
        course.setDescription(description);
        course.setYoutubePlaylistUrl(youtubePlaylistUrl);

        return courseRepository.save(course);
    }

    @Transactional
    public void enrollStudentInCourse(Student student, Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        if (!student.getEnrolledCourses().contains(course)) {
            student.getEnrolledCourses().add(course);
            studentRepository.save(student);
        } else {
            throw new RuntimeException("Already enrolled in this course");
        }
    }

    @Transactional
    public void unenrollStudentFromCourse(Student student, Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        if (student.getEnrolledCourses().contains(course)) {
            student.getEnrolledCourses().remove(course);
            studentRepository.save(student);
        } else {
            throw new RuntimeException("Not enrolled in this course");
        }
    }

    public List<Course> getCoursesForTeacher(Teacher teacher) {
        return courseRepository.findByTeacher(teacher);
    }

    public List<Course> getEnrolledCoursesForStudent(Student student) {
        return student.getEnrolledCourses();
    }

    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    public Course getCourseById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));
    }
}