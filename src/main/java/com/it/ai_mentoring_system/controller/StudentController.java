package com.it.ai_mentoring_system.controller;

import com.it.ai_mentoring_system.model.*;
import com.it.ai_mentoring_system.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    private static final Logger logger = LoggerFactory.getLogger(GeminiApiService.class);

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
        List<Quiz> quizzes = quizService.getQuizzesForStudent(student);
        List<QuizResult> quizResults = quizService.getQuizResultsForStudent(student);

        // Create a set of quiz IDs that have been attempted
        Set<Long> attemptedQuizIds = quizResults.stream()
                .map(result -> result.getQuiz().getId())
                .collect(Collectors.toSet());

        model.addAttribute("quizzes", quizzes);
        model.addAttribute("attemptedQuizIds", attemptedQuizIds);
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
                               @RequestParam(required = false) Integer timeLimit,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            // Validate timeLimit
            if (timeLimit == null || timeLimit < 5 || timeLimit > 60) {
                timeLimit = 20; // Default to 20 minutes if invalid
            }

            // Validate numberOfQuestions
            if (numberOfQuestions < 5) numberOfQuestions = 5;
            if (numberOfQuestions > 20) numberOfQuestions = 20;

            Student student = userService.getStudentByUsername(authentication.getName());
            Quiz quiz = quizService.generateAiQuiz(student, topic, difficulty, numberOfQuestions, timeLimit);

            if (quiz != null) {
                redirectAttributes.addFlashAttribute("success", "Quiz generated successfully!");
                return "redirect:/student/quizzes";
            } else {
                throw new RuntimeException("Quiz generation returned null");
            }
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Rate limit")) {
                redirectAttributes.addFlashAttribute("error",
                        "Rate limit exceeded. Please wait 1-2 minutes and try again. " +
                                "Tip: Generate fewer questions or use a simpler topic.");
            } else {
                redirectAttributes.addFlashAttribute("error",
                        "Failed to generate quiz. The AI service may be temporarily unavailable. " +
                                "A fallback quiz has been created. Error: " + e.getMessage());
            }
            return "redirect:/student/quiz/generate";
        } catch (Exception e) {
            logger.error("Unexpected error generating quiz", e);
            redirectAttributes.addFlashAttribute("error",
                    "An unexpected error occurred. Please try again with a different topic or fewer questions.");
            return "redirect:/student/quiz/generate";
        }
    }

    @GetMapping("/quiz/{id}/take")
    public String takeQuiz(@PathVariable Long id, Model model, Authentication authentication) {
        try {
            Quiz quiz = quizService.getQuizById(id);
            model.addAttribute("quiz", quiz);
            return "student/take-quiz";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Failed to load quiz: " + e.getMessage());
            return "redirect:/student/quizzes";
        }
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
            e.printStackTrace();
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

        List<Course> allCourses = courseService.getAllCourses();
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