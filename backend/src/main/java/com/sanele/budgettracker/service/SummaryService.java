package com.sanele.budgettracker.service;

import com.sanele.budgettracker.dto.CategorySummaryResponse;
import com.sanele.budgettracker.dto.MonthlySummaryResponse;
import com.sanele.budgettracker.entity.TransactionType;
import com.sanele.budgettracker.entity.User;
import com.sanele.budgettracker.repository.TransactionRepository;
import com.sanele.budgettracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SummaryService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public MonthlySummaryResponse getMonthlySummary(String username, String month) {
        User user = getUser(username);
        YearMonth yearMonth = YearMonth.parse(month);
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        BigDecimal totalIncome = transactionRepository.sumByUserIdAndTypeAndDateBetween(
                user.getId(), TransactionType.INCOME, start, end);
        BigDecimal totalExpenses = transactionRepository.sumByUserIdAndTypeAndDateBetween(
                user.getId(), TransactionType.EXPENSE, start, end);
        BigDecimal netBalance = totalIncome.subtract(totalExpenses);
        long transactionCount = transactionRepository.countByUserIdAndDateBetween(
                user.getId(), start, end);

        return new MonthlySummaryResponse(totalIncome, totalExpenses, netBalance, transactionCount);
    }

    @Transactional(readOnly = true)
    public List<CategorySummaryResponse> getCategorySummary(String username, String month) {
        User user = getUser(username);
        YearMonth yearMonth = YearMonth.parse(month);
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        List<Object[]> rawResults = transactionRepository.sumByCategoryAndDateBetween(
                user.getId(), start, end);

        BigDecimal grandTotal = rawResults.stream()
                .map(row -> (BigDecimal) row[2])
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<CategorySummaryResponse> responses = new ArrayList<>();
        for (Object[] row : rawResults) {
            String categoryName = (String) row[0];
            String type = row[1].toString();
            BigDecimal total = (BigDecimal) row[2];
            double percentage = grandTotal.compareTo(BigDecimal.ZERO) == 0
                    ? 0.0
                    : total.divide(grandTotal, 4, RoundingMode.HALF_UP)
                           .multiply(BigDecimal.valueOf(100))
                           .doubleValue();
            responses.add(new CategorySummaryResponse(categoryName, type, total, percentage));
        }

        return responses;
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
