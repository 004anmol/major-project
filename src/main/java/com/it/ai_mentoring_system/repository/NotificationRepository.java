package com.it.ai_mentoring_system.repository;

import com.it.ai_mentoring_system.model.Notification;
import com.it.ai_mentoring_system.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByStudent(Student student);
    List<Notification> findByStudentOrderByCreatedAtDesc(Student student);
    List<Notification> findByStudentAndIsRead(Student student, Boolean isRead);
}




