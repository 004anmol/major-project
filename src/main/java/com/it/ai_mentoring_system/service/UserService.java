package com.it.ai_mentoring_system.service;

import com.it.ai_mentoring_system.model.*;
import com.it.ai_mentoring_system.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private NotificationService notificationService;

    @Transactional
    public User registerUser(String username, String password, String email, String fullName, Role.RoleType roleType) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists");
        }

        Role role = roleRepository.findByName(roleType)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setFullName(fullName);
        user.setRole(role);

        user = userRepository.save(user);

        // Create corresponding Student or Teacher record
        if (roleType == Role.RoleType.STUDENT) {
            Student student = new Student();
            student.setUser(user);
            studentRepository.save(student);
        } else if (roleType == Role.RoleType.TEACHER) {
            Teacher teacher = new Teacher();
            teacher.setUser(user);
            teacherRepository.save(teacher);
        }

        return user;
    }

    @Transactional
    public Map<String, String> assignStudentsToTeacher(Teacher teacher, String emailsString) {
        Map<String, String> results = new HashMap<>();

        // Split emails by comma and trim whitespace
        String[] emails = emailsString.split(",");

        for (String email : emails) {
            email = email.trim();

            if (email.isEmpty()) {
                continue;
            }

            try {
                // Find user by email
                Optional<User> userOpt = userRepository.findByEmail(email);

                if (!userOpt.isPresent()) {
                    results.put(email, "Email not found in system");
                    continue;
                }

                User user = userOpt.get();

                // Check if user is a student
                if (user.getRole().getName() != Role.RoleType.STUDENT) {
                    results.put(email, "User is not a student");
                    continue;
                }

                // Find student record
                Optional<Student> studentOpt = studentRepository.findByUserUsername(user.getUsername());

                if (!studentOpt.isPresent()) {
                    results.put(email, "Student record not found");
                    continue;
                }

                Student student = studentOpt.get();

                // Check if student already has this teacher
                if (student.getMentor() != null && student.getMentor().getId().equals(teacher.getId())) {
                    results.put(email, "Already assigned to you");
                    continue;
                }

                // Assign teacher to student (reassign if already has a mentor)
                student.setMentor(teacher);
                studentRepository.save(student);

                // Send notification to student
                try {
                    notificationService.createNotification(
                            teacher,
                            student,
                            "Mentor Assigned",
                            "You have been assigned " + teacher.getUser().getFullName() + " as your mentor."
                    );
                } catch (Exception e) {
                    // Log notification error but don't fail the assignment
                    System.err.println("Failed to send notification: " + e.getMessage());
                }

                results.put(email, "Success");

            } catch (Exception e) {
                results.put(email, "Error: " + e.getMessage());
            }
        }

        return results;
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Student getStudentByUsername(String username) {
        return studentRepository.findByUserUsername(username)
                .orElseThrow(() -> new RuntimeException("Student not found"));
    }

    public Teacher getTeacherByUsername(String username) {
        return teacherRepository.findByUserUsername(username)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));
    }
}