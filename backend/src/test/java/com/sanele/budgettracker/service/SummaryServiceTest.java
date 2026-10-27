package com.sanele.budgettracker.service;

import com.sanele.budgettracker.dto.CategorySummaryResponse;
import com.sanele.budgettracker.dto.MonthlySummaryResponse;
import com.sanele.budgettracker.entity.TransactionType;
import com.sanele.budgettracker.entity.User;
import com.sanele.budgettracker.repository.TransactionRepository;
import com.sanele.budgettracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SummaryService Unit Tests")
class SummaryServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SummaryService summaryService;

    private User user;
    private final LocalDate start = LocalDate.of(2024, 3, 1);
    private final LocalDate end = LocalDate.of(2024, 3, 31);

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("alice").email("alice@example.com")
                .passwordHash("hashed").build();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
    }

    @Test
    @DisplayName("getMonthlySummary — computes net balance and totals for the month")
    void getMonthlySummary_computesTotalsAndNetBalance() {
        when(transactionRepository.sumByUserIdAndTypeAndDateBetween(1L, TransactionType.INCOME, start, end))
                .thenReturn(new BigDecimal("1000.00"));
        when(transactionRepository.sumByUserIdAndTypeAndDateBetween(1L, TransactionType.EXPENSE, start, end))
                .thenReturn(new BigDecimal("350.00"));
        when(transactionRepository.countByUserIdAndDateBetween(1L, start, end)).thenReturn(4L);

        MonthlySummaryResponse result = summaryService.getMonthlySummary("alice", "2024-03");

        assertThat(result.totalIncome()).isEqualByComparingTo("1000.00");
        assertThat(result.totalExpenses()).isEqualByComparingTo("350.00");
        assertThat(result.netBalance()).isEqualByComparingTo("650.00");
        assertThat(result.transactionCount()).isEqualTo(4L);
    }

    @Test
    @DisplayName("getCategorySummary — computes the percentage share of each category")
    void getCategorySummary_computesPercentages() {
        when(transactionRepository.sumByCategoryAndDateBetween(1L, start, end)).thenReturn(List.of(
                new Object[]{"Food", TransactionType.EXPENSE, new BigDecimal("200.00")},
                new Object[]{"Salary", TransactionType.INCOME, new BigDecimal("600.00")}
        ));

        List<CategorySummaryResponse> result = summaryService.getCategorySummary("alice", "2024-03");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(CategorySummaryResponse::categoryName)
                .containsExactly("Food", "Salary");
        assertThat(result.get(0).percentage()).isEqualTo(25.0);
        assertThat(result.get(1).percentage()).isEqualTo(75.0);
        assertThat(result.get(0).type()).isEqualTo("EXPENSE");
    }

    @Test
    @DisplayName("getCategorySummary — returns zero percentages when the grand total is zero")
    void getCategorySummary_zeroTotal_returnsZeroPercentages() {
        when(transactionRepository.sumByCategoryAndDateBetween(1L, start, end)).thenReturn(
                java.util.Collections.singletonList(
                        new Object[]{"Food", TransactionType.EXPENSE, BigDecimal.ZERO}));

        List<CategorySummaryResponse> result = summaryService.getCategorySummary("alice", "2024-03");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).percentage()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("getCategorySummary — returns an empty list when there are no transactions")
    void getCategorySummary_noData_returnsEmptyList() {
        when(transactionRepository.sumByCategoryAndDateBetween(1L, start, end)).thenReturn(List.of());

        assertThat(summaryService.getCategorySummary("alice", "2024-03")).isEmpty();
    }
}
