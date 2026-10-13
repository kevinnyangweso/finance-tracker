package com.financetracker.controller;

import com.financetracker.dto.MessageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/protected")
    public ResponseEntity<MessageResponse> protectedEndpoint(Authentication authentication) {
        return ResponseEntity.ok(
                new MessageResponse("Hello " + authentication.getName() + "! You have access.")
        );
    }
}