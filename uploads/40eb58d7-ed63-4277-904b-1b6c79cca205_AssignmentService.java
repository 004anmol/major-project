package com.it.ai_mentoring_system.service;

import com.it.ai_mentoring_system.model.*;
import com.it.ai_mentoring_system.repository.AssignmentRepository;
import com.it.ai_mentoring_system.repository.AssignmentSubmissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AssignmentService {

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private AssignmentSubmissionRepository submissionRepository;

    public Assignment createAssignment(Teacher teacher, Student student, String title, 
                                      String description, LocalDateTime deadline) {
        Assignment assignment = new Assignment();
        assignment.setTeacher(teacher);
        assignment.setStudent(student);
        assignment.setTitle(title);
        assignment.setDescription(description);
        assignment.setDeadline(deadline);
        
        return assignmentRepository.save(assignment);
    }

    public AssignmentSubmission submitAssignment(Assignment assignment, Student student, 
                                                 String filePath, String fileName) {
        Optional<AssignmentSubmission> existing = submissionRepository
                .findByAssignmentAndStudent(assignment, student);
        
        AssignmentSubmission submission;
        if (existing.isPresent()) {
            submission = existing.get();
        } else {
            submission = new AssignmentSubmission();
            submission.setAssignment(assignment);
            submission.setStudent(student);
        }
        
        submission.setFilePath(filePath);
        submission.setFileName(fileName);
        submission.setStatus(AssignmentSubmission.SubmissionStatus.SUBMITTED);
        
        return submissionRepository.save(submission);
    }

    public void reviewAssignment(Long submissionId, String remarks, 
                                AssignmentSubmission.SubmissionStatus status) {
        AssignmentSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found"));
        
        submission.setRemarks(remarks);
        submission.setStatus(status);
        
        submissionRepository.save(submission);
    }

    public List<Assignment> getAssignmentsForStudent(Student student) {
        return assignmentRepository.findByStudent(student);
    }

    public List<Assignment> getAssignmentsForTeacher(Teacher teacher) {
        return assignmentRepository.findByTeacher(teacher);
    }

    public Assignment getAssignmentById(Long id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));
    }
}

