package com.it.ai_mentoring_system.controller;

import com.it.ai_mentoring_system.model.*;
import com.it.ai_mentoring_system.repository.StudentRepository;
import com.it.ai_mentoring_system.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/teacher")
public class TeacherController {

    @Autowired
    private UserService userService;

    @Autowired
    private QuizService quizService;

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private StudentRepository studentRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        Teacher teacher = userService.getTeacherByUsername(authentication.getName());
        model.addAttribute("teacher", teacher);
        model.addAttribute("students", teacher.getStudents());
        return "teacher/dashboard";
    }

    @GetMapping("/students")
    public String students(Model model, Authentication authentication) {
        Teacher teacher = userService.getTeacherByUsername(authentication.getName());
        model.addAttribute("students", teacher.getStudents());
        return "teacher/students";
    }

    @GetMapping("/students/add")
    public String addStudentsForm(Model model, Authentication authentication) {
        Teacher teacher = userService.getTeacherByUsername(authentication.getName());
        model.addAttribute("teacher", teacher);
        return "teacher/add-students";
    }

    @PostMapping("/students/add")
    public String addStudents(@RequestParam String emails,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        try {
            Teacher teacher = userService.getTeacherByUsername(authentication.getName());
            Map<String, String> results = userService.assignStudentsToTeacher(teacher, emails);

            long successCount = results.values().stream().filter(v -> v.equals("Success")).count();
            long failCount = results.size() - successCount;

            if (successCount > 0) {
                redirectAttributes.addFlashAttribute("success",
                        successCount + " student(s) assigned successfully!");
            }
            if (failCount > 0) {
                redirectAttributes.addFlashAttribute("warning",
                        failCount + " email(s) could not be assigned. Check details below.");
            }

            redirectAttributes.addFlashAttribute("assignmentResults", results);
            return "redirect:/teacher/students/add";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to assign students: " + e.getMessage());
            return "redirect:/teacher/students/add";
        }
    }

    @GetMapping("/document/request")
    public String requestDocumentForm(Model model, Authentication authentication) {
        Teacher teacher = userService.getTeacherByUsername(authentication.getName());
        model.addAttribute("students", teacher.getStudents());
        return "teacher/request-document";
    }

    @PostMapping("/document/request")
    public String requestDocument(@RequestParam Long studentId,
                                  @RequestParam String requestMessage,
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
        try {
            Teacher teacher = userService.getTeacherByUsername(authentication.getName());
            Student student = studentRepository.findById(studentId)
                    .orElseThrow(() -> new RuntimeException("Student not found"));
            documentService.requestDocument(teacher, student, requestMessage);
            redirectAttributes.addFlashAttribute("success", "Document request sent successfully!");
            return "redirect:/teacher/documents";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to request document: " + e.getMessage());
            return "redirect:/teacher/document/request";
        }
    }

    @GetMapping("/documents")
    public String documents(Model model, Authentication authentication) {
        Teacher teacher = userService.getTeacherByUsername(authentication.getName());
        model.addAttribute("documents", documentService.getDocumentsForTeacher(teacher));
        return "teacher/documents";
    }

    @GetMapping("/document/{id}/review")
    public String reviewDocumentForm(@PathVariable Long id, Model model) {
        Document document = documentService.getDocumentById(id);
        model.addAttribute("document", document);
        return "teacher/review-document";
    }

    @PostMapping("/document/{id}/review")
    public String reviewDocument(@PathVariable Long id,
                                 @RequestParam String remarks,
                                 @RequestParam String status,
                                 RedirectAttributes redirectAttributes) {
        try {
            Document.DocumentStatus docStatus = Document.DocumentStatus.valueOf(status);
            documentService.reviewDocument(id, remarks, docStatus);
            redirectAttributes.addFlashAttribute("success", "Document reviewed successfully!");
            return "redirect:/teacher/documents";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to review document: " + e.getMessage());
            return "redirect:/teacher/document/" + id + "/review";
        }
    }

    @GetMapping("/notification/create")
    public String createNotificationForm(Model model, Authentication authentication) {
        Teacher teacher = userService.getTeacherByUsername(authentication.getName());
        model.addAttribute("students", teacher.getStudents());
        return "teacher/create-notification";
    }

    @PostMapping("/notification/create")
    public String createNotification(@RequestParam Long studentId,
                                     @RequestParam String title,
                                     @RequestParam String message,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        try {
            Teacher teacher = userService.getTeacherByUsername(authentication.getName());
            Student student = studentRepository.findById(studentId)
                    .orElseThrow(() -> new RuntimeException("Student not found"));
            notificationService.createNotification(teacher, student, title, message);
            redirectAttributes.addFlashAttribute("success", "Notification sent successfully!");
            return "redirect:/teacher/dashboard";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to send notification: " + e.getMessage());
            return "redirect:/teacher/notification/create";
        }
    }

    @GetMapping("/assignment/create")
    public String createAssignmentForm(Model model, Authentication authentication) {
        Teacher teacher = userService.getTeacherByUsername(authentication.getName());
        model.addAttribute("students", teacher.getStudents());
        return "teacher/create-assignment";
    }

    @PostMapping("/assignment/create")
    public String createAssignment(@RequestParam Long studentId,
                                   @RequestParam String title,
                                   @RequestParam String description,
                                   @RequestParam String deadline,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        try {
            Teacher teacher = userService.getTeacherByUsername(authentication.getName());
            Student student = studentRepository.findById(studentId)
                    .orElseThrow(() -> new RuntimeException("Student not found"));
            LocalDateTime deadlineDate = LocalDateTime.parse(deadline);
            assignmentService.createAssignment(teacher, student, title, description, deadlineDate);
            redirectAttributes.addFlashAttribute("success", "Assignment created successfully!");
            return "redirect:/teacher/assignments";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to create assignment: " + e.getMessage());
            return "redirect:/teacher/assignment/create";
        }
    }

    @GetMapping("/assignments")
    public String assignments(Model model, Authentication authentication) {
        Teacher teacher = userService.getTeacherByUsername(authentication.getName());
        model.addAttribute("assignments", assignmentService.getAssignmentsForTeacher(teacher));
        return "teacher/assignments";
    }

    @GetMapping("/quiz/create")
    public String createQuizForm(Model model, Authentication authentication) {
        Teacher teacher = userService.getTeacherByUsername(authentication.getName());
        model.addAttribute("students", teacher.getStudents());
        return "teacher/create-quiz";
    }

    @PostMapping("/quiz/create")
    public String createQuiz(@RequestParam Long studentId,
                             @RequestParam String title,
                             @RequestParam String description,
                             @RequestParam String questions,
                             @RequestParam(required = false, defaultValue = "20") Integer timeLimit,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        try {
            Teacher teacher = userService.getTeacherByUsername(authentication.getName());
            Student student = studentRepository.findById(studentId)
                    .orElseThrow(() -> new RuntimeException("Student not found"));
            quizService.createManualQuiz(teacher, student, title, description, questions, timeLimit);
            redirectAttributes.addFlashAttribute("success", "Quiz created successfully!");
            return "redirect:/teacher/quizzes";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to create quiz: " + e.getMessage());
            return "redirect:/teacher/quiz/create";
        }
    }

    @GetMapping("/quizzes")
    public String quizzes(Model model, Authentication authentication) {
        Teacher teacher = userService.getTeacherByUsername(authentication.getName());
        model.addAttribute("quizzes", quizService.getQuizzesForTeacher(teacher));
        return "teacher/quizzes";
    }

    @GetMapping("/course/create")
    public String createCourseForm() {
        return "teacher/create-course";
    }

    @PostMapping("/course/create")
    public String createCourse(@RequestParam String title,
                               @RequestParam String description,
                               @RequestParam String youtubePlaylistUrl,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            Teacher teacher = userService.getTeacherByUsername(authentication.getName());
            courseService.createCourse(teacher, title, description, youtubePlaylistUrl);
            redirectAttributes.addFlashAttribute("success", "Course created successfully!");
            return "redirect:/teacher/courses";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to create course: " + e.getMessage());
            return "redirect:/teacher/course/create";
        }
    }

    @GetMapping("/courses")
    public String courses(Model model, Authentication authentication) {
        Teacher teacher = userService.getTeacherByUsername(authentication.getName());
        model.addAttribute("courses", courseService.getCoursesForTeacher(teacher));
        return "teacher/courses";
    }
}