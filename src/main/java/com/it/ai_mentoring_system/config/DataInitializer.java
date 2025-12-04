package com.it.ai_mentoring_system.config;

import com.it.ai_mentoring_system.model.Role;
import com.it.ai_mentoring_system.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;

    @Override
    public void run(String... args) throws Exception {
        if (roleRepository.findByName(Role.RoleType.STUDENT).isEmpty()) {
            Role studentRole = new Role();
            studentRole.setName(Role.RoleType.STUDENT);
            roleRepository.save(studentRole);
        }

        if (roleRepository.findByName(Role.RoleType.TEACHER).isEmpty()) {
            Role teacherRole = new Role();
            teacherRole.setName(Role.RoleType.TEACHER);
            roleRepository.save(teacherRole);
        }
    }
}




