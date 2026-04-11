package com.sanele.budgettracker.dto;

import com.sanele.budgettracker.entity.Category;
import com.sanele.budgettracker.entity.TransactionType;

public record CategoryResponse(
        Long id,
        String name,
        TransactionType type
) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getType());
    }
}
