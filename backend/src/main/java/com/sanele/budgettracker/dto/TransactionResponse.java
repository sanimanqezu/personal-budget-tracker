package com.sanele.budgettracker.dto;

import com.sanele.budgettracker.entity.Transaction;
import com.sanele.budgettracker.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        Long categoryId,
        String categoryName,
        BigDecimal amount,
        String description,
        LocalDate transactionDate,
        TransactionType type,
        LocalDateTime createdAt
) {
    public static TransactionResponse from(Transaction t) {
        return new TransactionResponse(
                t.getId(),
                t.getCategory().getId(),
                t.getCategory().getName(),
                t.getAmount(),
                t.getDescription(),
                t.getTransactionDate(),
                t.getType(),
                t.getCreatedAt()
        );
    }
}
