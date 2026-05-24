package com.moneytracker.web;

import com.moneymanager.model.User;
import com.moneymanager.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class WebAuthController {

    public static final String SESSION_USER_ID = "userId";
    public static final String SESSION_USERNAME = "username";

    private final AuthService authService;

    public WebAuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/login")
    public String loginForm(@RequestParam(defaultValue = "login") String mode, Model model) {
        setMode(model, mode);
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpSession session,
                        Model model) {
        return authService.login(username, password)
                .map(user -> startSession(user, session))
                .orElseGet(() -> {
                    model.addAttribute("error", "Invalid username or password.");
                    setMode(model, "login");
                    return "login";
                });
    }

    @PostMapping("/register")
    public String register(@RequestParam String username,
                           @RequestParam String password,
                           @RequestParam String confirmPassword,
                           @RequestParam String recoveryCode,
                           HttpSession session,
                           Model model) {
        try {
            User user = authService.register(username, password, confirmPassword, recoveryCode);
            return startSession(user, session);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("submittedUsername", username);
            setMode(model, "register");
            return "login";
        }
    }

    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestParam String username,
                                 @RequestParam String recoveryCode,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 Model model) {
        try {
            authService.resetPassword(username, recoveryCode, newPassword, confirmPassword);
            model.addAttribute("message", "Password reset complete. Sign in with your new password.");
            setMode(model, "login");
            return "login";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("submittedUsername", username);
            setMode(model, "forgot");
            return "login";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    private String startSession(User user, HttpSession session) {
        session.setAttribute(SESSION_USER_ID, user.getUserId());
        session.setAttribute(SESSION_USERNAME, user.getUsername());
        return "redirect:/";
    }

    private void setMode(Model model, String requestedMode) {
        String mode = switch (requestedMode) {
            case "register", "forgot" -> requestedMode;
            default -> "login";
        };
        model.addAttribute("authMode", mode);
    }
}
