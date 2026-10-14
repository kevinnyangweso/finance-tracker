package com.financetracker.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    // Landing page
    @GetMapping("/")
    public String home() {
        return "index";
    }

    // Register page
    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    // Login page
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    // Dashboard (we'll build the content in 9C)
    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }
}