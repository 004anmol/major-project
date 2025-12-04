package com.it.ai_mentoring_system.service;

import com.it.ai_mentoring_system.model.*;
import com.it.ai_mentoring_system.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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




