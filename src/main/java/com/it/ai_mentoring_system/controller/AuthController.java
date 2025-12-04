package com.it.ai_mentoring_system.controller;

import com.it.ai_mentoring_system.model.Role;
import com.it.ai_mentoring_system.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    @GetMapping("/")
    public String home() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error, 
                       @RequestParam(required = false) String logout, 
                       Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid username or password");
        }
        if (logout != null) {
            model.addAttribute("message", "You have been logged out successfully");
        }
        return "login";
    }

    @GetMapping("/register")
    public String registerForm() {
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String username,
                          @RequestParam String password,
                          @RequestParam String email,
                          @RequestParam String fullName,
                          @RequestParam String role,
                          RedirectAttributes redirectAttributes) {
        try {
            Role.RoleType roleType = Role.RoleType.valueOf(role.toUpperCase());
            userService.registerUser(username, password, email, fullName, roleType);
            redirectAttributes.addFlashAttribute("success", "Registration successful! Please login.");
            return "redirect:/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/register";
        }
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String role = auth.getAuthorities().iterator().next().getAuthority();
        
        if (role.equals("ROLE_STUDENT")) {
            return "redirect:/student/dashboard";
        } else if (role.equals("ROLE_TEACHER")) {
            return "redirect:/teacher/dashboard";
        }
        return "redirect:/login";
    }
}




