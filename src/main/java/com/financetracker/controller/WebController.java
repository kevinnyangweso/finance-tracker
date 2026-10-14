package com.financetracker.controller;

import com.financetracker.dto.DashboardResponse;
import com.financetracker.dto.RegisterRequest;
import com.financetracker.dto.LoginRequest;
import com.financetracker.service.CategoryService;
import com.financetracker.service.DashboardService;
import com.financetracker.service.TransactionService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class WebController {

    private final DashboardService dashboardService;
    private final TransactionService transactionService;
    private final CategoryService categoryService;

    public WebController(DashboardService dashboardService,
                         TransactionService transactionService,
                         CategoryService categoryService) {
        this.dashboardService = dashboardService;
        this.transactionService = transactionService;
        this.categoryService = categoryService;
    }

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
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

        model.addAttribute("loginRequest", new LoginRequest());
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        String username = authentication.getName();

        DashboardResponse summary = dashboardService.getSummary(username);
        model.addAttribute("summary", summary);
        model.addAttribute("username", username);

        return "dashboard";
    }

    @GetMapping("/transactions")
    public String transactions(Authentication authentication, Model model) {
        String username = authentication.getName();

        model.addAttribute("transactions",
                transactionService.getAllForUser(username));
        model.addAttribute("categories",
                categoryService.getAll(username));
        model.addAttribute("username", username);

        return "transactions";
    }

    @GetMapping("/transactions/new")
    public String newTransactionForm(Authentication authentication, Model model) {
        model.addAttribute("categories",
                categoryService.getAll(authentication.getName()));
        model.addAttribute("username", authentication.getName());
        return "add-transaction";
    }

    @GetMapping("/categories")
    public String categories(Authentication authentication, Model model) {
        model.addAttribute("categories",
                categoryService.getAll(authentication.getName()));
        model.addAttribute("username", authentication.getName());
        return "categories";
    }
}