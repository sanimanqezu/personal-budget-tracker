package com.sanele.budgettracker.dto;

import java.math.BigDecimal;

public record CategorySummaryResponse(
        String categoryName,
        String type,
        BigDecimal total,
        double percentage
) {}
