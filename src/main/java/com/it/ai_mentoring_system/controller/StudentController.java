package com.it.ai_mentoring_system.controller;

import com.it.ai_mentoring_system.model.*;
import com.it.ai_mentoring_system.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/student")
public class StudentController {

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

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        Student student = userService.getStudentByUsername(authentication.getName());
        model.addAttribute("student", student);
        model.addAttribute("notifications", notificationService.getUnreadNotificationsForStudent(student));
        return "student/dashboard";
    }

    @GetMapping("/quizzes")
    public String quizzes(Model model, Authentication authentication) {
        Student student = userService.getStudentByUsername(authentication.getName());
        model.addAttribute("quizzes", quizService.getQuizzesForStudent(student));
        return "student/quizzes";
    }

    @GetMapping("/quiz/generate")
    public String generateQuizForm() {
        return "student/generate-quiz";
    }

    @PostMapping("/quiz/generate")
    public String generateQuiz(@RequestParam String topic,
                               @RequestParam String difficulty,
                               @RequestParam(defaultValue = "10") int numberOfQuestions,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            Student student = userService.getStudentByUsername(authentication.getName());
            quizService.generateAiQuiz(student, topic, difficulty, numberOfQuestions);
            redirectAttributes.addFlashAttribute("success", "Quiz generated successfully!");
            return "redirect:/student/quizzes";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to generate quiz: " + e.getMessage());
            return "redirect:/student/quiz/generate";
        }
    }

    @GetMapping("/quiz/{id}/take")
    public String takeQuiz(@PathVariable Long id, Model model, Authentication authentication) {
        Quiz quiz = quizService.getQuizById(id);
        model.addAttribute("quiz", quiz);
        return "student/take-quiz";
    }

    @PostMapping("/quiz/{id}/submit")
    public String submitQuiz(@PathVariable Long id,
                             @RequestParam String answers,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        try {
            Student student = userService.getStudentByUsername(authentication.getName());
            Quiz quiz = quizService.getQuizById(id);
            quizService.submitQuiz(quiz, student, answers);
            redirectAttributes.addFlashAttribute("success", "Quiz submitted successfully!");
            return "redirect:/student/quiz-results";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to submit quiz: " + e.getMessage());
            return "redirect:/student/quiz/" + id + "/take";
        }
    }

    @GetMapping("/quiz-results")
    public String quizResults(Model model, Authentication authentication) {
        Student student = userService.getStudentByUsername(authentication.getName());
        model.addAttribute("results", quizService.getQuizResultsForStudent(student));
        return "student/quiz-results";
    }

    @GetMapping("/quiz-result/{id}")
    public String quizResultDetail(@PathVariable Long id, Model model, Authentication authentication) {
        Student student = userService.getStudentByUsername(authentication.getName());
        QuizResult result = quizService.getQuizResultsForStudent(student).stream()
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Result not found"));
        model.addAttribute("result", result);
        return "student/quiz-result-detail";
    }

    @GetMapping("/assignments")
    public String assignments(Model model, Authentication authentication) {
        Student student = userService.getStudentByUsername(authentication.getName());
        model.addAttribute("assignments", assignmentService.getAssignmentsForStudent(student));
        return "student/assignments";
    }

    @GetMapping("/assignment/{id}/submit")
    public String submitAssignmentForm(@PathVariable Long id, Model model) {
        Assignment assignment = assignmentService.getAssignmentById(id);
        model.addAttribute("assignment", assignment);
        return "student/submit-assignment";
    }

    @PostMapping("/assignment/{id}/submit")
    public String submitAssignment(@PathVariable Long id,
                                   @RequestParam("file") MultipartFile file,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        try {
            Student student = userService.getStudentByUsername(authentication.getName());
            Assignment assignment = assignmentService.getAssignmentById(id);
            String filePath = documentService.uploadDocument(student, assignment.getTeacher(), file, "").getFilePath();
            assignmentService.submitAssignment(assignment, student, filePath, file.getOriginalFilename());
            redirectAttributes.addFlashAttribute("success", "Assignment submitted successfully!");
            return "redirect:/student/assignments";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to submit assignment: " + e.getMessage());
            return "redirect:/student/assignment/" + id + "/submit";
        }
    }

    @GetMapping("/documents")
    public String documents(Model model, Authentication authentication) {
        Student student = userService.getStudentByUsername(authentication.getName());
        model.addAttribute("documents", documentService.getDocumentsForStudent(student));
        return "student/documents";
    }

    @GetMapping("/document/upload")
    public String uploadDocumentForm(Model model, Authentication authentication) {
        Student student = userService.getStudentByUsername(authentication.getName());
        model.addAttribute("teacher", student.getMentor());
        return "student/upload-document";
    }

    @PostMapping("/document/upload")
    public String uploadDocument(@RequestParam("file") MultipartFile file,
                                 @RequestParam(required = false) String requestMessage,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        try {
            Student student = userService.getStudentByUsername(authentication.getName());
            if (student.getMentor() == null) {
                redirectAttributes.addFlashAttribute("error", "No mentor assigned");
                return "redirect:/student/documents";
            }
            documentService.uploadDocument(student, student.getMentor(), file, requestMessage);
            redirectAttributes.addFlashAttribute("success", "Document uploaded successfully!");
            return "redirect:/student/documents";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to upload document: " + e.getMessage());
            return "redirect:/student/document/upload";
        }
    }

    @GetMapping("/courses")
    public String courses(Model model, Authentication authentication) {
        Student student = userService.getStudentByUsername(authentication.getName());

        // Get all courses in the system
        List<Course> allCourses = courseService.getAllCourses();

        // Get enrolled courses for the student
        List<Course> enrolledCourses = courseService.getEnrolledCoursesForStudent(student);

        model.addAttribute("allCourses", allCourses);
        model.addAttribute("enrolledCourses", enrolledCourses);
        model.addAttribute("student", student);

        return "student/courses";
    }

    @PostMapping("/course/{id}/enroll")
    public String enrollInCourse(@PathVariable Long id,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        try {
            Student student = userService.getStudentByUsername(authentication.getName());
            courseService.enrollStudentInCourse(student, id);
            redirectAttributes.addFlashAttribute("success", "Enrolled in course successfully!");
            return "redirect:/student/courses";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to enroll: " + e.getMessage());
            return "redirect:/student/courses";
        }
    }

    @PostMapping("/course/{id}/unenroll")
    public String unenrollFromCourse(@PathVariable Long id,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        try {
            Student student = userService.getStudentByUsername(authentication.getName());
            courseService.unenrollStudentFromCourse(student, id);
            redirectAttributes.addFlashAttribute("success", "Unenrolled from course successfully!");
            return "redirect:/student/courses";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to unenroll: " + e.getMessage());
            return "redirect:/student/courses";
        }
    }

    @GetMapping("/notifications")
    public String notifications(Model model, Authentication authentication) {
        Student student = userService.getStudentByUsername(authentication.getName());
        model.addAttribute("notifications", notificationService.getNotificationsForStudent(student));
        return "student/notifications";
    }

    @PostMapping("/notification/{id}/read")
    public String markNotificationAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return "redirect:/student/notifications";
    }
}