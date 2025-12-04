package com.it.ai_mentoring_system.service;

import com.it.ai_mentoring_system.model.*;
import com.it.ai_mentoring_system.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    public Notification createNotification(Teacher teacher, Student student, String title, String message) {
        Notification notification = new Notification();
        notification.setTeacher(teacher);
        notification.setStudent(student);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setIsRead(false);
        
        return notificationRepository.save(notification);
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    public List<Notification> getNotificationsForStudent(Student student) {
        return notificationRepository.findByStudentOrderByCreatedAtDesc(student);
    }

    public List<Notification> getUnreadNotificationsForStudent(Student student) {
        return notificationRepository.findByStudentAndIsRead(student, false);
    }

    public Notification getNotificationById(Long id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
    }
}




