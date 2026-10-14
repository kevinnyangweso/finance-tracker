package com.financetracker.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class WebController {

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerRequest", new com.financetracker.dto.RegisterRequest());
        return "register";
    }

    @GetMapping("/login")
    public String loginPage(
            @RequestParam(required = false) String registered,
            @RequestParam(required = false) String logout,
            Model model) {

        if ("true".equals(registered)) {
            model.addAttribute("success", "Account created! Please log in.");
        }
        if ("true".equals(logout)) {
            model.addAttribute("success", "You have been logged out.");
        }

        model.addAttribute("loginRequest", new com.financetracker.dto.LoginRequest());
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }
}