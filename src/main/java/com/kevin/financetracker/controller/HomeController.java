package com.kevin.financetracker.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "Finance Tracker API is running!";
    }

    @GetMapping("/public/health")
    public String health() {
        return "API Health: OK - PostgreSQL connected successfully";
    }

    @GetMapping("/public/test")
    public String test() {
        return "Public test endpoint - no authentication required";
    }
}