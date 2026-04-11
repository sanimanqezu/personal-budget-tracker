package com.sanele.budgettracker.dto;

public record AuthResponse(
        String token,
        String username,
        String email,
        Long userId
) {}
