package com.kevin.financetracker.dto;

public record RegisterRequest(
        String name,
        String email,
        String password
) {}
