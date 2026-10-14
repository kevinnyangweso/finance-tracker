package com.financetracker.controller;

import com.financetracker.dto.AuthResponse;
import com.financetracker.dto.LoginRequest;
import com.financetracker.dto.RegisterRequest;
import com.financetracker.security.CookieUtil;
import com.financetracker.security.JwtService;
import com.financetracker.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class WebAuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final CookieUtil cookieUtil;

    public WebAuthController(AuthService authService,
                             JwtService jwtService,
                             CookieUtil cookieUtil) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.cookieUtil = cookieUtil;
    }

    // ============================================
    // REGISTER — form submission
    // ============================================
    @PostMapping("/register")
    public String registerSubmit(
            @ModelAttribute @Valid RegisterRequest request,
            Model model) {

        try {
            authService.register(request);
            // Success → redirect to login page with a success parameter
            return "redirect:/login?registered=true";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("username", request.getUsername());
            model.addAttribute("email", request.getEmail());
            return "register";
        }
    }

    // ============================================
    // LOGIN — form submission
    // ============================================
    @PostMapping("/login")
    public String loginSubmit(
            @ModelAttribute LoginRequest request,
            HttpServletResponse response,
            Model model) {

        try {
            AuthResponse authResponse = authService.login(request);

            // Set JWT as HTTP-only cookie
            cookieUtil.setJwtCookie(
                    response,
                    authResponse.getToken(),
                    jwtService.getJwtExpirationSeconds()
            );

            return "redirect:/dashboard";
        } catch (Exception e) {
            model.addAttribute("error", "Invalid username or password");
            model.addAttribute("username", request.getUsername());
            return "login";
        }
    }

    // ============================================
    // LOGOUT — clear the cookie
    // ============================================
    @PostMapping("/logout")
    public String logout(HttpServletResponse response) {
        cookieUtil.clearJwtCookie(response);
        return "redirect:/login?logout=true";
    }
}