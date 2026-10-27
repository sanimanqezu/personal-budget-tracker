package com.sanele.budgettracker.controller;

import com.sanele.budgettracker.dto.CategorySummaryResponse;
import com.sanele.budgettracker.dto.MonthlySummaryResponse;
import com.sanele.budgettracker.security.JwtAuthFilter;
import com.sanele.budgettracker.service.SummaryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SummaryController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(username = "alice")
@DisplayName("SummaryController Slice Tests")
class SummaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SummaryService summaryService;

    @Test
    @DisplayName("GET /api/summary/monthly — returns 200 with the monthly summary")
    void getMonthly_returns200() throws Exception {
        when(summaryService.getMonthlySummary("alice", "2024-03")).thenReturn(
                new MonthlySummaryResponse(new BigDecimal("1000.00"), new BigDecimal("350.00"),
                        new BigDecimal("650.00"), 4L));

        mockMvc.perform(get("/api/summary/monthly").param("month", "2024-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.netBalance").value(650.00))
                .andExpect(jsonPath("$.transactionCount").value(4));

        verify(summaryService).getMonthlySummary("alice", "2024-03");
    }

    @Test
    @DisplayName("GET /api/summary/by-category — returns 200 with per-category totals")
    void getByCategory_returns200() throws Exception {
        when(summaryService.getCategorySummary("alice", "2024-03")).thenReturn(List.of(
                new CategorySummaryResponse("Food", "EXPENSE", new BigDecimal("200.00"), 25.0),
                new CategorySummaryResponse("Salary", "INCOME", new BigDecimal("600.00"), 75.0)));

        mockMvc.perform(get("/api/summary/by-category").param("month", "2024-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$[0].percentage").value(25.0));
    }

    @Test
    @DisplayName("GET /api/summary/monthly — returns 400 when the month param is missing")
    void getMonthly_withoutMonthParam_returns400() throws Exception {
        mockMvc.perform(get("/api/summary/monthly"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString("month")));

        verifyNoInteractions(summaryService);
    }
}
